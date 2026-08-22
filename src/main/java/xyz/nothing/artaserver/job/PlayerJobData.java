package xyz.nothing.artaserver.job;

public class PlayerJobData {
    private final JobType job;
    private int level;
    private int xp;

    public PlayerJobData(JobType job, int level, int xp) {
        this.job = job;
        this.level = level;
        this.xp = xp;
    }

    public JobType getJob() {
        return job;
    }

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    public int getXpToNextLevel() {
        // Simple formula: each level needs more XP
        return 100 + (level * 50);
    }

    /**
     * Add XP and handle level ups. Returns number of levels gained.
     */
    public int addXp(int amount) {
        this.xp += amount;
        int levelsGained = 0;
        while (this.xp >= getXpToNextLevel()) {
            this.xp -= getXpToNextLevel();
            this.level++;
            levelsGained++;
        }
        return levelsGained;
    }

    public boolean isMaxLevel() {
        return level >= 100;
    }
}
