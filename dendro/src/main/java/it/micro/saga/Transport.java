package it.micro.saga;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public interface Transport {
  String TRANSACTION_ID = "transaction_id";

  void register(String groupId, String topicName, Handler handler);

  void unregister(String groupId, String topicName, Handler handler);

  void publish(String topicName, Map<String, Object> payload);

  interface Handler {
    void handle(String topicName, TransactionMessage message);
  }

  interface Message {

    Map<String, Object> payload();

    void acknowledge();
  }

  interface TransactionMessage extends Message {
    default UUID id() {
      return UUID.fromString((String) Objects.requireNonNull(payload().get(TRANSACTION_ID), "Transaction id not defined"));
    }
  }

  final class Memory implements Transport {
    private final Map<String, List<TransactionMessage>> messages = new HashMap<>();
    private final Map<String, Map<String, Long>> offsets = new HashMap<>();
    private final Map<String, Set<String>> groups = new HashMap<>();
    private final Map<KeyGroup, Handler> handlers = new HashMap<>();
    private final ExecutorService executor;

    public Memory(ExecutorService executor) {
      this.executor = executor;
    }

    @Override
    public void register(String groupId, String topicName, Handler handler) {
      handlers.put(new KeyGroup(topicName, groupId), handler);
      groups.computeIfAbsent(topicName, key -> new HashSet<>()).add(groupId);
    }

    @Override
    public void unregister(String groupId, String topicName, Handler handler) {
      handlers.remove(new KeyGroup(topicName, groupId), handler);
    }

    @Override
    public void publish(String topicName, Map<String, Object> payload) {
      List<TransactionMessage> buket = messages.computeIfAbsent(topicName, key -> new ArrayList<>());
      buket.add(new MemoryMessage(payload, buket.size()));
      fireMessage(topicName);
    }

    private void fireMessage(String topicName) {
      List<TransactionMessage> buket = messages.computeIfAbsent(topicName, key -> new ArrayList<>());
      Set<String> groupIds = groups.computeIfAbsent(topicName, key -> new HashSet<>());
      for (String groupId : groupIds) {
        Handler handler = handlers.get(new KeyGroup(topicName, groupId));
        if (handler == null) {
          continue;
        }
        Map<String, Long> offsetsMap = offsets.computeIfAbsent(groupId, key -> new HashMap<>());
        Long offset = offsetsMap.getOrDefault(topicName, 0L);
        for (int i = Math.toIntExact(offset); i < buket.size(); i++) {
          TransactionMessage message = buket.get(i);
          executor.submit(() -> {
            System.out.printf("[%s:%9s] %s%n", message.id(), groupId, message.payload());
            handler.handle(topicName, new OffsetMessage(message, () -> offsetsMap.put(topicName, offset + 1)));
          });
        }
      }
    }

    private record MemoryMessage(Map<String, Object> payload, long offset) implements TransactionMessage {
      @Override
      public void acknowledge() {
        throw new UnsupportedOperationException();
      }
    }

    private record OffsetMessage(TransactionMessage message, Runnable runnable) implements TransactionMessage {
      @Override
      public Map<String, Object> payload() {
        return message.payload();
      }

      @Override
      public void acknowledge() {
        runnable.run();
      }
    }

    private record KeyGroup(String topicName, String groupId) {
    }

  }
}
