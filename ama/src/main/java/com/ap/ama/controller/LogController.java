package com.ap.ama.controller;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Read-only view of the logs of both processes running in the container.
 *
 * <pre>
 *   GET /api/logs/ama?lines=500          tail of $log-dir/ama.log
 *   GET /api/logs/meteoro?lines=500      tail of $log-dir/meteoro.log
 *   GET /api/logs/{name}/stream          live tail (text/event-stream)
 * </pre>
 *
 * No authentication: this endpoint is meant for a demo deployment.
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {

    private static final Logger log = LoggerFactory.getLogger(LogController.class);

    private static final int DEFAULT_LINES = 500;
    private static final int MAX_LINES = 5000;
    private static final long STREAM_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Path logDir;

    public LogController(@Value("${log-dir:/tmp/ama}") String logDir) {
        this.logDir = Paths.get(logDir);
    }

    @GetMapping(value = "/{name}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String tail(@PathVariable String name,
                       @RequestParam(defaultValue = "" + DEFAULT_LINES) int lines) {
        Path file = fileFor(name);
        if (file == null) {
            return unknownLog(name);
        }
        return read(file, clamp(lines));
    }

    @GetMapping(value = "/{name}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String name) {
        Path file = fileFor(name);
        if (file == null) {
            throw new IllegalArgumentException(unknownLog(name));
        }
        long startAt = size(file);
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        Thread watcher = new Thread(() -> tailLoop(file, emitter, startAt), "log-tail-" + name);
        watcher.setDaemon(true);
        watcher.start();
        return emitter;
    }

    private void tailLoop(Path file, SseEmitter emitter, long offset) {
        long position = offset;
        while (true) {
            try {
                long size = size(file);
                if (size > position) {
                    String chunk = readFrom(file, position, size - position);
                    if (!chunk.isEmpty()) {
                        emitter.send(SseEmitter.event().data(chunk));
                    }
                    position = size;
                }
                Thread.sleep(1000);
            } catch (IOException | IllegalStateException e) {
                // client disconnected or the file went away
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private Path fileFor(String name) {
        return switch (name.toLowerCase()) {
            case "ama" -> logDir.resolve("ama.log");
            case "meteoro" -> logDir.resolve("meteoro.log");
            default -> null;
        };
    }

    private String unknownLog(String name) {
        return "Unknown log '" + name + "'. Available: ama, meteoro.";
    }

    private static int clamp(int lines) {
        return Math.min(Math.max(lines, 1), MAX_LINES);
    }

    private static long size(Path file) {
        try {
            return Files.exists(file) ? Files.size(file) : 0L;
        } catch (IOException e) {
            return 0L;
        }
    }

    /** Reads the last {@code lines} lines of a file. */
    private static String read(Path file, int lines) {
        if (!Files.exists(file)) {
            return "No log file yet at " + file + "\n";
        }
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long length = raf.length();
            int maxBytes = lines * 200;
            int start = (int) Math.max(0, length - maxBytes);
            raf.seek(start);
            byte[] buffer = new byte[(int) (length - start)];
            raf.readFully(buffer);
            List<String> all = new ArrayList<>(
                    List.of(new String(buffer, StandardCharsets.UTF_8).split("\\R", -1)));
            if (start > 0 && !all.isEmpty()) {
                all.remove(0); // first line may be cut in half
            }
            int from = Math.max(0, all.size() - lines);
            List<String> selected = new ArrayList<>(all.subList(from, all.size()));
            Collections.reverse(selected);
            return String.join("\n", selected);
        } catch (IOException e) {
            return "Could not read " + file + ": " + e.getMessage() + "\n";
        }
    }

    /** Reads {@code length} bytes starting at {@code offset}. */
    private static String readFrom(Path file, long offset, long length) throws IOException {
        if (length <= 0) {
            return "";
        }
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long size = raf.length();
            long from = Math.max(0, Math.min(offset, size));
            int count = (int) Math.min(length, size - from);
            byte[] buffer = new byte[count];
            raf.seek(from);
            raf.readFully(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }

    @GetMapping
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.OK)
    public String index() {
        return """
                AMA log endpoints:
                  /api/logs/ama?lines=500
                  /api/logs/meteoro?lines=500
                  /api/logs/ama/stream        (text/event-stream)
                  /api/logs/meteoro/stream    (text/event-stream)
                Log directory: %s
                """.formatted(logDir);
    }
}