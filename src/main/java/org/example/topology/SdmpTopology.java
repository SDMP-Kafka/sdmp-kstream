package org.example.topology;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Predicate;
import org.apache.kafka.streams.kstream.Produced;
import org.example.serdes.SerdesFactory;

public class SdmpTopology {
    public  static Topology buildTopology(String[] args){
        String INPUT_TOPIC = args[0];
        String OUTPUT_TOPIC = args[1];

        StreamsBuilder streamsBuilder = new StreamsBuilder();

        var stream = streamsBuilder.stream(INPUT_TOPIC
                , Consumed.with(Serdes.String(), SerdesFactory.dynamicJsonSerdes()));

        int i = 2;
        while (i < args.length){
            switch (args[i]) {
                case "STRING" -> {
                    i++;
                    String jsonKey = args[i++];
                    String jsonValue = args[i++];
                    Predicate<String, JsonNode> stringPredicate
                            = (key, value) -> value.get(jsonKey).asText().equals(jsonValue);
                    stream = stream.filter(stringPredicate);
                }
                case "NUMBER" -> {
                    i++;
                    String jsonKey = args[i++];
                    String jsonValue = args[i++];
                    Predicate<String, JsonNode> numberPredicate
                            = (key, value) -> value.get(jsonKey).asInt() == Integer.parseInt(jsonValue);
                    stream = stream.filter(numberPredicate);
                }
                case "RANGE" -> {
                    i++;
                    String jsonKey = args[i++];
                    String jsonRangeStart = args[i++];
                    String jsonRangeEnd = args[i++];
                    Predicate<String, JsonNode> numberRangePredicate = (key, value) -> {
                        int valueAsInt = value.get(jsonKey).asInt();
                        return valueAsInt >= Integer.parseInt(jsonRangeStart)
                                && valueAsInt <= Integer.parseInt(jsonRangeEnd);
                    };
                    stream = stream.filter(numberRangePredicate);
                }
            }
        }
        stream.to(OUTPUT_TOPIC, Produced.with(Serdes.String(),SerdesFactory.dynamicJsonSerdes()));
        return streamsBuilder.build();
    }
}
