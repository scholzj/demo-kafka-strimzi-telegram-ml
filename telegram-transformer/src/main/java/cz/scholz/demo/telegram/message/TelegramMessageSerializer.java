package cz.scholz.demo.telegram.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class TelegramMessageSerializer implements Serializer<TelegramMessage> {
    private static final Logger LOG = LogManager.getLogger(TelegramMessageSerializer.class);

    @Override
    public void configure(Map map, boolean b) {
        // Nothing to configure
    }

    @Override
    public byte[] serialize(String s, TelegramMessage o) {
        byte[] retVal = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            retVal = objectMapper.writeValueAsBytes(o);
        } catch (Exception e) {
            LOG.error("Failed to serialize TelegramMessage", e);
        }
        return retVal;
    }

    @Override
    public void close() {
        // Nothing to do
    }
}
