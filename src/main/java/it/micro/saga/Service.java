package it.micro.saga;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static it.micro.saga.Action.ACTION_KEY;
import static it.micro.saga.Action.ERROR_KEY;
import static it.micro.saga.Action.RESULT_KEY;
import static it.micro.saga.Action.SENDER_KEY;
import static it.micro.saga.Action.STATUS_KEY;
import static it.micro.saga.Transport.TRANSACTION_ID;
import static it.micro.saga.simple.Constants.ACTION_ROLLBACK;

public interface Service extends Transport.Handler {

    String name();

    Optional<String> execute(String action, Map<String, Object> params);

    Status status(String transactionId);

    Map<String, Object> result(String transactionId);

    void register(Transport transport, String topic);

    final class Reactive implements Service {
        private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();
        private final String name;
        private final Transport transport;
        private final Map<String, Action.Factory> factories;
        private final String transactionResultTopicName;

        public Reactive(String name,
                        Transport transport,
                        Map<String, Action.Factory> factories,
                        String transactionResultTopicName) {
            this.name = name;
            this.transport = transport;
            this.factories = factories;
            this.transactionResultTopicName = transactionResultTopicName;
            transport.register(name(), transactionResultTopicName, this);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Optional<String> execute(String action, Map<String, Object> params) {
            String transactionId = UUID.randomUUID().toString();
            try {
                execute(transactionId, action, params);
            } catch (Exception ex) {
                System.out.println("Error : " + ex.getMessage());
                return Optional.empty();
            }
            return Optional.of(transactionId);
        }

        private void execute(String transactionId, String action, Map<String, Object> params) {
            Action.Factory factory = factories.get(action);
            if (factory == null) {
                throw new IllegalArgumentException("Action '" + action + "' not defined");
            }
            Transaction transaction = new Transaction(transactionId, factory, params);
            try {
                transactions.put(transactionId, transaction);
                if (transaction.isReady()) {
                    transaction.execute(transport, name());
                }
            } catch (Exception ex) {
                transactions.put(transactionId, transaction.update(Status.FAILURE));
            }
            System.out.printf("[%s:%9s] %s%n", transactionId, name(), transaction.status());
        }

        @Override
        public Status status(String transactionId) {
            return Optional.ofNullable(transactions.get(transactionId)).map(Transaction::status).orElse(Status.NOT_FOUND);
        }

        @Override
        public Map<String, Object> result(String transactionId) {
            if (transactions.containsKey(transactionId)) {
                Transaction transaction = transactions.get(transactionId);
                if (transaction.status() == Status.SUCCESS) {
                    return transaction.result();
                }
            }
            return Map.of();
        }

        @Override
        public void register(Transport transport, String topic) {
            transport.register(name(), topic, this);
        }

        @Override
        public void handle(String topicName, Transport.TransactionMessage message) {
            String transactionId = message.id().toString();
            Map<String, Object> params = message.payload();
            String sender = (String) params.get(SENDER_KEY);
            if (sender != null && sender.equals(name())) {
                //NOTICE: Self skipping
                return;
            }
            try {
                if (transactions.containsKey(transactionId)) {
                    Transaction transaction = transactions.get(transactionId);
                    if (transaction.status() == Status.SUCCESS
                            || transaction.status() == Status.COMPENSATION_SUCCEEDED
                            || transaction.status() == Status.COMPENSATION_FAILED) {
                        return;
                    }
                    transaction.handle(topicName, params);
                    Status status = transaction.status();
                    System.out.printf("[%s:%9s] %s%n", transactionId, name(), status);
                    if (status == Status.FAILURE) {
                        compensation(transaction);
                    } else if (status == Status.SUCCESS) {
                        transport.publish(transactionResultTopicName,
                                Map.of(TRANSACTION_ID, transactionId,
                                        STATUS_KEY, Status.SUCCESS,
                                        SENDER_KEY, name(),
                                        RESULT_KEY, transaction.result()
                                ));
                    } else if (status == Status.COMPENSATION) {
                        //NOTICE: Еще в процессе компенсации
                    } else if (status == Status.COMPENSATION_SUCCEEDED) {
                        //NOTICE: Все компенсировали
                    } else if (status == Status.COMPENSATION_COMPLETE) {
                        transaction.update(Status.COMPENSATION_SUCCEEDED);
                        transport.publish(transactionResultTopicName,
                                Map.of(TRANSACTION_ID, transactionId,
                                        STATUS_KEY, Status.COMPENSATION_COMPLETE,
                                        SENDER_KEY, name()
                                ));
                    } else if (transaction.isReady()) {
                        transaction.execute(transport, name());
                    }
                } else if (isAction(params)) {
                    String action = (String) params.get(ACTION_KEY);
                    execute(transactionId, action, params);
                } else {
                    //NOTICE: Пропускаем сообщение
                }
            } catch (Exception ex) {
                transport.publish(transactionResultTopicName,
                        Map.of(ERROR_KEY, ex.getMessage(),
                                STATUS_KEY, Status.FAILURE,
                                TRANSACTION_ID, transactionId,
                                SENDER_KEY, name()));
            } finally {
                message.acknowledge();
            }
        }

        private void compensation(Transaction transaction) {
            if (transaction.status() == Status.FAILURE) {
                transaction.setCompensation();
                List<Request> requests = transaction.requests;
                boolean sent = false;
                for (int i = 0; i < requests.size(); i++) {
                    Request request = requests.get(i);
                    Map<String, Object> data = new HashMap<>(request.data());
                    data.put(ACTION_KEY, ACTION_ROLLBACK);
                    if (request.status() == Status.SUCCESS) {
                        transport.publish(request.information().sendTopic(), data);
                        requests.set(i, request.update(Status.PENDING));
                        sent = true;
                    }
                }
                if (!sent) {
                    transaction.update(Status.COMPENSATION_COMPLETE);
                }
            }
        }

        private boolean isAction(Map<String, Object> params) {
            return params.containsKey(ACTION_KEY);
        }

        private static final class Transaction {
            private final AtomicReference<Status> status = new AtomicReference<>(Status.NOT_READY);
            private final AtomicReference<LocalDateTime> lastAccess = new AtomicReference<>();
            private final List<Request> requests = new LinkedList<>();
            private final Action action;

            private Transaction(String transactionId, Action.Factory factory, Map<String, Object> params) {
                this.action = factory.create(transactionId, params);
                if (action.isReady()) {
                    status.set(Status.READY);
                }
            }

            private void handle(String topicName, Map<String, Object> params) {
                if (!action.isReady()) {
                    action.handle(topicName, params);
                }
                if (status() == Status.NOT_READY) {
                    update(Status.READY);
                }
                String sender = (String) params.get(SENDER_KEY);
                if (sender == null) {
                    throw new IllegalStateException();
                }
                boolean updated = false;
                for (int i = 0; i < requests.size(); i++) {
                    Request request = requests.get(i);
                    if (request.information().receiveTopic().equals(topicName) &&
                            request.information().serviceName().equals(sender)) {
                        requests.set(i, request.update(params));
                        updated = true;
                    }
                }
                if (updated) {
                    refreshStatus();
                }
            }

            private void refreshStatus() {
                var statuses = requests.stream().map(Request::status).collect(Collectors.toSet());
                if (!statuses.contains(Status.PENDING)) {
                    if (status() == Status.COMPENSATION_COMPLETE) {
                        System.out.println("Compensation complete already set");
                    } else if (status() == Status.COMPENSATION_SUCCEEDED) {
                        //NOTICE: Уже компенсирован
                    } else {
                        boolean isCompensationComplete = statuses.stream()
                                .allMatch(status -> status == Status.COMPENSATION_COMPLETE);
                        if (isCompensationComplete) {
                            update(Status.COMPENSATION_COMPLETE);
                        } else if (statuses.contains(Status.FAILURE)) {
                            update(Status.FAILURE);
                        } else {
                            update(Status.SUCCESS);
                        }
                    }
                }
                lastAccess.set(LocalDateTime.now());
            }

            private Status status() {
                return status.get();
            }

            private void setCompensation() {
                this.status.set(Status.COMPENSATION);
            }

            private Transaction update(Status status) {
                this.status.set(status);
                lastAccess.set(LocalDateTime.now());
                if (status.equals(Status.SUCCESS)) {
                    action.afterSuccess();
                } else if (status.equals(Status.FAILURE)) {
                    action.afterFailure();
                }
                return this;
            }

            private List<Request> requests() {
                return action.requests();
            }

            private boolean isReady() {
                return status.get().equals(Status.READY);
            }

            private void execute(Transport transport, String sender) {
                if (status.get().equals(Status.READY)) {
                    status.set(Status.PENDING);
                    action.preRequests();
                    for (Request req : requests()) {
                        Request.Information information = req.information();
                        Request updated = req.update(Status.PENDING, sender);
                        requests.add(updated);
                        transport.publish(information.sendTopic(), updated.data());
                    }
                    action.postRequests();
                    if (requests.isEmpty()) {
                        status.set(Status.SUCCESS);
                    }
                }
            }

            @Override
            public String toString() {
                var statuses = String.join(", ", requests.stream()
                        .map(Request::toString).collect(Collectors.toSet()));
                return status.get().toString() + "[" + statuses + "]";
            }

            private Map<String, Object> result() {
                Map<String, Object> result = new HashMap<>();
                for (Request request : requests) {
                    if (request.result() != null) {
                        result.put(request.information().serviceName(), request.result());
                    }
                }
                return result;
            }
        }
    }
}
