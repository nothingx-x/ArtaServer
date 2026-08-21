package xyz.nothing.artaserver;


import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.nothing.artaserver.listener.PlayerListener;

import java.util.List;

public class ArtaPlugin extends JavaPlugin {
    private WebhookService webhookService;
    private static boolean debug;
    private static ArtaPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        List<String> webhooks = getConfig().getStringList("config.webhooks");
        debug = getConfig().getBoolean("config.debug");
        webhookService = new WebhookService(webhooks);
        getServer().getPluginManager().registerEvents(new PlayerListener(webhookService), this);

        getComponentLogger().info(Component.text("ArtaPlugin enabled!"));
    }

    @Override
    public void onDisable() {
        getComponentLogger().info(Component.text("ArtaPlugin disabled!"));
    }

    public static boolean isDebug() {
        return debug;
    }

    public static ArtaPlugin getInstance() {
        return instance;
    }
}
