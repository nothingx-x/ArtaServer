package xyz.nothing.artaserver.listener.warrior;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import xyz.nothing.artaserver.job.JobManager;
import xyz.nothing.artaserver.job.JobType;
import xyz.nothing.artaserver.job.PlayerJobData;

import java.util.Map;

import static java.util.Map.entry;

public class WarriorListener implements Listener {
    private final JobManager jobManager;

    // XP per mob type
    private static final Map<String, Integer> MOB_XP = Map.ofEntries(
            entry("ZOMBIE", 5),
            entry("SKELETON", 5),
            entry("SPIDER", 4),
            entry("CREEPER", 6),
            entry("ENDERMAN", 10),
            entry("BLAZE", 12),
            entry("WITHER_SKELETON", 15),
            entry("GHAST", 15),
            entry("PIGLIN", 8),
            entry("HOGLIN", 10),
            entry("WARDEN", 100),
            entry("WITHER", 50),
            entry("ENDER_DRAGON", 200)
    );

    private static final int DEFAULT_XP = 3;

    public WarriorListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        PlayerJobData data = jobManager.getPlayerJob(killer.getUniqueId());
        if (data == null || data.getJob() != JobType.WARRIOR) return;

        String mobType = event.getEntityType().name();
        int xp = MOB_XP.getOrDefault(mobType, DEFAULT_XP);

        jobManager.grantXp(killer, xp);
    }
}
