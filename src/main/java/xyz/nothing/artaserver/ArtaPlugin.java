package xyz.nothing.artaserver;


import net.kyori.adventure.text.Component;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.nothing.artaserver.command.JobCommand;
import xyz.nothing.artaserver.db.DatabaseManager;
import xyz.nothing.artaserver.listener.JobEffects;
import xyz.nothing.artaserver.job.JobListenerScanner;
import xyz.nothing.artaserver.job.JobManager;
import xyz.nothing.artaserver.listener.FarmerListener;
import xyz.nothing.artaserver.listener.SmithmanListener;
import xyz.nothing.artaserver.listener.WarriorListener;
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

        // commands
        JobCommand jobCommand = new JobCommand(jobManager);
        PluginCommand jobCmd = getCommand("job");
        if (jobCmd != null) {
            jobCmd.setExecutor(jobCommand);
        }

        // listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(webhookService), this);
        getServer().getPluginManager().registerEvents(new FarmerListener(jobManager), this);
        getServer().getPluginManager().registerEvents(new WarriorListener(jobManager), this);
        getServer().getPluginManager().registerEvents(new SmithmanListener(jobManager), this);

        // Scan @OnJob annotated methods and register as listeners
        JobListenerScanner.scan(this, new JobEffects(), jobManager);

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
