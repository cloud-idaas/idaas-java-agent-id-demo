package com.alibaba.idaas.tool;

import com.cloud_idaas.core.exception.ConfigException;
import com.cloud_idaas.core.http.*;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FcSampleTool {

    @Tool(name = "fc_sample", description = "企业服务部署在函数计算的简单示例")
    public String fcSample(@ToolParam(name = "AccessToken", description = "访问企业服务的Access Token。") String accessToken) {
        Map<String, List<String>> headers = new HashMap();
        headers.put("Authorization", Collections.singletonList("Bearer " + accessToken));
        String fcUrl = System.getenv("FUNCTION_CALL_URL");
        if (fcUrl == null) {
            throw new ConfigException("FUNCTION_CALL_URL should be specified via an environment variable.");
        }
        HttpRequest request = new HttpRequest.Builder()
                .httpMethod(HttpMethod.POST)
                .url(fcUrl)
                .headers(headers)
                .build();
        HttpResponse response = HttpClientFactory.getDefaultHttpClient().send(request);
        return response.getBody();
    }
}