package com.alibaba.idaas.model;

public class ChunkChoice {
    private int index;
    private ChatMessage delta;
    private String finish_reason;

    public ChunkChoice() {
    }

    public ChunkChoice(int index, ChatMessage delta) {
        this.index = index;
        this.delta = delta;
    }

    public ChunkChoice(int index, String finishReason) {
        this.index = index;
        this.finish_reason = finishReason;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public ChatMessage getDelta() {
        return delta;
    }

    public void setDelta(ChatMessage delta) {
        this.delta = delta;
    }

    public String getFinish_reason() {
        return finish_reason;
    }

    public void setFinish_reason(String finish_reason) {
        this.finish_reason = finish_reason;
    }
}
