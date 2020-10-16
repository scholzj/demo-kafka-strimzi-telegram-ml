package cz.scholz.demo.telegram.message;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer messageId;
    private Date date;
    private String text;
    private TelegramMessageUser from;
    private TelegramMessageChat chat;
    private List<TelegramMessagePhoto> photo;
    private Map<String, Object> additionalProperties;

    @JsonProperty("message_id")
    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public TelegramMessageUser getFrom() {
        return from;
    }

    public void setFrom(TelegramMessageUser from) {
        this.from = from;
    }

    public TelegramMessageChat getChat() {
        return chat;
    }

    public void setChat(TelegramMessageChat chat) {
        this.chat = chat;
    }

    public List<TelegramMessagePhoto> getPhoto() {
        return photo;
    }

    public void setPhoto(List<TelegramMessagePhoto> photo) {
        this.photo = photo;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
}
