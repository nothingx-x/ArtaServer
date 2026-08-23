package xyz.nothing.artaserver.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.UUID;

@DatabaseTable(tableName = "daily_claims")
public class DailyClaimEntity {
    @DatabaseField(id = true, columnName = "uuid")
    private UUID uuid;

    @DatabaseField(columnName = "last_claim_time")
    private long lastClaimTime;

    public DailyClaimEntity() {
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public long getLastClaimTime() {
        return lastClaimTime;
    }

    public void setLastClaimTime(long lastClaimTime) {
        this.lastClaimTime = lastClaimTime;
    }
}
