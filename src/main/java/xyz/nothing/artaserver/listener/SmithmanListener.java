package xyz.nothing.artaserver.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import xyz.nothing.artaserver.job.JobManager;
import xyz.nothing.artaserver.job.JobType;
import xyz.nothing.artaserver.job.PlayerJobData;

public class SmithmanListener implements Listener {
    private final JobManager jobManager;

    private static final int XP_PER_ITEM = 2;

    public SmithmanListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        PlayerJobData data = jobManager.getPlayerJob(player.getUniqueId());

        if (data == null || data.getJob() != JobType.SMITHMAN) return;

        int itemsSmelted = event.getItemAmount();
        int xp = itemsSmelted * XP_PER_ITEM;

        int levelsGained = data.addXp(xp);
        jobManager.saveProgress(player.getUniqueId());

        sendXpMessage(player, xp, data);

        if (levelsGained > 0) {
            sendLevelUpMessage(player, data);
        }
    }

    private void sendXpMessage(Player player, int xp, PlayerJobData data) {
        player.sendActionBar(Component.text("+" + xp + " Job XP [" + data.getXp() + "/" + data.getXpToNextLevel() + "]",
                NamedTextColor.AQUA));
    }

    private void sendLevelUpMessage(Player player, PlayerJobData data) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("★ JOB LEVEL UP! ★", NamedTextColor.GOLD));
        player.sendMessage(Component.text("You are now Smithman level ", NamedTextColor.AQUA)
                .append(Component.text(data.getLevel(), NamedTextColor.WHITE))
                .append(Component.text("!", NamedTextColor.AQUA)));
        player.sendMessage(Component.empty());
    }
}
