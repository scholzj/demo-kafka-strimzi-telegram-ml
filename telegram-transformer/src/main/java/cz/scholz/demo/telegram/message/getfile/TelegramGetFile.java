package cz.scholz.demo.telegram.message.getfile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TelegramGetFile {
    private static final Logger LOG = LogManager.getLogger(TelegramGetFile.class);

    private static final String TELEGRAM_API_URI = "https://api.telegram.org";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    private static String getGetFileUri(String apiKey, String fileId)  {
        return TELEGRAM_API_URI + "/bot" + apiKey + "/getFile?file_id=" + fileId;
    }

    private static String getFileUri(String apiKey, String filePath)  {
        return TELEGRAM_API_URI + "/file/bot" + apiKey + "/" + filePath;
    }

    private static TelegramGetFileResponse callGetFile(String apiKey, String fileId)  {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getGetFileUri(apiKey, fileId)))
                .timeout(Duration.ofMinutes(1))
                .GET()
                .build();

        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException|InterruptedException e) {
            LOG.error("Failed to get file name from Telegram getFile API", e);
            return null;
        }

        TelegramGetFileResponse getFileResponse;
        try {
            getFileResponse = mapper.readValue(response.body(), TelegramGetFileResponse.class);
        } catch (Exception e) {
            LOG.error("Failed to deserialize TelegramGetFileResponse", e);
            return null;
        }

        return getFileResponse;
    }

    public static String getFileAddress(String apiKey, String fileId)   {
        TelegramGetFileResponse getFileResponse = callGetFile(apiKey, fileId);
        return getFileUri(apiKey, getFileResponse.getResult().getFilePath());
    }
}
