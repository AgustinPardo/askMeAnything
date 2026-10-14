package com.apa.meteoro_mcp_server;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;

/**
 * Tool definitions shared by both transports (stdio and streamable HTTP).
 */
public final class MeteoroTools {

    public static final McpJsonMapper JSON = McpJsonMapper.createDefault();

    private static final MeteoroData DATA = new MeteoroData();

    private static final String NO_ARGS = """
            {"type":"object","properties":{},"required":[]}
            """;

    private static final String RACE_ID_ARGS = """
            {"type":"object",
             "properties":{"raceId":{"type":"string","description":"Race id (R1..R5) or part of the race name"}},
             "required":["raceId"]}
            """;

    private static final String DRIVER_ID_ARGS = """
            {"type":"object",
             "properties":{"driverId":{"type":"string","description":"Driver id (d1..d4). Omit to get all drivers."}}}
            """;

    private MeteoroTools() {
    }

    /**
     * Registers all Meteoro tools on the given server specification.
     *
     * <p>{@code serverInfo(...)} returns the base {@code SyncSpecification<S>} type, so an
     * unchecked cast back to {@code S} is needed to keep chaining ({@code .build()}) working.
     */
    @SuppressWarnings("unchecked")
    public static <S extends McpServer.SyncSpecification<S>> S register(McpServer.SyncSpecification<S> spec) {
        spec.tool(tool("meteoro_race_times",
                "Total and winning times for every Meteoro Grand Prix race in the 2026 season.", NO_ARGS),
                (exchange, arguments) -> text(DATA.allRaceTimes()));

        spec.tool(tool("meteoro_race_info",
                "Detailed information about a single race (circuit, laps, weather, winner, fastest laps).",
                RACE_ID_ARGS),
                (exchange, arguments) -> {
                    String raceId = String.valueOf(arguments.get("raceId"));
                    return DATA.raceInfo(raceId)
                            .<McpSchema.CallToolResult>map(MeteoroTools::text)
                            .orElseGet(() -> new McpSchema.CallToolResult(
                                    "No Meteoro race found for id '" + raceId + "'. Valid ids: R1, R2, R3, R4, R5.",
                                    true));
                });

        spec.tool(tool("meteoro_driver_stats",
                "Driver statistics for the Meteoro championship: points, podiums, wins, fastest laps.",
                DRIVER_ID_ARGS),
                (exchange, arguments) -> {
                    Object driverId = arguments.get("driverId");
                    if (driverId == null || String.valueOf(driverId).isBlank()) {
                        return text(DATA.allDrivers());
                    }
                    return DATA.allDrivers().stream()
                            .filter(d -> String.valueOf(d.get("driverId")).equalsIgnoreCase(String.valueOf(driverId)))
                            .findFirst()
                            .<McpSchema.CallToolResult>map(MeteoroTools::text)
                            .orElseGet(() -> new McpSchema.CallToolResult(
                                    "No Meteoro driver found for id '" + driverId + "'. Valid ids: d1, d2, d3, d4.",
                                    true));
                });

        spec.tool(tool("meteoro_track_list",
                "List of Meteoro circuits with length and number of corners.", NO_ARGS),
                (exchange, arguments) -> text(DATA.allTracks()));

        spec.tool(tool("meteoro_season_summary",
                "Overall Meteoro 2026 season summary: standings, total laps, fastest lap overall.", NO_ARGS),
                (exchange, arguments) -> text(DATA.seasonSummary()));

        return (S) spec;
    }

    private static McpSchema.Tool tool(String name, String description, String inputSchema) {
        return McpSchema.Tool.builder()
                .name(name)
                .description(description)
                .inputSchema(JSON, inputSchema)
                .build();
    }

    private static McpSchema.CallToolResult text(Map<String, Object> payload) {
        try {
            return new McpSchema.CallToolResult(JSON.writeValueAsString(payload), false);
        } catch (Exception e) {
            return new McpSchema.CallToolResult("Could not serialize Meteoro data: " + e.getMessage(), true);
        }
    }

    private static McpSchema.CallToolResult text(List<Map<String, Object>> payload) {
        try {
            return new McpSchema.CallToolResult(JSON.writeValueAsString(payload), false);
        } catch (Exception e) {
            return new McpSchema.CallToolResult("Could not serialize Meteoro data: " + e.getMessage(), true);
        }
    }
}