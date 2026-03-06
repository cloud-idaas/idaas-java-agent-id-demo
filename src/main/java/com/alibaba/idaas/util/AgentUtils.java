package com.alibaba.idaas.util;

import com.alibaba.idaas.tool.FcSampleTool;
import com.cloud_idaas.core.exception.ConfigException;
import com.cloud_idaas.core.factory.IDaaSCredentialProviderFactory;
import com.cloud_idaas.core.provider.IDaaSCredentialProvider;
import com.cloud_idaas.pam.IDaaSPamClient;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;

import java.time.Duration;
import java.util.Map;

public class AgentUtils {

    public static ReActAgent createAgent() {

        IDaaSCredentialProviderFactory.init();

        IDaaSPamClient client = IDaaSPamClient.builder().build();
        String llmApiKeyIdentifier = System.getenv("LLM_API_KEY_IDENTIFIER");
        if (llmApiKeyIdentifier == null){
            throw new ConfigException("LLM_API_KEY_IDENTIFIER should be specified via an environment variable.");
        }
        String llmApiKey = client.getApiKey(llmApiKeyIdentifier);

        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(llmApiKey)
                .modelName("qwen-plus")
                .stream(true)
                .enableThinking(true)
                .formatter(new DashScopeChatFormatter())
                .defaultOptions(GenerateOptions.builder()
                        .thinkingBudget(5000)
                        .build())
                .build();

        Toolkit toolkit = new Toolkit();
        String mcpServerApiKeyIdentifier = System.getenv("MCP_SERVER_API_KEY_IDENTIFIER");
        if (mcpServerApiKeyIdentifier == null){
            throw new ConfigException("MCP_SERVER_API_KEY_IDENTIFIER should be specified via an environment variable.");
        }
        String mcpServerApiKey = client.getApiKey(mcpServerApiKeyIdentifier);

        String mcpServerUrl = System.getenv("MCP_SERVER_URL");
        if (mcpServerUrl == null){
            throw new ConfigException("MAP_MCP_SERVER_URL should be specified via an environment variable.");
        }
        McpClientWrapper mapClient = McpClientBuilder.create("Amap-Maps")
                .sseTransport(mcpServerUrl)
                .header("Authorization", "Bearer " + mcpServerApiKey)
                .timeout(Duration.ofSeconds(60))
                .buildAsync()
                .block();

        toolkit.registerMcpClient(mapClient).block();

        String mcpScope = System.getenv("MCP_SCOPE");
        if (mcpScope == null){
            throw new ConfigException("MCP_SCOPE should be specified via an environment variable.");
        }
        IDaaSCredentialProvider credentialProvider = IDaaSCredentialProviderFactory.getIDaaSCredentialProvider(mcpScope);
        String AT_mcp = credentialProvider.getBearerToken();

        toolkit.registration()
                .tool(new FcSampleTool())
                .presetParameters(Map.of("fc_sample", Map.of("AccessToken", AT_mcp)))
                .apply();

        return ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("You are a helpful AI assistant. Be friendly and concise, and think and reply in chinese.")
                .model(model)
                .memory(new InMemoryMemory())
                .toolkit(toolkit)
                .build();
    }
}
