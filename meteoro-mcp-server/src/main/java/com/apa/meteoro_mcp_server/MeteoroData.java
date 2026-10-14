package com.apa.meteoro_mcp_server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory, intentionally FICTIONAL racing data for the "Meteoro" championship.
 * None of this data exists in any real LLM training set, which is the whole point:
 * it can only be answered by calling this MCP server.
 */
public final class MeteoroData {

    public record Lap(String driverId, int lapNumber, String lapTime) {
    }

    public record Race(
            String id,
            String name,
            String circuit,
            String country,
            String date,
            int laps,
            String weather,
            String winnerDriverId,
            String winningTime,
            List<Lap> bestLaps) {
    }

    public record Driver(
            String id,
            String name,
            String team,
            String nationality,
            int points,
            int podiums,
            int wins,
            String fastestLap) {
    }

    public record Track(String id, String name, String country, double lengthKm, int corners) {
    }

    private final List<Race> races = List.of(
            new Race("R1", "Meteoro Grand Prix - Aurora", "Frostpeak Circuit", "Norway", "2026-03-14", 58,
                    "Dry, 8C", "d1", "1:32:44.318",
                    List.of(
                            new Lap("d1", 1, "1:31.004"),
                            new Lap("d3", 1, "1:30.882"),
                            new Lap("d2", 1, "1:31.771"))),
            new Race("R2", "Meteoro Grand Prix - Dust Devil", "Cinder Ridge", "Argentina", "2026-04-05", 62,
                    "Hot dust, 31C", "d3", "1:44:09.771",
                    List.of(
                            new Lap("d3", 1, "1:39.550"),
                            new Lap("d1", 1, "1:39.905"),
                            new Lap("d4", 1, "1:40.112"))),
            new Race("R3", "Meteoro Grand Prix - Nightfall", "Vanta Loop", "Singapore", "2026-05-02", 54,
                    "Night, humid", "d2", "1:38:22.104",
                    List.of(
                            new Lap("d2", 1, "1:36.443"),
                            new Lap("d4", 1, "1:36.902"),
                            new Lap("d1", 1, "1:37.115"))),
            new Race("R4", "Meteoro Grand Prix - Comet", "Halcyon Speedway", "Australia", "2026-06-20", 49,
                    "Clear, 19C", "d1", "1:28:55.640",
                    List.of(
                            new Lap("d1", 1, "1:34.201"),
                            new Lap("d2", 1, "1:34.560"),
                            new Lap("d3", 1, "1:34.988"))),
            new Race("R5", "Meteoro Grand Prix - Stormfront", "Tempest Bay", "Japan", "2026-08-09", 57,
                    "Heavy rain", "d4", "1:51:07.229",
                    List.of(
                            new Lap("d4", 1, "1:42.310"),
                            new Lap("d3", 1, "1:42.744"),
                            new Lap("d1", 1, "1:43.006"))));

    private final List<Driver> drivers = List.of(
            new Driver("d1", "Lena Vargas", "Meteoro Works", "Spain", 148, 5, 2, "1:31.004"),
            new Driver("d2", "Kenji Mori", "Apex Vector", "Japan", 121, 3, 0, "1:36.443"),
            new Driver("d3", "Ana Duarte", "Vortex Racing", "Brazil", 134, 4, 1, "1:30.882"),
            new Driver("d4", "Oscar Lindqvist", "Polar Nine", "Sweden", 96, 2, 1, "1:42.310"));

    private final List<Track> tracks = List.of(
            new Track("t1", "Frostpeak Circuit", "Norway", 4.82, 14),
            new Track("t2", "Cinder Ridge", "Argentina", 5.11, 9),
            new Track("t3", "Vanta Loop", "Singapore", 4.05, 19),
            new Track("t4", "Halcyon Speedway", "Australia", 6.44, 11),
            new Track("t5", "Tempest Bay", "Japan", 5.90, 16));

    /** Total / winning time for every race, as text. */
    public List<Map<String, Object>> allRaceTimes() {
        return races.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("raceId", r.id());
            m.put("race", r.name());
            m.put("circuit", r.circuit());
            m.put("date", r.date());
            m.put("laps", r.laps());
            m.put("winningTime", r.winningTime());
            m.put("winner", driverName(r.winnerDriverId()));
            m.put("fastestLap", fastestLapOf(r));
            return m;
        }).toList();
    }

    public Optional<Map<String, Object>> raceInfo(String raceId) {
        return races.stream()
                .filter(r -> r.id().equalsIgnoreCase(raceId) || r.name().toLowerCase().contains(raceId.toLowerCase()))
                .findFirst()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("raceId", r.id());
                    m.put("race", r.name());
                    m.put("circuit", r.circuit());
                    m.put("country", r.country());
                    m.put("date", r.date());
                    m.put("laps", r.laps());
                    m.put("weather", r.weather());
                    m.put("winner", driverName(r.winnerDriverId()));
                    m.put("winningTime", r.winningTime());
                    m.put("fastestLaps", r.bestLaps());
                    return m;
                });
    }

    public List<Map<String, Object>> allDrivers() {
        return drivers.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("driverId", d.id());
            m.put("name", d.name());
            m.put("team", d.team());
            m.put("nationality", d.nationality());
            m.put("points", d.points());
            m.put("podiums", d.podiums());
            m.put("wins", d.wins());
            m.put("fastestLap", d.fastestLap());
            return m;
        }).toList();
    }

    public List<Map<String, Object>> allTracks() {
        return tracks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("trackId", t.id());
            m.put("name", t.name());
            m.put("country", t.country());
            m.put("lengthKm", t.lengthKm());
            m.put("corners", t.corners());
            return m;
        }).toList();
    }

    public Map<String, Object> seasonSummary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("championship", "Meteoro Grand Prix 2026");
        m.put("racesCompleted", races.size());
        m.put("totalLaps", races.stream().mapToInt(Race::laps).sum());
        m.put("driverStandings", allDrivers());
        m.put("fastestLapOverall", drivers.stream()
                .min((a, b) -> Double.compare(seconds(a.fastestLap()), seconds(b.fastestLap())))
                .map(d -> d.name() + " - " + d.fastestLap())
                .orElse("n/a"));
        m.put("note", "All data is fictional and generated for demo purposes.");
        return m;
    }

    private String driverName(String driverId) {
        return drivers.stream()
                .filter(d -> d.id().equals(driverId))
                .map(Driver::name)
                .findFirst()
                .orElse(driverId);
    }

    private String fastestLapOf(Race r) {
        return r.bestLaps().stream()
                .min((a, b) -> Double.compare(seconds(a.lapTime()), seconds(b.lapTime())))
                .map(l -> driverName(l.driverId()) + " - " + l.lapTime())
                .orElse("n/a");
    }

    private static double seconds(String lapTime) {
        String[] parts = lapTime.split(":");
        double total = 0;
        for (String part : parts) {
            total = total * 60 + Double.parseDouble(part);
        }
        return total;
    }
}