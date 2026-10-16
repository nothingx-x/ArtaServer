package xyz.nothing.artaserver;


import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;
import xyz.nothing.artaserver.cmd.ArtaBoxCommand;
import xyz.nothing.artaserver.listener.PlayerListener;
import xyz.nothing.artaserver.service.ArtaBoxService;
import xyz.nothing.artaserver.service.HealthReportService;
import xyz.nothing.artaserver.service.WebhookService;

import java.sql.SQLException;
import java.util.List;

public class ArtaPlugin extends JavaPlugin {
    private WebhookService webhookService;
    private HealthReportService healthReportService;
    private ArtaBoxService artaBoxService;
    private static boolean debug;
    private static ArtaPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("health_report.yml", false);
        saveResource("boxes.yml", false);

        List<String> webhooks = getConfig().getStringList("config.webhooks");
        debug = getConfig().getBoolean("config.debug");
        webhookService = new WebhookService(webhooks);
        getServer().getPluginManager().registerEvents(new PlayerListener(webhookService), this);

        webhookService.notifyStartStop(false);
        healthReportService.init();
        getServer().getScheduler().scheduleSyncRepeatingTask(this, healthReportService::run, 1 * 60 * 20L, 1 * 60 * 20);
        getComponentLogger().info(Component.text("ArtaPlugin enabled!"));

        artaBoxService = new ArtaBoxService();
        registerCommand("artabox", new ArtaBoxCommand(artaBoxService));
    }

    @Override
    public void onDisable() {
        webhookService.notifyStartStop(true);
        healthReportService.stop();
        getComponentLogger().info(Component.text("ArtaPlugin disabled!"));
    }

    public boolean isAuraSkillEnabled() {
        Plugin plugin = getServer().getPluginManager().getPlugin("AuraSkills");
        return plugin != null && plugin.isEnabled();
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
