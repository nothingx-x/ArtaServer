package xyz.nothing.artaserver.listener;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.nothing.artaserver.ArtaPlugin;
import xyz.nothing.artaserver.WebhookService;
import xyz.nothing.artaserver.job.JobManager;
import xyz.nothing.artaserver.util.SafeLocationFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerListener implements Listener {
    private final WebhookService webhookService;

    // Radius to search around the target player for a safe spawn spot
    private static final int SPAWN_SEARCH_RADIUS = 5;

    public PlayerListener(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        webhookService.notifyPlayerEvent(event);
        JobManager.getInstance().loadPlayer(event.getPlayer().getUniqueId());

        teleportNewPlayers(event);
    }

    private static void teleportNewPlayers(PlayerJoinEvent event) {
        // Teleport new player to a safe location near a random online player (first join only)
        Player newPlayer = event.getPlayer();
        if (newPlayer.hasPlayedBefore()) return;
        ArtaPlugin plugin = ArtaPlugin.getInstance();

        // Delay 1 tick to ensure the player is fully loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            UUID newUuid = newPlayer.getUniqueId();

            List<Location> candidates = new ArrayList<>();

            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (!p.getUniqueId().equals(newUuid)) {
                    candidates.add(p.getLocation());
                }
            }

            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getUniqueId().equals(newUuid)) continue;
                if (op.isOnline()) continue; // already added above
                Location lastLoc = op.getLocation();
                if (lastLoc != null && lastLoc.getWorld() != null) {
                    candidates.add(lastLoc);
                }
            }

            if (candidates.isEmpty()) return;

            // Pick a random location and find a safe spot near it
            Location target = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

            Location safeLoc = SafeLocationFinder.findSafeLocation(target, SPAWN_SEARCH_RADIUS);
            if (safeLoc == null) {
                // Fallback: try a larger radius
                safeLoc = SafeLocationFinder.findSafeLocation(target, 20);
            }

            if (safeLoc != null) {
                newPlayer.setRespawnLocation(safeLoc);
                newPlayer.teleport(safeLoc);
            }
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        webhookService.notifyPlayerEvent(event);
        JobManager.getInstance().unloadPlayer(event.getPlayer().getUniqueId());
    }
}
