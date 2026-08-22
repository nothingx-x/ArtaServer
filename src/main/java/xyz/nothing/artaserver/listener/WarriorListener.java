package xyz.nothing.artaserver.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import xyz.nothing.artaserver.job.JobManager;
import xyz.nothing.artaserver.job.JobType;
import xyz.nothing.artaserver.job.PlayerJobData;

import java.util.HashMap;
import java.util.Map;

public class WarriorListener implements Listener {
    private final JobManager jobManager;

    // XP per mob type
    private static final Map<String, Integer> MOB_XP;
    static {
        MOB_XP = new HashMap<>();
        MOB_XP.put("ZOMBIE", 5);
        MOB_XP.put("SKELETON", 5);
        MOB_XP.put("SPIDER", 4);
        MOB_XP.put("CREEPER", 6);
        MOB_XP.put("ENDERMAN", 10);
        MOB_XP.put("BLAZE", 12);
        MOB_XP.put("WITHER_SKELETON", 15);
        MOB_XP.put("GHAST", 15);
        MOB_XP.put("PIGLIN", 8);
        MOB_XP.put("HOGLIN", 10);
        MOB_XP.put("WARDEN", 100);
        MOB_XP.put("WITHER", 50);
        MOB_XP.put("ENDER_DRAGON", 200);
    }

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

        int levelsGained = data.addXp(xp);
        jobManager.saveProgress(killer.getUniqueId());

        sendXpMessage(killer, xp, data);

        if (levelsGained > 0) {
            sendLevelUpMessage(killer, data);
        }
    }

    private void sendXpMessage(Player player, int xp, PlayerJobData data) {
        player.sendActionBar(Component.text("+" + xp + " Job XP [" + data.getXp() + "/" + data.getXpToNextLevel() + "]",
                NamedTextColor.RED));
    }

    private void sendLevelUpMessage(Player player, PlayerJobData data) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("★ JOB LEVEL UP! ★", NamedTextColor.GOLD));
        player.sendMessage(Component.text("You are now Warrior level ", NamedTextColor.RED)
                .append(Component.text(data.getLevel(), NamedTextColor.WHITE))
                .append(Component.text("!", NamedTextColor.RED)));
        player.sendMessage(Component.empty());
    }
}
