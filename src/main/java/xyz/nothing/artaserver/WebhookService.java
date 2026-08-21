package xyz.nothing.artaserver;


import ca.spottedleaf.concurrentutil.completable.Completable;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class WebhookService {
    private final List<String> webhooks;
    private final HttpClient client;
    public WebhookService(List<String> webhooks) {
        this.webhooks = webhooks;
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();
    }

    public void notifyPlayerEvent(PlayerEvent event) {
        String playerName = event.getPlayer().getName();
        Class<? extends PlayerEvent> clazz = event.getClass();
        Request request;
        if (clazz == PlayerJoinEvent.class) {
            request = new Request(playerName, Action.JOIN);
        } else if (clazz == PlayerQuitEvent.class) {
            request = new Request(playerName, Action.QUIT);
        } else {
            return;
        }

        if (ArtaPlugin.isDebug()) {
            ArtaPlugin.getInstance().getLogger().info("notifyPlayerEvent player: " + request.playerName + ", action: " + request.action);
        }

        sendRequestAsync(request);
    }

    private void sendRequestAsync(Request requestData) {
        List<CompletableFuture<HttpResponse<Void>>> futures = new ArrayList<>();
        for (String webhook : webhooks) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhook))
                    .POST(HttpRequest.BodyPublishers.ofString(requestData.toJSONString()))
                    .setHeader("Content-type", "application/json")
                    .build();
            futures.add(client.sendAsync(request, HttpResponse.BodyHandlers.discarding()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{}));
    }

    public enum Action {
        JOIN,
        QUIT
    }
    public record Request(String playerName, Action action) {
        public String toJSONString() {
          return String.format("{\"playerName\":\"%s\", \"action\":\"%s\"}", playerName, action);
        };
    }
}
