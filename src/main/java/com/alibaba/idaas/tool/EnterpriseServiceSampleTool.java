package com.alibaba.idaas.tool;

import com.cloud_idaas.core.exception.ConfigException;
import com.cloud_idaas.core.http.*;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnterpriseServiceSampleTool {
    @Tool(name = "enterprise_service_sample", description = "A simple example of enterprise service deployed on Function Compute")
    public String enterpriseServiceSample(@ToolParam(name = "AccessToken", description = "Access Token for accessing the enterprise service.") String accessToken) {
        Map<String, List<String>> headers = new HashMap();
        headers.put("Authorization", Collections.singletonList("Bearer " + accessToken));
        // The public access address of Function Compute, needs to be specified via environment variable
        String enterpriseServiceUrl = System.getenv("ENTERPRISE_SERVICE_URL");
        if (enterpriseServiceUrl == null) {
            throw new ConfigException("ENTERPRISE_SERVICE_URL should be specified via an environment variable.");
        }
        HttpRequest request = new HttpRequest.Builder()
                .httpMethod(HttpMethod.POST)
                .url(enterpriseServiceUrl)
                .headers(headers)
                .build();
        HttpResponse response = HttpClientFactory.getDefaultHttpClient().send(request);
        return response.getBody();
    }
}