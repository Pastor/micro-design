package it.micro;

import it.micro.saga.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Шина")
class TrunkTest {
    private Transport trunk;

    @BeforeEach
    void setUp() {
        trunk = new Transport.Memory();
    }

    @Test
    void workflow() {
        List<Map<String, Object>> messages = new ArrayList<>();
        new UniqueConsumer(trunk, "test", messages);
        new UniqueConsumer(trunk, "test", messages);
        new UniqueConsumer(trunk, "test", messages);
        for (int i = 0; i < 1000; i++) {
            messages.add(Map.of(Transport.TRANSACTION_ID, UUID.randomUUID().toString(), "data", "test" + i));
        }
        for (Map<String, Object> message : messages) {
            trunk.publish("test", message);
        }
    }

    private static final class UniqueConsumer implements Transport.Handler {
        private final AtomicInteger count = new AtomicInteger();
        private final String groupId;
        private final List<Map<String, Object>> expected;

        private UniqueConsumer(Transport trunk, String topicName, List<Map<String, Object>> expected) {
            this.expected = expected;
            this.groupId = UUID.randomUUID().toString();
            trunk.register(this.groupId, topicName, this);
        }

        @Override
        public void handle(String topicName, Transport.TransactionMessage message) {
            assertEquals(expected.get(count.intValue()), message.payload(), "Сообщения не одинаковые");
            message.acknowledge();
            System.out.printf("[%s] offset: %3d, message: %s %n", groupId, count.intValue(), message.payload());
            count.incrementAndGet();
        }
    }
}