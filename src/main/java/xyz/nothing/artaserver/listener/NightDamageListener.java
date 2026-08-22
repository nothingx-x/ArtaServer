package xyz.nothing.artaserver.listener;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Monsters deal increased damage to players at night.
 */
public class NightDamageListener implements Listener {

    // Night time range (Minecraft ticks): 13000 = sunset, 23000 = pre-dawn
    private static final long NIGHT_START = 13000;
    private static final long NIGHT_END = 23000;

    // Damage multiplier during night
    private static final double NIGHT_DAMAGE_MULTIPLIER = 1.5;

    // Extra multiplier during deep night (midnight ~ 3am, ticks 18000-21000)
    private static final long DEEP_NIGHT_START = 18000;
    private static final long DEEP_NIGHT_END = 21000;
    private static final double DEEP_NIGHT_DAMAGE_MULTIPLIER = 2.0;

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Only affect player victims
        if (!(event.getEntity() instanceof Player player)) return;

        // Only affect monster attackers
        if (!(event.getDamager() instanceof Monster)) return;

        LivingEntity damager = (LivingEntity) event.getDamager();

        World world = player.getWorld();
        if (world == null) return;
        if (world.getEnvironment() != World.Environment.NORMAL) return;

        long time = world.getTime();

        // Calculate multiplier based on time of night
        double multiplier;
        if (time >= DEEP_NIGHT_START && time <= DEEP_NIGHT_END) {
            multiplier = DEEP_NIGHT_DAMAGE_MULTIPLIER;
        } else if (time >= NIGHT_START && time <= NIGHT_END) {
            multiplier = NIGHT_DAMAGE_MULTIPLIER;
        } else {
            return; // Not night, no boost
        }

        double originalDamage = event.getDamage();
        event.setDamage(originalDamage * multiplier);
    }
}
