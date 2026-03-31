package com.alibaba.idaas.util;

import com.alibaba.idaas.tool.EnterpriseServiceSampleTool;
import com.cloud_idaas.core.credential.IDaaSCredential;
import com.cloud_idaas.core.domain.constants.OAuth2Constants;
import com.cloud_idaas.core.exception.ConfigException;
import com.cloud_idaas.core.factory.IDaaSCredentialProviderFactory;
import com.cloud_idaas.core.implementation.StaticIDaaSCredentialProvider;
import com.cloud_idaas.core.provider.IDaaSCredentialProvider;
import com.cloud_idaas.core.provider.IDaaSTokenExchangeCredentialProvider;
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

    public static ReActAgent createAgentByMachineIdentity() {

        // Read SDK configuration file and complete IDaaS configuration initialization
        // To use IDaaS SDK to retrieve credentials hosted in IDaaS, this initialization method must be completed first
        IDaaSCredentialProviderFactory.init();

        // Create IDaaS SDK client to retrieve hosted credentials as M2M client identity
        IDaaSPamClient client = IDaaSPamClient.builder().build();

        // Example: Create Agent Model based on Model Studio platform
        // Retrieve hosted LLM API Key here to initialize the Agent's Model
        // API Key identifier needs to be specified via environment variable
        String llmApiKeyIdentifier = System.getenv("LLM_API_KEY_IDENTIFIER");
        if (llmApiKeyIdentifier == null){
            throw new ConfigException("LLM_API_KEY_IDENTIFIER should be specified via an environment variable.");
        }
        String llmApiKey = client.getApiKey(llmApiKeyIdentifier);

        // Initialize Model Studio qwen-plus Model
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

        // Example: Use Amap MCP Server on Model Studio platform as external service
        // Retrieve hosted external service API Key here to construct Tool for accessing external service
        // API Key identifier needs to be specified via environment variable
        String externalServerApiKeyIdentifier = System.getenv("EXTERNAL_SERVER_API_KEY_IDENTIFIER");
        if (externalServerApiKeyIdentifier == null){
            throw new ConfigException("EXTERNAL_SERVER_API_KEY_IDENTIFIER should be specified via an environment variable.");
        }
        String externalServerApiKey = client.getApiKey(externalServerApiKeyIdentifier);

        // Amap MCP Server SSE Endpoint needs to be specified via environment variable
        String externalServerUrl = System.getenv("EXTERNAL_SERVER_URL");
        if (externalServerUrl == null){
            throw new ConfigException("EXTERNAL_SERVER_URL should be specified via an environment variable.");
        }
        // Construct Tool for accessing Amap MCP Server
        McpClientWrapper externalServerClient = McpClientBuilder.create("Amap-Maps")
                .streamableHttpTransport(externalServerUrl)
                .header("Authorization", "Bearer " + externalServerApiKey)
                .timeout(Duration.ofSeconds(60))
                .buildAsync()
                .block();

        toolkit.registerMcpClient(externalServerClient).block();

        // Example: Deploy enterprise service based on Function Compute
        // When Agent accesses enterprise service, AccessToken is required, and scope needs to be specified via environment variable
        // Scope format is audience identifier + "|" + permission identifier, corresponding to enterprise service application's audience and permission identifiers
        String enterpriseServiceScope = System.getenv("ENTERPRISE_SERVICE_SCOPE");
        if (enterpriseServiceScope == null){
            throw new ConfigException("ENTERPRISE_SERVICE_SCOPE should be specified via an environment variable.");
        }
        // Retrieve IDaaS credential provider for obtaining credentials to access enterprise service
        IDaaSCredentialProvider credentialProvider = IDaaSCredentialProviderFactory.getIDaaSCredentialProvider(enterpriseServiceScope);
        String AT_s = credentialProvider.getBearerToken();

        // Register custom Tool for accessing enterprise service
        toolkit.registration()
                .tool(new EnterpriseServiceSampleTool())
                .presetParameters(Map.of("enterprise_service_sample", Map.of("AccessToken", AT_s)))
                .apply();

        // Initialize Agent with configured Model Studio Model, Amap MCP Server Tool, and enterprise service Tool
        return ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("You are a helpful AI assistant. Be friendly and concise.")
                .model(model)
                .memory(new InMemoryMemory())
                .toolkit(toolkit)
                .build();
    }

    public static ReActAgent createAgentByHumanIdentity(String accessToken) {

        // Read SDK configuration file and complete IDaaS configuration initialization
        // To use IDaaS SDK to retrieve credentials hosted in IDaaS, this initialization method must be completed first
        IDaaSCredentialProviderFactory.init();

        // Retrieve IDaaS Token Exchange credential provider for exchanging credentials to access IDaaS with user identity
        IDaaSTokenExchangeCredentialProvider defaultTokenExchangeProvider = IDaaSCredentialProviderFactory.getIDaaSTokenExchangeCredentialProvider();
        // Exchange AT_u for credentials to access IDaaS with user identity
        IDaaSCredential credential = defaultTokenExchangeProvider.getCredential(accessToken, OAuth2Constants.ACCESS_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE);

        // Build static credential provider with user identity credentials
        IDaaSCredentialProvider staticCredentialProvider = StaticIDaaSCredentialProvider.builder()
                .setCredential(credential)
                .build();
        // Create IDaaS SDK client to retrieve hosted credentials with user identity
        IDaaSPamClient client = IDaaSPamClient.builder()
                .credentialProvider(staticCredentialProvider)
                .build();

        // Example: Create Agent Model based on Model Studio platform
        // Retrieve hosted LLM API Key here to initialize the Agent's Model
        // API Key identifier needs to be specified via environment variable
        String llmApiKeyIdentifier = System.getenv("LLM_API_KEY_IDENTIFIER");
        if (llmApiKeyIdentifier == null){
            throw new ConfigException("LLM_API_KEY_IDENTIFIER should be specified via an environment variable.");
        }
        String llmApiKey = client.getApiKey(llmApiKeyIdentifier);

        // Initialize Model Studio qwen-plus Model
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

        // Example: Use Amap MCP Server on Model Studio platform as external service
        // Retrieve hosted external service API Key here to construct Tool for accessing external service
        // API Key identifier needs to be specified via environment variable
        String externalServerApiKeyIdentifier = System.getenv("EXTERNAL_SERVER_API_KEY_IDENTIFIER");
        if (externalServerApiKeyIdentifier == null){
            throw new ConfigException("EXTERNAL_SERVER_API_KEY_IDENTIFIER should be specified via an environment variable.");
        }
        String externalServerApiKey = client.getApiKey(externalServerApiKeyIdentifier);

        // Amap MCP Server SSE Endpoint needs to be specified via environment variable
        String externalServerUrl = System.getenv("EXTERNAL_SERVER_URL");
        if (externalServerUrl == null){
            throw new ConfigException("EXTERNAL_SERVER_URL should be specified via an environment variable.");
        }
        // Construct Tool for accessing Amap MCP Server
        McpClientWrapper externalServerClient = McpClientBuilder.create("Amap-Maps")
                .streamableHttpTransport(externalServerUrl)
                .header("Authorization", "Bearer " + externalServerApiKey)
                .timeout(Duration.ofSeconds(60))
                .buildAsync()
                .block();

        toolkit.registerMcpClient(externalServerClient).block();

        // Example: Deploy enterprise service based on Function Compute
        // When Agent accesses enterprise service, AccessToken is required, and scope needs to be specified via environment variable
        // Scope format is audience identifier + "|" + permission identifier, corresponding to enterprise service application's audience and permission identifiers
        String enterpriseServiceScope = System.getenv("ENTERPRISE_SERVICE_SCOPE");
        if (enterpriseServiceScope == null){
            throw new ConfigException("ENTERPRISE_SERVICE_SCOPE should be specified via an environment variable.");
        }

        // Retrieve IDaaS Token Exchange credential provider for exchanging credentials to access enterprise service with user identity
        IDaaSTokenExchangeCredentialProvider enterpriseServiceTokenExchangeProvider = IDaaSCredentialProviderFactory.getIDaaSTokenExchangeCredentialProvider(enterpriseServiceScope);
        // Exchange AT_u for credentials to access enterprise service with user identity
        String AT_s = enterpriseServiceTokenExchangeProvider.getIssuedToken(accessToken, OAuth2Constants.ACCESS_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE);

        // Register custom Tool for accessing enterprise service
        toolkit.registration()
                .tool(new EnterpriseServiceSampleTool())
                .presetParameters(Map.of("enterprise_service_sample", Map.of("AccessToken", AT_s)))
                .apply();

        // Initialize Agent with configured Model Studio Model, Amap MCP Server Tool, and enterprise service Tool
        return ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("You are a helpful AI assistant. Be friendly and concise.")
                .model(model)
                .memory(new InMemoryMemory())
                .toolkit(toolkit)
                .build();
    }
}
