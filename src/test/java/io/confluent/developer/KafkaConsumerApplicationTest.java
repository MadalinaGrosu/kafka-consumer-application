package io.confluent.developer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class KafkaConsumerApplicationTest {

    @Test
    void consumerTest() throws Exception {
        //given
        final Path tempFilePath = Files.createTempFile("test-consumer-output", ".out");
        final ConsumerRecordsHandler<String, String> recordsHandler = new FileWritingRecordsHandler(tempFilePath);
        final Properties testConsumerProperties = KafkaConsumerApplication.loadProperties("configuration/test.properties");
        final String topic = testConsumerProperties.getProperty("input.topic.name");
        final TopicPartition topicPartition = new TopicPartition(topic, 0);
        final MockConsumer<String, String> mockConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        final KafkaConsumerApplication consumerApplication = new KafkaConsumerApplication(mockConsumer, recordsHandler);
        mockConsumer.schedulePollTask(() -> addTopicPartitionsAssignmentAndAddConsumerRecords(topic, mockConsumer, topicPartition));
        mockConsumer.schedulePollTask(consumerApplication::shutdown);

        //when
        consumerApplication.runConsume(testConsumerProperties);

        //then
        final List<String> expectedWords = Arrays.asList("foo", "bar", "baz");
        List<String> actualRecords = Files.readAllLines(tempFilePath);
        assertThat(actualRecords, equalTo(expectedWords));

    }

    private void addTopicPartitionsAssignmentAndAddConsumerRecords(final String topic,
                                                                   final MockConsumer<String, String> mockConsumer,
                                                                   final TopicPartition topicPartition) {

        final Map<TopicPartition, Long> beginningOffsets = new HashMap<>();
        beginningOffsets.put(topicPartition, 0L);
        mockConsumer.rebalance(Collections.singletonList(topicPartition));
        mockConsumer.updateBeginningOffsets(beginningOffsets);
        addConsumerRecords(mockConsumer, topic);
    }

    private void addConsumerRecords(final MockConsumer<String, String> mockConsumer, final String topic) {
        mockConsumer.addRecord(new ConsumerRecord<>(topic, 0, 0, null, "foo"));
        mockConsumer.addRecord(new ConsumerRecord<>(topic, 0, 1, null, "bar"));
        mockConsumer.addRecord(new ConsumerRecord<>(topic, 0, 2, null, "baz"));
    }
}