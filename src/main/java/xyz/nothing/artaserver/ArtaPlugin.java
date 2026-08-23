package xyz.nothing.artaserver;


import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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
        registerCommand("job", "Choose your job", "/job pick <job>", new JobCommand(jobManager));
        registerCommand("daily", "Claim your daily reward", "/daily", new DailyCommand(dailyManager));

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

    private void registerCommand(String name, String description, String usage, Object executor) {
        Command command = new Command(name, description, usage, List.of()) {
            @Override
            public boolean execute(@NonNull CommandSender sender, @NonNull String commandLabel, @NonNull String[] args) {
                return ((CommandExecutor) executor).onCommand(sender, this, commandLabel, args);
            }

            @Override
            public java.util.@Nullable List<String> tabComplete(@NonNull CommandSender sender, @NonNull String alias, @NonNull String[] args) {
                if (executor instanceof TabCompleter tabCompleter) {
                    return tabCompleter.onTabComplete(sender, this, alias, args);
                }
                return super.tabComplete(sender, alias, args);
            }
        };
        getServer().getCommandMap().register("artaserver", command);
    }
}
