package xyz.nothing.artaserver.db;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import xyz.nothing.artaserver.ArtaPlugin;

import java.io.File;
import java.sql.SQLException;

public class DatabaseManager {
    private static DatabaseManager instance;
    private ConnectionSource connectionSource;
    private JdbcPlayerJobDao playerJobDao;
    private JdbcDailyClaimDao dailyClaimDao;

    private DatabaseManager() {
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initialize the database connection and create tables.
     */
    public void init() throws SQLException {
        File dbFile = new File(ArtaPlugin.getInstance().getDataFolder(), "jobs.db");
        ArtaPlugin.getInstance().getDataFolder().mkdirs();

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connectionSource = new JdbcConnectionSource(url);

        TableUtils.createTableIfNotExists(connectionSource, PlayerJobEntity.class);
        TableUtils.createTableIfNotExists(connectionSource, DailyClaimEntity.class);
        playerJobDao = new JdbcPlayerJobDao(connectionSource);
        dailyClaimDao = new JdbcDailyClaimDao(connectionSource);

        ArtaPlugin.getInstance().getLogger().info("Database initialized: " + dbFile.getAbsolutePath());
    }

    public JdbcPlayerJobDao getPlayerJobDao() {
        return playerJobDao;
    }

    public JdbcDailyClaimDao getDailyClaimDao() {
        return dailyClaimDao;
    }

    /**
     * Close the database connection.
     */
    public void close() {
        if (connectionSource != null) {
            try {
                connectionSource.close();
            } catch (Exception e) {
                ArtaPlugin.getInstance().getLogger().severe("Could not close database: " + e.getMessage());
            }
        }
    }
}
