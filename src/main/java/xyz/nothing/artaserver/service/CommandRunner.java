package xyz.nothing.artaserver.service;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandRunner {
    public static String run(long timeoutSeconds, String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // merge stderr into stdout

        try {
            Process process = pb.start();

            // Read output on a separate thread so a full pipe buffer can't deadlock us
            CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> {
                try (BufferedReader reader = process.inputReader()) {
                    return reader.lines().collect(Collectors.joining("\n"));
                } catch (IOException e) {
                    return "Error reading output: " + e.getMessage();
                }
            });

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "Command timed out after " + timeoutSeconds + "s";
            }

            return outputFuture.get(5, TimeUnit.SECONDS);

        } catch (Exception e) {
            return "Failed to run command: " + e.getMessage();
        }
    }
}
