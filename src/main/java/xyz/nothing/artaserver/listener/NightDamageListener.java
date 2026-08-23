package xyz.nothing.artaserver.listener;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import xyz.nothing.artaserver.util.TimeOfDay;

/**
 * Monsters deal increased damage to players at night.
 */
public class NightDamageListener implements Listener {

    // Damage multiplier during night
    private static final double NIGHT_DAMAGE_MULTIPLIER = 1.5;

    // Extra multiplier during deep night (midnight ~ 3am)
    private static final double DEEP_NIGHT_DAMAGE_MULTIPLIER = 2.0;

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Only affect player victims
        if (!(event.getEntity() instanceof Player player)) return;

        // Only affect monster attackers
        if (!(event.getDamager() instanceof Monster)) return;

        LivingEntity damager = (LivingEntity) event.getDamager();

        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) return;

        TimeOfDay time = TimeOfDay.fromTicks(world.getTime());

        // Calculate multiplier based on time of night
        double multiplier;
        if (time == TimeOfDay.MIDNIGHT) {
            multiplier = DEEP_NIGHT_DAMAGE_MULTIPLIER;
        } else if (time.isNight()) {
            multiplier = NIGHT_DAMAGE_MULTIPLIER;
        } else {
            return; // Not night, no boost
        }

        double originalDamage = event.getDamage();
        event.setDamage(originalDamage * multiplier);
    }
}
