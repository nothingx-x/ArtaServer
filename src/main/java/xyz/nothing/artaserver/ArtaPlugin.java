package xyz.nothing.artaserver;


import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.nothing.artaserver.listener.AuraListener;
import xyz.nothing.artaserver.listener.PlayerListener;

import java.util.List;

public class ArtaPlugin extends JavaPlugin {
    private WebhookService webhookService;
    private HealthReportService healthReportService;
    private static boolean debug;
    private static ArtaPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("health_report.yml", false);

        List<String> webhooks = getConfig().getStringList("config.webhooks");
        debug = getConfig().getBoolean("config.debug");

        webhookService = new WebhookService(webhooks);
        healthReportService = new HealthReportService();
        getServer().getPluginManager().registerEvents(new PlayerListener(webhookService), this);
        if (isAuraSkillEnabled()) {
            getServer().getPluginManager().registerEvents(new AuraListener(webhookService), this);
        }

        webhookService.notifyStartStop(false);
        healthReportService.init();
        getServer().getScheduler().scheduleSyncRepeatingTask(this, healthReportService::run, 1 * 60 * 20L, 1 * 60 * 20);
        getComponentLogger().info(Component.text("ArtaPlugin enabled!"));
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
}
