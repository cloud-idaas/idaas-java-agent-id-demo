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
        // Remove "Bearer " prefix to extract the Access Token
        accessToken = accessToken.substring(7);

        // Validate the Access Token for accessing the Agent, including signature verification, audience verification, and scope verification
        // Signature verification is performed using the IDaaS instance's JWKS endpoint, which must be specified via environment variable
        // The audience and scope to be validated correspond to the Agent's audience and scope, which must be specified via environment variables
        String jwksEndpoint = System.getenv("JWKS_ENDPOINT");
        if(jwksEndpoint == null){
            throw new ConfigException("JWKS_ENDPOINT should be specified via an environment variable.");
        }
        JwtValidator.validate(jwksEndpoint, accessToken);

        // Initialize Agent
        // The Agent identity mode must be specified via environment variable: Machine or Human
        ReActAgent agent;
        String accessIdentity = System.getenv("ACCESS_IDENTITY");
        if ("Machine".equals(accessIdentity)){
            agent = AgentUtils.createAgentByMachineIdentity();
        } else if ("Human".equals(accessIdentity)){
            agent = AgentUtils.createAgentByHumanIdentity(accessToken);
        } else {
            throw new ConfigException("ACCESS_IDENTITY should be either Machine or User.");
        }

        // Start conversation with the Agent
        return ChatUtils.startChat(agent, request);
    }
}