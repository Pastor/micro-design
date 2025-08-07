package it.micro.saga;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static it.micro.saga.Action.*;
import static it.micro.saga.Transport.TRANSACTION_ID;

public interface Service extends Transport.Handler {

    String name();

    Optional<String> execute(String action, Map<String, Object> params);

    Status status(String transactionId);

    void register(Transport transport, String topic);

    final class Reactive implements Service {
        private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();
        private final String name;
        private final Transport transport;
        private final Map<String, Action.Factory> factories;
        String TOPIC_TRANSACTION_REQUEST_RESULT = "seller.transaction.result.create";

        public Reactive(String name, Transport transport, Map<String, Action.Factory> factories) {
            this.name = name;
            this.transport = transport;
            this.factories = factories;
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
                    transaction.execute(transport);
                }
            } catch (Exception ex) {
                transactions.put(transactionId, transaction.update(Status.FAILURE));
            }
        }

        @Override
        public Status status(String transactionId) {
            return Optional.ofNullable(transactions.get(transactionId)).map(Transaction::status).orElse(Status.NOT_FOUND);
        }

        @Override
        public void register(Transport transport, String topic) {
            transport.register(name(), topic, this);
        }

        @Override
        public void handle(String topicName, Transport.TransactionMessage message) {
            String transactionId = message.id().toString();
            try {
                Map<String, Object> params = message.payload();
                if (transactions.containsKey(transactionId)) {
                    Transaction transaction = transactions.get(transactionId);
                    transaction.handle(topicName, params);
                    Status status = transaction.status();
                    if (status == Status.FAILURE) {
                        compensation(transaction);
                    } else if (transaction.isReady()) {
                        transaction.execute(transport);
                    }
                } else if (isAction(params)) {
                    String action = (String) params.get(ACTION_KEY);
                    execute(transactionId, action, params);
                } else {
                    System.out.println("Skip  : " + message);
                }
            } catch (Exception ex) {
                System.out.println("Error : " + ex.getMessage());
                transport.publish(TOPIC_TRANSACTION_REQUEST_RESULT,
                        Map.of(ERROR_KEY, ex.getMessage(),
                                STATUS_KEY, Status.FAILURE,
                                TRANSACTION_ID, transactionId,
                                SENDER_KEY, name()));
            } finally {
                message.acknowledge();
            }
        }

        private void compensation(Transaction transaction) {
            transaction.setCompensation();
        }

        private boolean isAction(Map<String, Object> params) {
            return params.containsKey(ACTION_KEY);
        }

        private static final class Transaction {
            private final AtomicReference<Status> status = new AtomicReference<>(Status.NOT_FOUND);
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
                String sender = Objects.requireNonNull((String) params.get(SENDER_KEY), "Sender is null");
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
                    if (statuses.contains(Status.FAILURE)) {
                        update(Status.FAILURE);
                    } else {
                        update(Status.SUCCESS);
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

            private void execute(Transport transport) {
                if (status.get().equals(Status.READY)) {
                    status.set(Status.PENDING);
                    action.preRequests();
                    for (Request req : requests()) {
                        Request.Information information = req.information();
                        requests.add(req.update(Status.PENDING));
                        transport.publish(information.sendTopic(), req.data());
                    }
                    action.postRequests();
                }
            }
        }
    }


}
