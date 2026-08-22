package xyz.nothing.artaserver.job;

import xyz.nothing.artaserver.ArtaPlugin;
import xyz.nothing.artaserver.db.DatabaseManager;
import xyz.nothing.artaserver.db.PlayerJobEntity;

import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JobManager {
    private static JobManager instance;
    private final Map<UUID, PlayerJobData> playerJobs = new HashMap<>();

    private JobManager() {
    }

    public static JobManager getInstance() {
        if (instance == null) {
            instance = new JobManager();
        }
        return instance;
    }

    /**
     * Try to assign a job to a player. Returns true if successful, false if already has a job.
     */
    public boolean assignJob(UUID playerId, JobType job) {
        if (playerJobs.containsKey(playerId)) {
            return false;
        }

        PlayerJobData data = new PlayerJobData(job, 1, 0);
        playerJobs.put(playerId, data);

        // Persist to database
        try {
            PlayerJobEntity entity = new PlayerJobEntity();
            entity.setUuid(playerId);
            entity.setJob(job);
            entity.setLevel(1);
            entity.setXp(0);
            DatabaseManager.getInstance().getPlayerJobDao().save(entity);
        } catch (SQLException e) {
            ArtaPlugin.getInstance().getLogger().severe("Could not save job for " + playerId + ": " + e.getMessage());
        }

        return true;
    }

    /**
     * Grant XP to a player, save progress, and send feedback messages.
     */
    public void grantXp(Player player, int xp) {
        PlayerJobData data = playerJobs.get(player.getUniqueId());
        if (data == null) return;

        int levelsGained = data.addXp(xp);
        saveProgress(player.getUniqueId());

        JobMessageUtil.sendXpMessage(player, xp, data);

        if (levelsGained > 0) {
            JobMessageUtil.sendLevelUpMessage(player, data);
        }
    }

    /**
     * Get a player's job data (from cache).
     */
    public PlayerJobData getPlayerJob(UUID playerId) {
        return playerJobs.get(playerId);
    }

    /**
     * Check if a player has a job.
     */
    public boolean hasJob(UUID playerId) {
        return playerJobs.containsKey(playerId);
    }

    /**
     * Load a player's data from database into cache.
     */
    public void loadPlayer(UUID playerId) {
        try {
            PlayerJobEntity entity = DatabaseManager.getInstance().getPlayerJobDao().queryById(playerId);
            if (entity != null && entity.getJob() != null) {
                playerJobs.put(playerId, new PlayerJobData(entity.getJob(), entity.getLevel(), entity.getXp()));
            }
        } catch (SQLException e) {
            ArtaPlugin.getInstance().getLogger().severe("Could not load job for " + playerId + ": " + e.getMessage());
        }
    }

    /**
     * Save a player's XP progress to database.
     */
    public void saveProgress(UUID playerId) {
        PlayerJobData data = playerJobs.get(playerId);
        if (data == null) return;

        try {
            PlayerJobEntity entity = new PlayerJobEntity();
            entity.setUuid(playerId);
            entity.setJob(data.getJob());
            entity.setLevel(data.getLevel());
            entity.setXp(data.getXp());
            DatabaseManager.getInstance().getPlayerJobDao().save(entity);
        } catch (SQLException e) {
            ArtaPlugin.getInstance().getLogger().severe("Could not save progress for " + playerId + ": " + e.getMessage());
        }
    }

    /**
     * Remove a player from cache (on quit).
     */
    public void unloadPlayer(UUID playerId) {
        playerJobs.remove(playerId);
    }
}
