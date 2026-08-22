package xyz.nothing.artaserver.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Random;

/**
 * Slows animal breeding by randomly cancelling breed events.
 * Configurable via config.yml: breeding.cancel-chance (0.0 - 1.0, default 0.7)
 */
public class BreedingListener implements Listener {
    private final double cancelChance;
    private final Random random = new Random();

    public BreedingListener(double cancelChance) {
        this.cancelChance = Math.clamp(cancelChance, 0.0, 1.0);
    }

    @EventHandler
    public void onEntityBreed(EntityBreedEvent event) {
        if (random.nextDouble() < cancelChance) {
            event.setCancelled(true);

            LivingEntity breeder = event.getBreeder();
            if (breeder instanceof Player player) {
                player.sendActionBar(Component.text("Breeding failed! Try again later.", NamedTextColor.YELLOW));
            }
        }
    }
}
