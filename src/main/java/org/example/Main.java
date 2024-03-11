package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.Deserializers;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Predicate;
import org.apache.kafka.streams.kstream.Produced;
import org.example.serdes.JsonDeserializer;
import org.example.serdes.JsonSerializer;
import org.example.serdes.SerdesFactory;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class Main {
    public static void main(String[] args) {
        String SPEED = "speed";
        String OVER_SPEED = "overspeed";
        String INPUT_TOPIC = args[0];
        String OUTPUT_TOPIC = args[1];

        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG,"greetings-app");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"latest");


        Predicate<String,JsonNode> speedPredicate
                = (key, value) -> value.get("speed").asInt()>120;

        StreamsBuilder streamsBuilder = new StreamsBuilder();
        streamsBuilder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(),SerdesFactory.dynamicJsonSerdes()))
                .filter(speedPredicate)
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(),SerdesFactory.dynamicJsonSerdes()));

        Topology topology = streamsBuilder.build();

        var kafkaStreams = new KafkaStreams(topology,properties);

        createTopics(properties, List.of(INPUT_TOPIC,OUTPUT_TOPIC));
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));
        kafkaStreams.start();

    }

    private static void createTopics(Properties config, List<String> greetings) {

        AdminClient admin = AdminClient.create(config);
        var partitions = 2;
        short replication  = 1;

        var newTopics = greetings
                .stream()
                .map(topic ->{
                    return new NewTopic(topic, partitions, replication);
                })
                .collect(Collectors.toList());

        var createTopicResult = admin.createTopics(newTopics);
        try {
            createTopicResult
                    .all().get();
        } catch (Exception e) {

        }
    }
}