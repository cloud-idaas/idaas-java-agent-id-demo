package com.alibaba.idaas.model;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ChatCompletionChunk {
    private String id;
    private String object = "chat.completion.chunk";
    private long created;
    private String model;
    private List<ChunkChoice> choices;

    public ChatCompletionChunk(){
    }

    public ChatCompletionChunk(String model, ChunkChoice choice) {
        this.id = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "");
        this.created = System.currentTimeMillis() / 1000L;
        this.model = model;
        this.choices = Collections.singletonList(choice);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChunkChoice> getChoices() {
        return choices;
    }

    public void setChoices(List<ChunkChoice> choices) {
        this.choices = choices;
    }
}
