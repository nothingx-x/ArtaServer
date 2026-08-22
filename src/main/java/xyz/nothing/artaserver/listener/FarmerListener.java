package xyz.nothing.artaserver.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import xyz.nothing.artaserver.job.JobConstants;
import xyz.nothing.artaserver.job.JobManager;
import xyz.nothing.artaserver.job.JobType;
import xyz.nothing.artaserver.job.PlayerJobData;

public class FarmerListener implements Listener {
    private final JobManager jobManager;

    private static final int BASE_XP = 3;

    public FarmerListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerJobData data = jobManager.getPlayerJob(player.getUniqueId());

        if (data == null || data.getJob() != JobType.FARMER) return;

        Block block = event.getBlock();
        if (!JobConstants.CROPS.contains(block.getType())) return;

        int levelsGained = data.addXp(BASE_XP);
        jobManager.saveProgress(player.getUniqueId());

        sendXpMessage(player, BASE_XP, data);

        if (levelsGained > 0) {
            sendLevelUpMessage(player, data);
        }
    }

    private void sendXpMessage(Player player, int xp, PlayerJobData data) {
        player.sendActionBar(Component.text("+" + xp + " Job XP [" + data.getXp() + "/" + data.getXpToNextLevel() + "]",
                NamedTextColor.GREEN));
    }

    private void sendLevelUpMessage(Player player, PlayerJobData data) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("★ JOB LEVEL UP! ★", NamedTextColor.GOLD));
        player.sendMessage(Component.text("You are now Farmer level ", NamedTextColor.GREEN)
                .append(Component.text(data.getLevel(), NamedTextColor.WHITE))
                .append(Component.text("!", NamedTextColor.GREEN)));
        player.sendMessage(Component.empty());
    }
}
