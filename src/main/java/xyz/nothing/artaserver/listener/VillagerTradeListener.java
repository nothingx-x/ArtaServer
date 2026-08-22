package xyz.nothing.artaserver.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Disables villager trading for all players.
 */
public class VillagerTradeListener implements Listener {

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.MERCHANT) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("Villager trading is disabled!", NamedTextColor.RED));
        }
    }
}
