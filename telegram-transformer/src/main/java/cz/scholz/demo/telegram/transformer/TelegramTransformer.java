package cz.scholz.demo.telegram.transformer;

import cz.scholz.demo.telegram.message.TelegramMessage;
import cz.scholz.demo.telegram.message.TelegramMessageDeserializer;
import cz.scholz.demo.telegram.message.TelegramMessagePhoto;
import cz.scholz.demo.telegram.message.TelegramMessageSerializer;
import cz.scholz.demo.telegram.message.getfile.TelegramGetFile;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;

public class TelegramTransformer {
    private static final Logger LOG = LogManager.getLogger(TelegramTransformer.class);
    private static final Serde<TelegramMessage> telegramSerde = Serdes.serdeFrom(new TelegramMessageSerializer(), new TelegramMessageDeserializer());

    public static void main(String[] args) {
        TelegramTransformerConfig config = TelegramTransformerConfig.fromEnv();
        Properties props = TelegramTransformerConfig.createProperties(config);
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class.getName());

        LOG.info(TelegramTransformerConfig.class.getName() + ": {}",  config.toString());

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, TelegramMessage>[] branches = builder.stream(config.getSourceTopic(), Consumed.with(Serdes.String(), telegramSerde))
                .transform(TelegramMessageHeaderRemover::new)
                .peek((key, message) -> LOG.info("Processing message with chat ID {}", message.getChat().getId()))
                .branch((key, message) -> message.getPhoto() != null && !message.getPhoto().isEmpty(),
                        (key, message) -> message.getText() != null && !message.getText().isEmpty());

        // Branch 0 => Photos
        branches[0]
                .map((key, message) -> KeyValue.pair(message.getChat().getId().toString(), getPhotoFile(config, message)))
                .peek((key, value) -> LOG.info("Processing message with chat ID {} and some photo", key))
                .to(config.getObjectDetectionTopic(), Produced.with(Serdes.String(), Serdes.String()));

        // Branch 1 => Texts
        branches[1]
                .map((key, message) -> KeyValue.pair(message.getChat().getId().toString(), message.getText()))
                .peek((key, value) -> LOG.info("Processing message with chat ID {} and text {}", key, value))
                .to(config.getSentimentAnalysisTopic(), Produced.with(Serdes.String(), Serdes.String()));

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();
    }

    private static String getPhotoFile(TelegramTransformerConfig config, TelegramMessage message)    {
        TelegramMessagePhoto biggest = Collections.max(message.getPhoto(), Comparator.comparing(TelegramMessagePhoto::getWidth));
        return TelegramGetFile.getFileAddress(config.getTelegramApiKey(), biggest.getFileId());
    }

    static class TelegramMessageHeaderRemover implements Transformer<String, TelegramMessage, KeyValue<String, TelegramMessage>> {
        ProcessorContext context;

        @Override
        public void init(ProcessorContext context) {
            this.context = context;
        }

        @Override
        public KeyValue<String, TelegramMessage> transform(String key, TelegramMessage message) {
            // Remove headers
            for (Header header : context.headers().toArray())   {
                context.headers().remove(header.key());
            }

            return KeyValue.pair(key, message);
        }

        @Override
        public void close() {
            // Nothing to do
        }
    }
}
