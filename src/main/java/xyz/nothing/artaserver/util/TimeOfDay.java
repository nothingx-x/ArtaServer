package xyz.nothing.artaserver.util;

/**
 * Represents a period of the Minecraft day using readable names.
 * Minecraft time is in ticks (0–23999), where 1 tick = 1 minute of game time.
 */
public enum TimeOfDay {
    DAWN(0, 1000),
    MORNING(1000, 6000),
    NOON(6000, 6001),
    AFTERNOON(6001, 12000),
    DUSK(12000, 13000),
    NIGHT(13000, 18000),
    MIDNIGHT(18000, 18001),
    LATE_NIGHT(18001, 23000),
    PREDAWN(23000, 24000);

    private final long startTick;
    private final long endTick;

    TimeOfDay(long startTick, long endTick) {
        this.startTick = startTick;
        this.endTick = endTick;
    }

    public long getStartTick() {
        return startTick;
    }

    public long getEndTick() {
        return endTick;
    }

    public boolean isNight() {
        return this == NIGHT || this == MIDNIGHT || this == LATE_NIGHT || this == DUSK || this == PREDAWN;
    }

    public boolean isDay() {
        return !isNight();
    }

    /**
     * Convert a Minecraft time (ticks 0–23999) to a readable TimeOfDay enum.
     */
    public static TimeOfDay fromTicks(long ticks) {
        long t = ticks % 24000;
        if (t < 1000) return DAWN;
        if (t < 6000) return MORNING;
        if (t < 6001) return NOON;
        if (t < 12000) return AFTERNOON;
        if (t < 13000) return DUSK;
        if (t < 18000) return NIGHT;
        if (t < 18001) return MIDNIGHT;
        if (t < 23000) return LATE_NIGHT;
        return PREDAWN;
    }

    /**
     * Convert ticks to a 12-hour clock string (e.g. "6:00 AM").
     */
    public static String toClock(long ticks) {
        long t = ticks % 24000;
        int hours = (int) ((t / 1000 + 6) % 24); // Minecraft 0 = 6 AM
        int minutes = (int) ((t % 1000) * 60 / 1000);
        String suffix = hours >= 12 ? "PM" : "AM";
        int display = hours % 12;
        if (display == 0) display = 12;
        return display + ":" + String.format("%02d", minutes) + " " + suffix;
    }
}
