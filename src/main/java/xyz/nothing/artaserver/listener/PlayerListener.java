package xyz.nothing.artaserver.listener;


import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.nothing.artaserver.WebhookService;

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
}
