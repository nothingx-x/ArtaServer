package xyz.nothing.artaserver.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import xyz.nothing.artaserver.job.JobManager;

/**
 * Locks all actions for players who haven't picked a job yet.
 */
public class JobLockListener implements Listener {
    private static final Component LOCKED_MESSAGE = Component.text("You must pick a job first! Use /job pick <job>", NamedTextColor.RED);
    private static final Component JOIN_MESSAGE = Component.text("Select your job using /job pick", NamedTextColor.RED);

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (hasJob(player)) return;
        player.sendMessage(JOIN_MESSAGE);
        player.sendActionBar(JOIN_MESSAGE);
    }
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (hasJob(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (hasJob(event.getPlayer())) return;
        // Allow head rotation but block position change
        if (event.getFrom().equals(event.getTo())) return;
        event.setTo(event.getFrom());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (hasJob(event.getPlayer())) return;
        event.setCancelled(true);
        event.getPlayer().sendActionBar(LOCKED_MESSAGE);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (hasJob(event.getPlayer())) return;
        event.setCancelled(true);
        event.getPlayer().sendActionBar(LOCKED_MESSAGE);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (hasJob(event.getPlayer())) return;
        event.setCancelled(true);
        event.getPlayer().sendActionBar(LOCKED_MESSAGE);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (hasJob(player)) return;
        event.setCancelled(true);
        player.sendActionBar(LOCKED_MESSAGE);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (hasJob(player)) return;
        event.setCancelled(true);
        player.sendActionBar(LOCKED_MESSAGE);
    }

    private boolean hasJob(Player player) {
        return JobManager.getInstance().hasJob(player.getUniqueId());
    }
}
