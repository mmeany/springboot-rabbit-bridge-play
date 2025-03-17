package net.mmeany.play.rabbitbridge;

// {"name": "Joe", "age": 25}

public class TestData {
    public static final String EXCHANGE = "primary";
    public static final String QUEUE_1 = "queue-1";
    public static final String QUEUE_2 = "queue-2";
    public static final String MESSAGE = "{\"name\": \"Joe\", \"age\": 25}";
    public static final String MESSAGES = "[]";
    public static final String JSON_PATH = "$.path";
    public static final int MESSAGE_INDEX = 0;
    public static final int QUEUE_COUNT = 1;
    public static final String CONSUMER_TAG = "consumerTag";
}
