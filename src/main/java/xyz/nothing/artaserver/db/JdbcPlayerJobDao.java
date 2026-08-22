package xyz.nothing.artaserver.db;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.UUID;

public class JdbcPlayerJobDao {
    private final Dao<PlayerJobEntity, UUID> dao;

    public JdbcPlayerJobDao(ConnectionSource connectionSource) throws SQLException {
        dao = DaoManager.createDao(connectionSource, PlayerJobEntity.class);
    }

    /**
     * Get or create a player's job entity.
     */
    public PlayerJobEntity getOrCreate(UUID uuid) throws SQLException {
        PlayerJobEntity entity = dao.queryForId(uuid);
        if (entity == null) {
            entity = new PlayerJobEntity();
            entity.setUuid(uuid);
            entity.setJob(null);
            entity.setLevel(1);
            entity.setXp(0);
            dao.create(entity);
        }
        return entity;
    }

    /**
     * Save (insert or update) a player's job entity.
     */
    public void save(PlayerJobEntity entity) throws SQLException {
        dao.createOrUpdate(entity);
    }

    /**
     * Get a player's job entity (returns null if not found).
     */
    public PlayerJobEntity queryById(UUID uuid) throws SQLException {
        return dao.queryForId(uuid);
    }
}
