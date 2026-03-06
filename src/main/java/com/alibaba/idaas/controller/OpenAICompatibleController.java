package com.alibaba.idaas.controller;

import com.alibaba.idaas.model.*;
import com.alibaba.idaas.util.AgentUtils;
import com.alibaba.idaas.util.ChatUtils;
import com.alibaba.idaas.util.JwtValidator;
import com.cloud_idaas.core.exception.ConfigException;
import io.agentscope.core.ReActAgent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class OpenAICompatibleController {

    @PostMapping("/openai/v1/chat/completions")
    public SseEmitter chatCompletions(@RequestHeader(value = "Authorization", required = true) String accessToken, @RequestBody ChatCompletionRequest request) throws Exception {

        // validate token
        String jwksEndpoint = System.getenv("JWKS_ENDPOINT");
        if(jwksEndpoint == null){
            throw new ConfigException("JWKS_ENDPOINT should be specified via an environment variable.");
        }
        JwtValidator.validate(jwksEndpoint, accessToken);

        // create agent
        ReActAgent agent = AgentUtils.createAgent();

        return ChatUtils.startChat(agent, request);
    }
}