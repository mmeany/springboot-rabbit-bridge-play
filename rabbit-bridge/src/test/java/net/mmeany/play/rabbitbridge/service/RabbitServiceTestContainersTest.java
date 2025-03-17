package net.mmeany.play.rabbitbridge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.mmeany.play.rabbitbridge.TestData;
import net.mmeany.play.rabbitbridge.model.QueueCount;
import net.mmeany.play.rabbitbridge.model.SimpleMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class RabbitServiceTestContainersTest {

    @Container
    private static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(
        DockerImageName.parse("rabbitmq:3.11.9-management"))
        .withExposedPorts(5672)
        .withStartupTimeout(Duration.ofSeconds(60));

    private CachingConnectionFactory connectionFactory;
    private RabbitTemplate rabbitTemplate;
    private RabbitAdmin rabbitAdmin;
    private RabbitService rabbitService;

    @BeforeEach
    void setUp() {
        connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitMQContainer.getHost());
        connectionFactory.setPort(rabbitMQContainer.getAmqpPort());
        connectionFactory.setUsername(rabbitMQContainer.getAdminUsername());
        connectionFactory.setPassword(rabbitMQContainer.getAdminPassword());

        rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter(new ObjectMapper()));

        rabbitAdmin = new RabbitAdmin(connectionFactory);

        rabbitAdmin.declareQueue(new Queue(TestData.QUEUE_1, false, false, true));
        rabbitAdmin.declareQueue(new Queue(TestData.QUEUE_2, false, false, true));

        rabbitAdmin.declareExchange(new org.springframework.amqp.core.TopicExchange(TestData.EXCHANGE));

        rabbitAdmin.declareBinding(new org.springframework.amqp.core.Binding(
            TestData.QUEUE_1,
            org.springframework.amqp.core.Binding.DestinationType.QUEUE,
            TestData.EXCHANGE,
            "*.*." + TestData.QUEUE_1,
            null));

        rabbitAdmin.declareBinding(new org.springframework.amqp.core.Binding(
            TestData.QUEUE_2,
            org.springframework.amqp.core.Binding.DestinationType.QUEUE,
            TestData.EXCHANGE,
            "*.*." + TestData.QUEUE_2,
            null));

        rabbitService = new RabbitService(rabbitTemplate, new ObjectMapper(), null);
        ReflectionTestUtils.setField(rabbitService, "self", rabbitService);
    }

    @AfterEach
    void tearDown() {
        rabbitService.reset();
        rabbitAdmin.deleteQueue(TestData.QUEUE_1);
        rabbitAdmin.deleteQueue(TestData.QUEUE_2);
        connectionFactory.destroy();
    }

    @Test
    void shouldSendAndReceiveMessage() {
        rabbitService.listenForMessages(TestData.QUEUE_1);

        SimpleMessage message = new SimpleMessage(TestData.QUEUE_1, TestData.MESSAGE);
        rabbitService.sendMessage(message);

        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                rabbitService.stopListeningForMessages(TestData.QUEUE_1);
                QueueCount count = rabbitService.messageCount(TestData.QUEUE_1);
                assertThat(count.count(), is(1));
                assertThat(rabbitService.message(TestData.QUEUE_1, 0), containsString(TestData.MESSAGE.replace("\"", "\\\"")));
            });
    }

    @Test
    void shouldHandleMultipleQueues() {
        List<String> queueNames = Arrays.asList(TestData.QUEUE_1, TestData.QUEUE_2);
        rabbitService.listenForMessages(queueNames);

        rabbitService.sendMessage(new SimpleMessage(TestData.QUEUE_1, TestData.MESSAGE + "-1"));
        rabbitService.sendMessage(new SimpleMessage(TestData.QUEUE_2, TestData.MESSAGE + "-2"));

        await().atLeast(Duration.ofMillis(100)).atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                rabbitService.stopListeningToAllQueues();
                List<QueueCount> counts = rabbitService.messageCount();
                assertThat(counts, hasSize(2));

                QueueCount queue1Count = rabbitService.messageCount(TestData.QUEUE_1);
                QueueCount queue2Count = rabbitService.messageCount(TestData.QUEUE_2);

                assertThat(queue1Count.count(), is(1));
                assertThat(rabbitService.message(TestData.QUEUE_1, 0), containsString(TestData.MESSAGE.replace("\"", "\\\"") + "-1"));
                assertThat(queue2Count.count(), is(1));
                assertThat(rabbitService.message(TestData.QUEUE_2, 0), containsString(TestData.MESSAGE.replace("\"", "\\\"") + "-2"));
            });
    }

    @Test
    void shouldResetServiceState() {
        rabbitService.listenForMessages(TestData.QUEUE_1);
        rabbitService.sendMessage(new SimpleMessage(TestData.QUEUE_1, TestData.MESSAGE));

        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                rabbitService.stopListeningForMessages(TestData.QUEUE_1);
                assertThat(rabbitService.messageCount(TestData.QUEUE_1).count(), is(1));
            });

        rabbitService.reset();

        List<QueueCount> counts = rabbitService.messageCount();
        assertThat(counts.size(), is(0));
    }

    @Test
    void shouldFindMessagesByJsonPath() {
        rabbitService.listenForMessages(TestData.QUEUE_1);

        rabbitService.sendMessage(new SimpleMessage(TestData.QUEUE_1, TestData.MESSAGE));
        rabbitService.sendMessage(new SimpleMessage(TestData.QUEUE_1, "{\"age\":30}"));

        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                rabbitService.stopListeningForMessages(TestData.QUEUE_1);
                assertThat(rabbitService.messageCount(TestData.QUEUE_1).count(), is(2));
            });

        String result = rabbitService.findMessagesByJsonPath(TestData.QUEUE_1, "$[?(@.name == 'Joe')]");

        assertTrue(result.contains("\"name\": \"Joe\""));
        assertFalse(result.contains("\"age\": 30"));
    }

    @Test
    void shouldSendMessageWithIncrementingId() {
        rabbitService.listenForMessages(TestData.QUEUE_1);

        rabbitService.sendMessage(new SimpleMessage(TestData.QUEUE_1, TestData.MESSAGE));
        rabbitService.sendMessage(new SimpleMessage(TestData.QUEUE_1, TestData.MESSAGE));

        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                rabbitService.stopListeningForMessages(TestData.QUEUE_1);
                assertThat(rabbitService.messageCount(TestData.QUEUE_1).count(), is(2));
            });

        String message1 = rabbitService.message(TestData.QUEUE_1, 0);
        String message2 = rabbitService.message(TestData.QUEUE_1, 1);

        assertTrue(message1.contains("\"id\":1"));
        assertTrue(message2.contains("\"id\":2"));
    }

    @Test
    void shouldStopListeningToSpecificQueue() {
        rabbitService.listenForMessages(Arrays.asList(TestData.QUEUE_1, TestData.QUEUE_2));

        rabbitService.stopListeningForMessages(TestData.QUEUE_1);

        rabbitService.sendMessage(new SimpleMessage(TestData.QUEUE_1, TestData.MESSAGE));
        rabbitService.sendMessage(new SimpleMessage(TestData.QUEUE_2, TestData.MESSAGE));

        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                rabbitService.stopListeningForMessages(TestData.QUEUE_2);
                QueueCount queue1Count = rabbitService.messageCount(TestData.QUEUE_1);
                QueueCount queue2Count = rabbitService.messageCount(TestData.QUEUE_2);

                assertEquals(0, queue1Count.count());
                assertEquals(1, queue2Count.count());
            });
    }
}