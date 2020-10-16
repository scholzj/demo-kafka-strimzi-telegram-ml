package cz.scholz.demo.ml.sentimentanalysis;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.translate.TranslateException;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Properties;

public class SentimentAnalysis {
    protected static final Logger log = LogManager.getLogger(SentimentAnalysis.class);

    protected static Predictor<String, Classifications> predictor;

    public static void main(String[] args) {
        SentimentAnalysisConfig config = SentimentAnalysisConfig.fromEnv();
        Properties props = SentimentAnalysisConfig.createProperties(config);
        log.info(SentimentAnalysisConfig.class.getName() + ": {}",  config.toString());

        try {
            Criteria<String, Classifications> criteria = Criteria.builder()
                    .optApplication(Application.NLP.SENTIMENT_ANALYSIS)
                    .setTypes(String.class, Classifications.class)
                    .build();
            predictor = ModelZoo.loadModel(criteria).newPredictor();
        } catch (IOException|ModelNotFoundException|MalformedModelException e) {
            log.error("Failed to load model", e);
            System.exit(0);
        }

        StreamsBuilder builder = new StreamsBuilder();

        builder.stream(config.getSourceTopic(), Consumed.with(Serdes.String(), Serdes.String()))
                .transform(SentimentAnalysisTransformer::new)
                .to(config.getTargetTopic(), Produced.with(Serdes.String(), Serdes.String()));

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();
    }

    static class SentimentAnalysisTransformer implements Transformer<String, String, KeyValue<String, String>> {
        private static final DecimalFormat f = new DecimalFormat("##.00");
        ProcessorContext context;

        @Override
        public void init(ProcessorContext context) {
            this.context = context;
        }

        @Override
        public KeyValue<String, String> transform(String key, String message) {
            // Add Chat ID as header for Camel connector
            context.headers().add("CamelHeader.CamelTelegramChatId", key.getBytes());

            try {
                Classifications classifications = predictor.predict(message);
                String reply = "Your message was " + classifications.best().getClassName() + " with " + f.format(classifications.best().getProbability()*100) + "% probability";
                return KeyValue.pair(null, reply);
            } catch (TranslateException e) {
                log.error("Failed to do model prediction", e);
            }

            return KeyValue.pair(null, "Failed to make prediction 😒");
        }

        @Override
        public void close() {
            // Do nothing
        }
    }
}
