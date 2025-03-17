package net.mmeany.play.rabbitbridge.model;

import net.mmeany.play.rabbitbridge.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
public class RabbitListenerTestContainersTest {

    @Container
    private static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(
        DockerImageName.parse("rabbitmq:3.11.9-management"))
        .withExposedPorts(5672)
        .withStartupTimeout(Duration.ofSeconds(60));

    private CachingConnectionFactory connectionFactory;
    private RabbitTemplate rabbitTemplate;
    private RabbitAdmin rabbitAdmin;
    private RabbitListener rabbitListener;

    @BeforeEach
    void setUp() {
        connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitMQContainer.getHost());
        connectionFactory.setPort(rabbitMQContainer.getAmqpPort());
        connectionFactory.setUsername(rabbitMQContainer.getAdminUsername());
        connectionFactory.setPassword(rabbitMQContainer.getAdminPassword());

        rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitAdmin = new RabbitAdmin(connectionFactory);

        rabbitAdmin.declareQueue(new org.springframework.amqp.core.Queue(TestData.QUEUE_1, false, false, true));

        rabbitListener = new RabbitListener(connectionFactory, TestData.QUEUE_1);
    }

    @AfterEach
    void tearDown() {
        if (rabbitListener != null) {
            rabbitListener.dispose(true);
        }
        rabbitAdmin.deleteQueue(TestData.QUEUE_1);
        connectionFactory.destroy();
    }

    @Test
    void shouldReceiveMessages_whenListening() {
        rabbitListener.startListening();
        rabbitTemplate.convertAndSend(TestData.QUEUE_1, TestData.MESSAGE);

        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                rabbitListener.stopListening();
                assertThat(rabbitListener.getMessageCount()).isEqualTo(1);
                assertThat(rabbitListener.getEntry(0)).isEqualTo(TestData.MESSAGE);
            });
    }

    @Test
    void shouldNotReceiveMessages_afterStoppingListener() {
        rabbitListener.startListening();
        rabbitListener.stopListening();

        rabbitTemplate.convertAndSend(TestData.QUEUE_1, TestData.MESSAGE);

        await().pollDelay(Duration.ofMillis(500)).untilAsserted(() -> {
            assertEquals(0, rabbitListener.getMessageCount());
        });
    }

    @Test
    void shouldReceiveMultipleMessages_inOrder() {
        rabbitListener.startListening();

        for (int i = 0; i < 5; i++) {
            rabbitTemplate.convertAndSend(TestData.QUEUE_1, TestData.MESSAGE + "-" + i);
        }

        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                rabbitListener.stopListening();
                assertThat(rabbitListener.getMessageCount()).isEqualTo(5);

                for (int i = 0; i < 5; i++) {
                    assertEquals(TestData.MESSAGE + "-" + i, rabbitListener.getEntry(i));
                }
            });
    }

    @Test
    void shouldThrowException_whenRequestingMessagesBeforeCapture() {
        assertThrows(RuntimeException.class, () -> rabbitListener.getMessageCount());
        assertThrows(RuntimeException.class, () -> rabbitListener.getMessages());
        assertThrows(RuntimeException.class, () -> rabbitListener.getEntry(0));
    }

    @Test
    void shouldResetState_whenDisposingWithStateReset() {
        rabbitListener.startListening();
        rabbitTemplate.convertAndSend(TestData.QUEUE_1, TestData.MESSAGE);

        await().atMost(Duration.ofSeconds(5))
            .until(() -> {
                try {
                    rabbitListener.stopListening();
                    return rabbitListener.getMessageCount() > 0;
                } catch (Exception e) {
                    return false;
                }
            });

        assertEquals(1, rabbitListener.getMessageCount());

        rabbitListener.dispose(true);
        assertThrows(RuntimeException.class, () -> rabbitListener.getMessageCount());
    }
}
