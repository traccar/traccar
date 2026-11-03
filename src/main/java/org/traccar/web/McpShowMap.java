/*
 * Copyright 2026 Joaquim Cardeira (joaquim_cardeira@hotmail.com)
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

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class McpShowMap {

    private static final String RESOURCE_URI = "ui://traccar/map.html";
    private static final String CDN = "https://cdn.jsdelivr.net";
    private static final String TILES = "https://tiles.openfreemap.org";

    private McpShowMap() {
    }

    public static McpServerFeatures.AsyncToolSpecification tool() {
        var schema = McpSchema.Tool.builder("show_map", inputSchema())
                .title("Show map")
                .description("Shows the user a map using maplibre with openfreemap style.")
                .meta(Map.of("ui", Map.of("resourceUri", RESOURCE_URI)))
                .build();
        return new McpServerFeatures.AsyncToolSpecification(schema, (exchange, request) ->
                Mono.just(McpSchema.CallToolResult.builder()
                        .structuredContent(request.arguments())
                        .build()));
    }

    public static McpServerFeatures.AsyncResourceSpecification resource(Config config) {
        var resource = McpSchema.Resource.builder(RESOURCE_URI, "traccar")
                .title("Traccar")
                .build();
        return new McpServerFeatures.AsyncResourceSpecification(resource, (exchange, request) ->
                Mono.fromCallable(() -> McpSchema.TextResourceContents.builder(RESOURCE_URI,
                                        Files.readString(Path.of(config.getString(Keys.WEB_MCP_MAP_PATH))))
                        .mimeType("text/html;profile=mcp-app")
                        .meta(Map.of("ui", Map.of("csp", csp())))
                        .build())
                        .map(contents -> McpSchema.ReadResourceResult.builder(List.of(contents)).build()));
    }

    private static Map<String, Object> csp() {
        return Map.of(
                "connectDomains", List.of(CDN, TILES),
                "resourceDomains", List.of(CDN, TILES),
                "workerDomains", List.of("blob:", CDN));
    }

    private static Map<String, Object> inputSchema() {
        Map<String, Object> layer = new LinkedHashMap<>();
        layer.put("type", "object");
        layer.put("description", "A GeoJSON layer rendered with MapLibre.");
        layer.put("properties", Map.of(
                "data", Map.of(
                        "type", "object",
                        "description", "Inline GeoJSON."),
                "type", Map.of(
                        "type", "string",
                        "enum", List.of("circle", "fill", "line"),
                        "description", "MapLibre layer type."),
                "paint", Map.of(
                        "type", "object", "description", "MapLibre paint."),
                "layout", Map.of(
                        "type", "object", "description", "MapLibre layout.")));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("description", "Render GeoJSON layers and markers on a map.");
        schema.put("properties", Map.of(
                "style", Map.of(
                        "type", "string",
                        "enum", List.of("liberty", "bright", "positron", "dark", "fiord"),
                        "description", "OpenFreeMap basemap, liberty by default."),
                "layers", Map.of("type", "array", "items", layer),
                "markers", Map.of(
                        "type", "array",
                        "items", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "coordinates", Map.of(
                                                "type", "array",
                                                "items", Map.of("type", "number"),
                                                "description", "[longitude, latitude]"),
                                        "label", Map.of("type", "string"),
                                        "color", Map.of("type", "string"),
                                        "style", Map.of("type", "string",
                                                "enum", List.of("pin", "text"),
                                                "description", "pin (default) or text, which shows the label"))))));
        return schema;
    }
}
