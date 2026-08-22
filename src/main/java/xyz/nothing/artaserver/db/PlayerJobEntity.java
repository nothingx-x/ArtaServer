package xyz.nothing.artaserver.db;


import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import xyz.nothing.artaserver.job.JobType;

import java.util.UUID;

@DatabaseTable(tableName = "players_job")
public class PlayerJobEntity {
    @DatabaseField(id = true, columnName = "uuid")
    private UUID uuid;

    @DatabaseField(columnName = "job")
    private JobType job;

    @DatabaseField(columnName = "level")
    private int level;

    @DatabaseField(columnName = "xp")
    private int xp;

    public PlayerJobEntity() {
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public JobType getJob() {
        return job;
    }

    public void setJob(JobType job) {
        this.job = job;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }
}
