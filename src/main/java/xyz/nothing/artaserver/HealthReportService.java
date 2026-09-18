package xyz.nothing.artaserver;


import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.nothing.artaserver.event.HealthReportEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class HealthReportService {
    private final File file = Path.of(ArtaPlugin.getInstance().getDataFolder().toPath().toString(), "health_report.yml").toFile();
    private FileConfiguration config;

    private Instant lastCheck;
    private Duration interval;
    private boolean stopped;

    public void init() {
        config = YamlConfiguration.loadConfiguration(file);
        long intervalMinutes = config.getLong("interval_minute");
        lastCheck = Instant.ofEpochMilli(config.getLong("last_check_ms"));
        interval = Duration.ofMinutes(intervalMinutes);
    }

    public void run() {
        if (stopped) return;
        var elapsed = Duration.between(lastCheck, Instant.now());
        if (elapsed.toMillis() < interval.toMillis()) {
            return;
        }

        lastCheck = Instant.now();
        config.set("last_check_ms", lastCheck.toEpochMilli());
        try {
            config.save(file);
        } catch (IOException e) {
            ArtaPlugin.getInstance().getLogger().log(Level.SEVERE, "Could not save health report file!", e);
        }

        Map<String, Integer> entities = new HashMap<>();
        for (World world : Bukkit.getServer().getWorlds()) {
            entities.put(world.getName(), world.getEntities().size());
        }
        double tps = Arrays.stream(Bukkit.getServer().getTPS()).average().orElse(0);
        HealthReport healthReport = new HealthReport(entities, tps);

        ArtaPlugin.getInstance().getServer().getPluginManager().callEvent(new HealthReportEvent(healthReport));
    }

    public void stop() {
        stopped = true;
    }

    public record HealthReport(Map<String, Integer> entities, double tps) {

    }
}
