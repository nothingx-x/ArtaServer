package xyz.nothing.artaserver;


import net.kyori.adventure.text.Component;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.nothing.artaserver.command.DailyCommand;
import xyz.nothing.artaserver.command.JobCommand;
import xyz.nothing.artaserver.db.DatabaseManager;
import xyz.nothing.artaserver.listener.farmer.FarmerEffects;
import xyz.nothing.artaserver.listener.warrior.WarriorEffects;
import xyz.nothing.artaserver.listener.smithman.SmithmanEffects;
import xyz.nothing.artaserver.job.JobListenerScanner;
import xyz.nothing.artaserver.job.JobManager;
import xyz.nothing.artaserver.listener.farmer.FarmerListener;
import xyz.nothing.artaserver.listener.BreedingListener;
import xyz.nothing.artaserver.listener.NightDamageListener;
import xyz.nothing.artaserver.listener.NightSpawnListener;
import xyz.nothing.artaserver.listener.JobLockListener;
import xyz.nothing.artaserver.listener.VillagerTradeListener;
import xyz.nothing.artaserver.listener.smithman.SmithmanListener;
import xyz.nothing.artaserver.listener.warrior.WarriorListener;
import xyz.nothing.artaserver.listener.PlayerListener;

import java.sql.SQLException;
import java.util.List;

public class ArtaPlugin extends JavaPlugin {
    private WebhookService webhookService;
    private static boolean debug;
    private static ArtaPlugin instance;
    private JobManager jobManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        List<String> webhooks = getConfig().getStringList("config.webhooks");
        debug = getConfig().getBoolean("config.debug");

        try {
            DatabaseManager.getInstance().init();
        } catch (SQLException e) {
            getLogger().severe("Could not initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        webhookService = new WebhookService(webhooks);
        jobManager = JobManager.getInstance();
        DailyManager dailyManager = new DailyManager(jobManager);

        // commands
        JobCommand jobCommand = new JobCommand(jobManager);
        DailyCommand dailyCommand = new DailyCommand(dailyManager);
        PluginCommand jobCmd = getCommand("job");
        PluginCommand dailyCmd = getCommand("daily");

        if (jobCmd != null) {
            jobCmd.setExecutor(jobCommand);
        }

        if (dailyCmd != null) {
            dailyCmd.setExecutor(dailyCommand);
        }

        // listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(webhookService), this);
        getServer().getPluginManager().registerEvents(new FarmerListener(jobManager), this);
        getServer().getPluginManager().registerEvents(new WarriorListener(jobManager), this);
        getServer().getPluginManager().registerEvents(new SmithmanListener(jobManager), this);
        getServer().getPluginManager().registerEvents(new NightSpawnListener(), this);
        getServer().getPluginManager().registerEvents(new NightDamageListener(), this);
        getServer().getPluginManager().registerEvents(new VillagerTradeListener(), this);
        getServer().getPluginManager().registerEvents(new JobLockListener(), this);

        double breedingCancelChance = getConfig().getDouble("config.breeding-cancel-chance", 0.2);
        getServer().getPluginManager().registerEvents(new BreedingListener(breedingCancelChance), this);

        // Scan @OnJob annotated methods and register as listeners
        JobListenerScanner.scan(this, new FarmerEffects(), jobManager);
        JobListenerScanner.scan(this, new WarriorEffects(), jobManager);
        JobListenerScanner.scan(this, new SmithmanEffects(), jobManager);

        webhookService.notifyStartStop(false);
        getComponentLogger().info(Component.text("ArtaPlugin enabled!"));
    }

    @Override
    public void onDisable() {
        DatabaseManager.getInstance().close();
        webhookService.notifyStartStop(true);
        getComponentLogger().info(Component.text("ArtaPlugin disabled!"));
    }

    public static boolean isDebug() {
        return debug;
    }

    public static ArtaPlugin getInstance() {
        return instance;
    }
}
