package xyz.nothing.artaserver.listener;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import xyz.nothing.artaserver.util.SafeLocationFinder;

import java.util.Random;

public class NightSpawnListener implements Listener {
    private final Random random = new Random();

    // Chance to spawn an extra monster
    private static final double EXTRA_SPAWN_CHANCE = 0.7;

    // Max extra monsters per spawn event
    private static final int MAX_EXTRA_SPAWNS = 4;

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Only trigger for natural spawns
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;

        // Only trigger for hostile mobs
        if (!(event.getEntity() instanceof Monster)) return;

        World world = event.getLocation().getWorld();
        if (world == null) return;

        // Only at night (time between 13000 and 23000)
        long time = world.getTime();
        if (time < 13000 || time > 23000) return;

        // Spawn extra monsters
        int extraSpawns = 0;
        for (int i = 0; i < MAX_EXTRA_SPAWNS; i++) {
            if (random.nextDouble() < EXTRA_SPAWN_CHANCE) {
                Location loc = SafeLocationFinder.findSafeLocation(event.getLocation(), 15);
                if (loc != null) {
                    world.spawnEntity(loc, event.getEntityType(), CreatureSpawnEvent.SpawnReason.NATURAL);
                    extraSpawns++;
                }
            }
        }

        if (extraSpawns > 0) {
            event.getEntity().getServer().getLogger().fine(
                    "Night spawn: " + extraSpawns + " extra " + event.getEntityType().name() + " at " + event.getLocation().getBlockX() + "," + event.getLocation().getBlockZ());
        }
    }

    /**
     * Find a valid spawn location near the original spawn point.
     */
    private Location findSpawnLocation(Location origin) {
        World world = origin.getWorld();
        if (world == null) return null;

        for (int attempt = 0; attempt < 10; attempt++) {
            double x = origin.getX() + random.nextGaussian() * 5;
            double z = origin.getZ() + random.nextGaussian() * 5;
            int y = world.getHighestBlockYAt((int) x, (int) z) + 1;

            Location loc = new Location(world, x, y, z);
            if (loc.getBlock().isPassable()) {
                return loc;
            }
        }
        return null;
    }
}
