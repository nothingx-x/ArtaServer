package xyz.nothing.artaserver;


import dev.aurelium.auraskills.api.event.skill.SkillLevelUpEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

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
            request = new GeneralRequest(playerName, Action.JOIN);
        } else if (clazz == PlayerQuitEvent.class) {
            request = new GeneralRequest(playerName, Action.QUIT);
        } else if (clazz == AsyncChatEvent.class) {
            request = new ChatRequest(playerName, Action.CHAT, PlainTextComponentSerializer.plainText().serialize(((AsyncChatEvent) event).message()));
        } else {
            return;
        }

        if (ArtaPlugin.isDebug()) {
            ArtaPlugin.getInstance().getLogger().info("notifyPlayerEvent player: " + request.playerName() + ", action: " + request.action());
        }

        sendRequestAsync(request);
    }

    public void notifyAuraSkills(Event event) {
        if (!ArtaPlugin.getInstance().isAuraSkillEnabled()) {
            return;
        }

        Request request;
        if (event instanceof SkillLevelUpEvent e) {
            request = new SkillLevelUpRequest(e.getPlayer().getName(), Action.AURA_LEVEL_UP, e.getLevel(), e.getSkill().getDisplayName(Locale.ENGLISH));
        } else {
            return;
        }

        sendRequestAsync(request);
    }

    public void notifyStartStop(boolean isStopped) {
        if (isStopped) {
            // if stop is true, it means that plugin is disabled. in this case request will be sent synchronously
            sendRequest(new GeneralRequest("", Action.STOP));
        } else {
            sendRequestAsync(new GeneralRequest("", Action.START));
        }
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

    private void sendRequest(Request requestData) {
        for (String webhook : webhooks) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhook))
                    .POST(HttpRequest.BodyPublishers.ofString(requestData.toJSONString()))
                    .setHeader("Content-type", "application/json")
                    .build();
            try {
                client.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (IOException | InterruptedException e) {
                ArtaPlugin.getInstance().getLogger().severe("Failed to send webhook: " + e.getMessage());
            }
        }
    }

    public enum Action {
        JOIN,
        QUIT,
        START,
        STOP,
        CHAT,
        AURA_LEVEL_UP
    }

    public interface Request {
        String playerName();

        Action action();

        String toJSONString();
    }

    public record GeneralRequest(String playerName, Action action) implements Request {

        @Override
        public String toJSONString() {
            return String.format("{\"playerName\":\"%s\", \"action\":\"%s\"}", playerName, action);
        }
    }

    public record ChatRequest(String playerName, Action action, String message) implements Request {

        @Override
        public String toJSONString() {
            return String.format("{\"playerName\":\"%s\", \"action\":\"%s\", \"message\":\"%s\"}", playerName, action, message);
        }
    }

    public record SkillLevelUpRequest(String playerName, Action action, int level, String displayName) implements Request {

        @Override
        public String toJSONString() {
            return String.format("{\"playerName\":\"%s\", \"action\":\"%s\", \"displayName\":\"%s\", \"level\":%d}", playerName, action, displayName, level);
        }
    }
}
