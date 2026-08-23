package xyz.nothing.artaserver.db;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.UUID;

public class JdbcDailyClaimDao {
    private final Dao<DailyClaimEntity, UUID> dao;

    public JdbcDailyClaimDao(ConnectionSource connectionSource) throws SQLException {
        dao = DaoManager.createDao(connectionSource, DailyClaimEntity.class);
    }

    /**
     * Get or create a daily claim entity for a player.
     */
    public DailyClaimEntity getOrCreate(UUID uuid) throws SQLException {
        DailyClaimEntity entity = dao.queryForId(uuid);
        if (entity == null) {
            entity = new DailyClaimEntity();
            entity.setUuid(uuid);
            entity.setLastClaimTime(0);
            dao.create(entity);
        }
        return entity;
    }

    /**
     * Save (insert or update) a daily claim entity.
     */
    public void save(DailyClaimEntity entity) throws SQLException {
        dao.createOrUpdate(entity);
    }

    /**
     * Get a player's daily claim entity (returns null if not found).
     */
    public DailyClaimEntity queryById(UUID uuid) throws SQLException {
        return dao.queryForId(uuid);
    }
}
