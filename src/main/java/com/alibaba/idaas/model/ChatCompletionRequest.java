package com.alibaba.idaas.model;

import java.util.List;

public class ChatCompletionRequest {
    private String model;
    private List<ChatMessage> messages;
    private Boolean stream;

    public ChatCompletionRequest(){
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }
}
