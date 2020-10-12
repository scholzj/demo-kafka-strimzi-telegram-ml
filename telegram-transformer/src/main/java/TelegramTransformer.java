import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TelegramTransformer {
    private static final Logger log = LogManager.getLogger(TelegramTransformer.class);
    private static final Serde<JsonNode> jsonSerde = Serdes.serdeFrom(new JsonSerializer(), new JsonDeserializer());

    public static void main(String[] args) {
        TelegramTransformerConfig config = TelegramTransformerConfig.fromEnv();
        Properties props = TelegramTransformerConfig.createProperties(config);
        log.info(TelegramTransformerConfig.class.getName() + ": {}",  config.toString());

        StreamsBuilder builder = new StreamsBuilder();

        builder.stream(config.getSourceTopic(), Consumed.with(Serdes.String(), jsonSerde))
                .flatTransform(TelegramMessageTransformer::new)
                .to(config.getTargetTopic(), Produced.with(Serdes.String(), Serdes.String()));

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();
    }

    static class TelegramMessageTransformer implements Transformer<String, JsonNode, Iterable<KeyValue<String, String>>> {
        ProcessorContext context;

        @Override
        public void init(ProcessorContext context) {
            this.context = context;
        }

        @Override
        public Iterable<KeyValue<String, String>> transform(String key, JsonNode value) {
            // Remove headers
            for (Header header : context.headers().toArray())   {
                context.headers().remove(header.key());

            }

            List<KeyValue<String, String>> transformedMessages = new ArrayList<>();

            if (value.get("text") != null
                    && !value.get("text").asText().isEmpty()
                    && value.get("chat") != null
                    && value.get("chat").get("id") != null
                    && !value.get("chat").get("id").asText().isEmpty())   {
                String text = value.get("text").asText();
                String chatId = value.get("chat").get("id").asText();
                transformedMessages.add(KeyValue.pair(chatId, text));
                log.info("Found text={} for chat_id={} => transforming message", text, chatId);
            } else {
                log.warn("Did not found text or chat_id");
            }

            return transformedMessages;
        }

        @Override
        public void close() {

        }
    }
}
