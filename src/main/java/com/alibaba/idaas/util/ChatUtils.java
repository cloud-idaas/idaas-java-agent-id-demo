package com.alibaba.idaas.util;

import com.alibaba.idaas.model.ChatCompletionChunk;
import com.alibaba.idaas.model.ChatCompletionRequest;
import com.alibaba.idaas.model.ChatMessage;
import com.alibaba.idaas.model.ChunkChoice;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ChatUtils {

    public static SseEmitter startChat(Agent agent, ChatCompletionRequest request) throws Exception {

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        String userMessage = extractUserMessage(request);

        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(userMessage).build())
                .build();

        sendChunk(emitter, "qwen-plus", new ChatMessage("assistant", ""), null);

        AtomicBoolean hasSentThinkingHeader = new AtomicBoolean(false);
        AtomicBoolean hasSentTextHeader = new AtomicBoolean(false);
        AtomicBoolean hasSentTextSeparator = new AtomicBoolean(false);
        AtomicReference<String> lastThinkingContent = new AtomicReference<>("");
        AtomicReference<String> lastTextContent = new AtomicReference<>("");

        StreamOptions streamOptions = StreamOptions.builder()
                .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
                .incremental(true)
                .includeReasoningResult(false)
                .build();

        agent.stream(userMsg, streamOptions)
                .doOnNext(event -> {
                    Msg msg = event.getMessage();
                    for (ContentBlock block : msg.getContent()) {
                        try {
                            if (block instanceof ThinkingBlock) {
                                String thinking = ((ThinkingBlock) block).getThinking();
                                sendThinkingDelta(emitter, "qwen-plus", thinking, hasSentThinkingHeader, lastThinkingContent);
                            } else if (block instanceof TextBlock) {
                                String text = ((TextBlock) block).getText();
                                sendTextDelta(emitter, "qwen-plus", text, hasSentThinkingHeader, hasSentTextHeader, hasSentTextSeparator,lastTextContent);
                            }
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                            return;
                        }
                    }
                })
                .doOnComplete(() -> {
                    try {
                        sendChunk(emitter, "qwen-plus", null, "stop");
                        emitter.send(SseEmitter.event().data("[DONE]"));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                })
                .doOnError(emitter::completeWithError)
                .subscribe();


        return emitter;
    }

    private static String extractUserMessage(ChatCompletionRequest request) {
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            for (int i = request.getMessages().size() - 1; i >= 0; i--) {
                ChatMessage msg = request.getMessages().get(i);
                if ("user".equals(msg.getRole())) {
                    return msg.getContent();
                }
            }
        }
        return "";
    }

    private static void sendThinkingDelta(
            SseEmitter emitter,
            String model,
            String currentThinking,
            AtomicBoolean hasSentHeader,
            AtomicReference<String> lastThinkingRef) throws Exception {

        String last = lastThinkingRef.get();
        String toSend;
        if (currentThinking.startsWith(last)) {
            toSend = currentThinking.substring(last.length());
            lastThinkingRef.set(currentThinking);
        } else {
            toSend = currentThinking;
            lastThinkingRef.set(last + currentThinking);
        }

        if (!toSend.isEmpty()) {
            if (!hasSentHeader.get()) {
                sendChunk(emitter, model, new ChatMessage(null, "Thinking: "), null);
                hasSentHeader.set(true);
            }
            sendChunk(emitter, model, new ChatMessage(null, toSend), null);
        }
    }

    private static void sendTextDelta(
            SseEmitter emitter,
            String model,
            String currentText,
            AtomicBoolean hasSentThinkingHeader,
            AtomicBoolean hasSentTextHeader,
            AtomicBoolean hasSentTextSeparator,
            AtomicReference<String> lastTextRef) throws Exception {

        String last = lastTextRef.get();
        String toSend;

        if (currentText.startsWith(last)) {
            toSend = currentText.substring(last.length());
            lastTextRef.set(currentText);
        } else {
            toSend = currentText;
            lastTextRef.set(last + currentText);
        }

        if (!toSend.isEmpty()) {
            if (hasSentThinkingHeader.get() && !hasSentTextSeparator.get()) {
                sendChunk(emitter, model, new ChatMessage(null, "\n\n"), null);
                hasSentTextSeparator.set(true);
            }
            if (!hasSentTextHeader.get()) {
                sendChunk(emitter, model, new ChatMessage(null, "Text: "), null);
                hasSentTextHeader.set(true);
            }
            sendChunk(emitter, model, new ChatMessage(null, toSend), null);
        }
    }


    private static void sendChunk(SseEmitter emitter, String model, ChatMessage delta, String finishReason) throws Exception {
        ChunkChoice choice = (finishReason != null)
                ? new ChunkChoice(0, finishReason)
                : new ChunkChoice(0, delta);
        ChatCompletionChunk chunk = new ChatCompletionChunk(model, choice);
        emitter.send(SseEmitter.event().data(chunk));
    }
}
