package xyz.nothing.artaserver.util;

import org.bukkit.World;

/**
 * Utility class for converting Minecraft time ticks into readable values.
 */
public final class MinecraftTimeUtil {

    private MinecraftTimeUtil() {
    }

    /**
     * Get the TimeOfDay enum for a world's current time.
     */
    public static TimeOfDay getTimeOfDay(World world) {
        return TimeOfDay.fromTicks(world.getTime());
    }

    /**
     * Get the TimeOfDay enum for a given tick value.
     */
    public static TimeOfDay getTimeOfDay(long ticks) {
        return TimeOfDay.fromTicks(ticks);
    }

    /**
     * Get a clock string (e.g. "6:00 AM") for a world's current time.
     */
    public static String toClock(World world) {
        return TimeOfDay.toClock(world.getTime());
    }

    /**
     * Get a clock string for a given tick value.
     */
    public static String toClock(long ticks) {
        return TimeOfDay.toClock(ticks);
    }

    /**
     * Check if it is currently night in the given world.
     */
    public static boolean isNight(World world) {
        return getTimeOfDay(world).isNight();
    }
}
