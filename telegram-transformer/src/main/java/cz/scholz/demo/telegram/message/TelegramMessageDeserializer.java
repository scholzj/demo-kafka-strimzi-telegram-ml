package cz.scholz.demo.telegram.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class TelegramMessageDeserializer implements Deserializer {
    private static final Logger LOG = LogManager.getLogger(TelegramMessageDeserializer.class);

    @Override
    public void configure(Map map, boolean b) {
        // Nothing to do
    }

    @Override
    public Object deserialize(String s, byte[] bytes) {
        ObjectMapper mapper = new ObjectMapper();
        TelegramMessage obj = null;

        try {
            obj = mapper.readValue(bytes, TelegramMessage.class);
        } catch (Exception e) {
            LOG.error("Failed to deserialize TelegramMessage", e);
        }

        return obj;
    }

    @Override
    public void close() {
        // Nothing to close
    }
}
