package io.kabanero.event.constants;

public class KafkaInfo {
    public static String KAFKA_BROKERS = "my-cluster-kafka-bootstrap:9092";
    public static String CLIENT_ID = "client1";
    public static String TOPIC_NAME = "my-topic";
    public static String GROUP_ID_CONFIG = "consumerGroup1";
    public static String OFFSET_RESET_LATEST = "latest";
    public static String OFFSET_RESET_EARLIER = "earliest";
    public static Integer MAX_POLL_RECORDS = 1;
}
