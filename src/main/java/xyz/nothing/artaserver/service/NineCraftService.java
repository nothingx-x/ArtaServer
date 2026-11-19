package xyz.nothing.artaserver.service;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public final class NineCraftService implements AutoCloseable {

    private static final String API_BASE = "https://api.9craft.ir/api";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private final String token;
    private final String serverName;
    private final HttpClient client;
    private final ExecutorService httpExecutor;

    public NineCraftService(String token, String serverName) {
        this.token = Objects.requireNonNull(token, "token");
        this.serverName = Objects.requireNonNull(serverName, "serverName");
        this.httpExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .executor(httpExecutor)
                .build();
    }

    public String serverName() {
        return serverName;
    }

    /** Starts the managed server. */
    public CompletableFuture<ApiResult<SimpleMessage>> startServer() {
        return send(worldAction("/server/start"), SimpleMessage.class);
    }

    /** Stops the managed server. */
    public CompletableFuture<ApiResult<SimpleMessage>> stopServer() {
        return send(worldAction("/server/stop"), SimpleMessage.class);
    }

    /** Restarts the managed server, letting it save first. */
    public CompletableFuture<ApiResult<SimpleMessage>> restartServer() {
        return send(worldAction("/server/restart"), SimpleMessage.class);
    }

    /**
     * Force-restarts the managed server without waiting for a save. Only for a frozen server.
     */
    public CompletableFuture<ApiResult<SimpleMessage>> forceRestartServer() {
        return send(worldAction("/server/force_restart"), SimpleMessage.class);
    }

    /** Runs a console command and waits for its output ({@code use_rcon = true}). */
    public CompletableFuture<ApiResult<CmdResponse>> runCommand(String command) {
        return runCommand(command, true);
    }

    /**
     * Runs a console command on the managed server.
     *
     * @param command the command, leading slash optional (e.g. {@code whitelist add Steve})
     * @param useRcon {@code false} if the command output is not needed
     */
    public CompletableFuture<ApiResult<CmdResponse>> runCommand(String command, boolean useRcon) {
        Objects.requireNonNull(command, "command");
        JsonObject body = new JsonObject();
        body.addProperty("world_name", serverName);
        body.addProperty("cmd", command);
        body.addProperty("use_rcon", useRcon);
        return send(post("/minecraft/server/cmd", body), CmdResponse.class);
    }

    /** Lists every game server on the account. */
    public CompletableFuture<ApiResult<ServerListResponse>> listServers() {
        return send(get("/servers"), ServerListResponse.class);
    }

    /** Returns details of the managed server. */
    public CompletableFuture<ApiResult<ServerData>> getServerData() {
        return getServerData(serverName);
    }

    /** Returns details of one server. */
    public CompletableFuture<ApiResult<ServerData>> getServerData(String worldName) {
        Objects.requireNonNull(worldName, "worldName");
        return send(get("/server/data?world_name=" + encode(worldName)), ServerData.class);
    }


    private <T> CompletableFuture<ApiResult<T>> send(HttpRequest request, Class<T> bodyType) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> parse(response, bodyType))
                .exceptionally(this::networkFailure);
    }

    private <T> ApiResult<T> parse(HttpResponse<String> response, Class<T> bodyType) {
        int status = response.statusCode();
        String body = response.body();

        if (status < 200 || status >= 300) {
            return ApiResult.fail(status, parseError(body));
        }

        if (body == null || body.isBlank()) {
            return ApiResult.ok(status, null);
        }

        try {
            return ApiResult.ok(status, GSON.fromJson(body, bodyType));
        } catch (JsonParseException | IllegalStateException e) {
            return ApiResult.fail(status, new ApiError(
                    "INVALID_RESPONSE",
                    "9Craft returned a response that could not be parsed",
                    null,
                    e.getMessage(),
                    null));
        }
    }

    private <T> ApiResult<T> networkFailure(Throwable throwable) {
        Throwable cause = throwable;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }

        if (cause instanceof HttpTimeoutException) {
            return ApiResult.fail(0, new ApiError(
                    "TIMEOUT",
                    "The request to 9Craft timed out",
                    null,
                    cause.getMessage(),
                    null));
        }

        return ApiResult.fail(0, new ApiError(
                "NETWORK_ERROR",
                "Could not reach the 9Craft API",
                null,
                cause.getClass().getSimpleName() + (cause.getMessage() == null ? "" : ": " + cause.getMessage()),
                null));
    }

    private HttpRequest worldAction(String path) {
        JsonObject body = new JsonObject();
        body.addProperty("world_name", serverName);
        return post(path, body);
    }

    private HttpRequest post(String path, JsonObject body) {
        return HttpRequest.newBuilder(uri(path))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "api_key " + token)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8))
                .build();
    }

    private HttpRequest get(String pathAndQuery) {
        return HttpRequest.newBuilder(uri(pathAndQuery))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "api_key " + token)
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    private static URI uri(String pathAndQuery) {
        return URI.create(API_BASE + pathAndQuery);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static ApiError parseError(String body) {
        if (body == null || body.isBlank()) {
            return new ApiError("HTTP_ERROR", "9Craft returned an error", null, null, null);
        }

        try {
            JsonElement element = JsonParser.parseString(body);
            if (!element.isJsonObject()) {
                return new ApiError("HTTP_ERROR", "9Craft returned an error", null, body, null);
            }

            JsonObject object = element.getAsJsonObject();
            String code = string(object, "error_code");
            String english = string(object, "msg_en");
            String persian = string(object, "msg_fa");
            String raw = firstNonNull(
                    string(object, "message"),
                    string(object, "error"),
                    string(object, "msg"));

            Integer retryAfter = null;
            JsonElement retry = object.get("retry_after");
            if (retry != null && retry.isJsonPrimitive() && retry.getAsJsonPrimitive().isNumber()) {
                retryAfter = retry.getAsInt();
            }

            return new ApiError(
                    code == null ? "HTTP_ERROR" : code,
                    english,
                    persian,
                    raw,
                    retryAfter);
        } catch (RuntimeException e) {
            return new ApiError("HTTP_ERROR", "9Craft returned an error", null, body, null);
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Stops the virtual-thread executor. In-flight requests are interrupted, so call this during
     * shutdown only.
     */
    @Override
    public void close() {
        httpExecutor.shutdownNow();
    }

    /**
     * Outcome of an API call. Exactly one of {@link #data()} and {@link #error()} is non-null.
     * {@code statusCode} is {@code 0} for network/timeout failures.
     */
    public record ApiResult<T>(int statusCode, T data, ApiError error) {

        public boolean isSuccess() {
            return error == null;
        }

        public static <T> ApiResult<T> ok(int statusCode, T data) {
            return new ApiResult<>(statusCode, data, null);
        }

        public static <T> ApiResult<T> fail(int statusCode, ApiError error) {
            return new ApiResult<>(statusCode, null, Objects.requireNonNull(error, "error"));
        }

        public T dataOrThrow() {
            if (error != null) {
                String detail = firstNonNull(error.messageEn(), error.message(), error.code());
                throw new IllegalStateException("9Craft request failed: " + detail);
            }
            return data;
        }
    }

    /**
     * Error payload. Rely on {@link #code()} in code, and show {@link #messageEn()} or
     * {@link #messageFa()} to users.
     *
     * @param code              stable error code such as {@code SERVER_NOT_FOUND}
     * @param messageEn         English text suitable for users
     * @param messageFa         Persian text suitable for users
     * @param message           raw debug message from the API
     * @param retryAfterSeconds seconds to wait after a {@code 429}, otherwise null
     */
    public record ApiError(
            String code,
            String messageEn,
            String messageFa,
            String message,
            Integer retryAfterSeconds) {

        /** A message to show to a user, preferring the requested language. */
        public String userMessage(boolean persian) {
            String preferred = persian ? messageFa : messageEn;
            if (preferred != null) {
                return preferred;
            }
            return firstNonNull(messageEn, messageFa, message, code);
        }
    }

    public record SimpleMessage(String message) {
    }

    public record CmdResponse(String result) {
    }

    public record ServerListResponse(
            List<ServerListItem> worlds,
            Integer remainingConversionsThisMonth,
            Integer convertMonthlyLimit) {
    }

    public record ServerListItem(
            String worldName,
            String game,
            Integer memory,
            Integer disk,
            Boolean isBedrock,
            Integer remainingTime,
            Boolean isTmod,
            Double renewalDiscount) {
    }

    public record ServerData(
            String worldName,
            Integer port,
            String host,
            String ip,
            String ftpHost,
            String filemanagerBaseUrl,
            Integer remainingTime,
            Integer isFull,
            Boolean isMigrating,
            Integer disk,
            Integer memory,
            String version,
            String java,
            String software,
            Boolean isBedrock,
            Integer additionalPort,
            List<String> listenPorts,
            String subdomain,
            Double renewalDiscount) {
    }
}
