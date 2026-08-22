package xyz.nothing.artaserver.job;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Common messaging utilities for all jobs.
 */
public final class JobMessageUtil {

    private JobMessageUtil() {
    }

    /**
     * Send an XP gain action bar message to the player.
     */
    public static void sendXpMessage(Player player, int xp, PlayerJobData data) {
        player.sendActionBar(Component.text("+" + xp + " Job XP [" + data.getXp() + "/" + data.getXpToNextLevel() + "]",
                data.getJob().getColor()));
    }

    /**
     * Send a level up chat message to the player.
     */
    public static void sendLevelUpMessage(Player player, PlayerJobData data) {
        NamedTextColor color = data.getJob().getColor();
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("★ JOB LEVEL UP! ★", NamedTextColor.GOLD));
        player.sendMessage(Component.text("You are now " + data.getJob().getDisplayName() + " level ", color)
                .append(Component.text(data.getLevel(), NamedTextColor.WHITE))
                .append(Component.text("!", color)));
        player.sendMessage(Component.empty());
    }
}
