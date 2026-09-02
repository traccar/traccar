/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class McpToolRegistry {

    public record McpApiTool(String name, String title, String path, McpSchema.JsonSchema inputSchema) {
    }

    private final List<McpApiTool> tools = new ArrayList<>();

    public List<McpApiTool> getTools() {
        return tools;
    }

    public McpToolRegistry() throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("openapi.yaml")) {
            JsonNode root = yamlMapper.readTree(input);
            for (Map.Entry<String, JsonNode> pathEntry : root.get("paths").properties()) {
                JsonNode operation = pathEntry.getValue().get("get");
                if (operation != null && operation.path("x-mcp").asBoolean(false)) {
                    tools.add(parseTool(pathEntry.getKey(), operation));
                }
            }
        }
    }

    private McpApiTool parseTool(String path, JsonNode operation) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (JsonNode parameter : operation.path("parameters")) {
            String name = parameter.get("name").asText();
            properties.put(name, parseParameterSchema(parameter.get("schema")));
            if (parameter.path("required").asBoolean(false)) {
                required.add(name);
            }
        }
        var inputSchema = new McpSchema.JsonSchema(
                "object", properties, required.isEmpty() ? null : required, null, null, null);
        return new McpApiTool(
                operation.get("operationId").asText(), operation.path("summary").asText(""), path, inputSchema);
    }

    private Map<String, Object> parseParameterSchema(JsonNode schema) {
        String type = schema.get("type").asText();
        if (type.equals("array")) {
            return Map.of("type", "array", "items", Map.of("type", schema.path("items").path("type").asText()));
        } else if (schema.has("format")) {
            return Map.of("type", type, "format", schema.get("format").asText());
        } else {
            return Map.of("type", type);
        }
    }

}
