package xyz.nothing.artaserver.listener.farmer;

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

        jobManager.grantXp(player, BASE_XP);
    }
}
