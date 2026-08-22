package xyz.nothing.artaserver.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Finds safe locations within a given radius.
 * A location is safe if the block at feet level is passable,
 * the block below is solid, and no unsafe blocks are nearby.
 */
public class SafeLocationFinder {

    private static final Random RANDOM = new Random();

    private static final Set<Material> UNSAFE_BLOCKS = new HashSet<>();
    static {
        // Liquids
        UNSAFE_BLOCKS.add(Material.LAVA);
        UNSAFE_BLOCKS.add(Material.WATER);

        // Fire / damage
        UNSAFE_BLOCKS.add(Material.FIRE);
        UNSAFE_BLOCKS.add(Material.SOUL_FIRE);
        UNSAFE_BLOCKS.add(Material.CACTUS);
        UNSAFE_BLOCKS.add(Material.MAGMA_BLOCK);
        UNSAFE_BLOCKS.add(Material.POINTED_DRIPSTONE);

        // Dangerous blocks
        UNSAFE_BLOCKS.add(Material.WITHER_ROSE);
        UNSAFE_BLOCKS.add(Material.SWEET_BERRY_BUSH);
        UNSAFE_BLOCKS.add(Material.COBWEB);
    }

    // Max attempts before giving up
    private static final int MAX_ATTEMPTS = 50;

    /**
     * Find a random safe location within the given radius of the origin.
     *
     * @param origin the center location
     * @param radius the search radius in blocks
     * @return a safe Location, or null if none found
     */
    public static Location findSafeLocation(Location origin, int radius) {
        World world = origin.getWorld();
        if (world == null) return null;

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            int x = origin.getBlockX() + RANDOM.nextInt(-radius, radius + 1);
            int z = origin.getBlockZ() + RANDOM.nextInt(-radius, radius + 1);
            int y = world.getHighestBlockYAt(x, z) + 1;

            Location candidate = new Location(world, x + 0.5, y, z + 0.5);
            if (isSafe(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Check if a location is safe for a player to stand at.
     *
     * @param location the location to check (feet level)
     * @return true if safe
     */
    public static boolean isSafe(Location location) {
        World world = location.getWorld();
        if (world == null) return false;

        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block below = feet.getRelative(0, -1, 0);

        // Feet and head must be passable (air, grass, etc.)
        if (!feet.isPassable() || !head.isPassable()) return false;

        // Block below must be solid
        if (below.getType().isAir() || below.isPassable()) return false;

        // Check surrounding blocks for hazards (3x3 area around feet)
        if (hasUnsafeBlocksNearby(feet)) return false;

        return true;
    }

    /**
     * Check if any unsafe blocks exist in a 3x3 area around the given block.
     */
    private static boolean hasUnsafeBlocksNearby(Block center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    Block block = center.getRelative(dx, dy, dz);
                    if (UNSAFE_BLOCKS.contains(block.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
