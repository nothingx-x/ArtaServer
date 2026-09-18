package xyz.nothing.artaserver.listener;


import dev.aurelium.auraskills.api.event.skill.SkillLevelUpEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.nothing.artaserver.WebhookService;
import xyz.nothing.artaserver.event.HealthReportEvent;

public class PlayerListener implements Listener {
    private final WebhookService webhookService;

    public PlayerListener(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        webhookService.notifyPlayerEvent(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        webhookService.notifyPlayerEvent(event);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        webhookService.notifyPlayerEvent(event);
    }

    @EventHandler
    public void onHealthReport(HealthReportEvent event) {
        webhookService.notifyEvent(event);
    }
}
