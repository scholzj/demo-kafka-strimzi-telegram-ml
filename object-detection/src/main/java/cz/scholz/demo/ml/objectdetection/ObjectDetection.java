package cz.scholz.demo.ml.objectdetection;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
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

public class ObjectDetection {
    protected static final Logger log = LogManager.getLogger(ObjectDetection.class);

    protected static Predictor<Image, DetectedObjects> predictor;

    public static void main(String[] args) {
        ObjectDetectionConfig config = ObjectDetectionConfig.fromEnv();
        Properties props = ObjectDetectionConfig.createProperties(config);
        log.info(ObjectDetectionConfig.class.getName() + ": {}",  config.toString());

        try {
            Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                    .optApplication(Application.CV.OBJECT_DETECTION)
                    .setTypes(Image.class, DetectedObjects.class)
                    .optFilter("backbone", "resnet50")
                    .build();
            predictor = ModelZoo.loadModel(criteria).newPredictor();
        } catch (IOException|ModelNotFoundException|MalformedModelException e) {
            log.error("Failed to load model", e);
            System.exit(0);
        }

        StreamsBuilder builder = new StreamsBuilder();

        builder.stream(config.getSourceTopic(), Consumed.with(Serdes.String(), Serdes.String()))
                .transform(ObjectDetectionTransformer::new)
                .to(config.getTargetTopic(), Produced.with(Serdes.String(), Serdes.String()));

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();
    }

    static class ObjectDetectionTransformer implements Transformer<String, String, KeyValue<String, String>> {
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
                Image img = ImageFactory.getInstance().fromUrl(message);
                DetectedObjects detected = predictor.predict(img);

                String reply;

                if (detected.getNumberOfObjects() == 0) {
                    reply = "No objects were detected in your photo 😕";
                } else {
                    reply = "Following objects were detected in your photo:\n";

                    for (Classifications.Classification classification : detected.items()) {
                        reply += "- " + classification.getClassName() + " (" + f.format(classification.getProbability()*100) + "% probability)\n";
                    }
                }

                return KeyValue.pair(null, reply);
            } catch (TranslateException|IOException e) {
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
