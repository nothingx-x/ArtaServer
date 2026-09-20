package xyz.nothing.artaserver.service;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.nothing.artaserver.ArtaPlugin;
import xyz.nothing.artaserver.event.ArtaBoxResultEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

public class ArtaBoxService {
    private static final ArtaBox ARTABOX_RARE = new ArtaBox("artabox_rare", "آرتا باکس ریر", ArtaBox.Rarity.RARE);
    private static final ArtaBox ARTABOX_EPIC = new ArtaBox("artabox_epic", "آرتا باکس اپیک", ArtaBox.Rarity.EPIC);
    private static final ArtaBox ARTABOX_MYTHIC = new ArtaBox("artabox_mythic", "آرتا باکس میتیک", ArtaBox.Rarity.MYTHIC);
    private static final ArtaBox ARTABOX_LEGENDARY = new ArtaBox("artabox_legendary", "آرتا باکس لجندری", ArtaBox.Rarity.LEGENDARY);
    public static final Set<ArtaBox> ARTA_BOXES;
    public static Map<ArtaBox.Rarity, Double> CHANCE_PER_RARITY;

    private static final Duration DAY = Duration.ofDays(1);
    public static final int MAX_BOX_PER_DAY = 4;

    private final SecureRandom r = new SecureRandom();
    private final Map<UUID, PlayerOpenedBoxes> playerOpenedBoxesMap = new HashMap<>();
    private final File file = Path.of(ArtaPlugin.getInstance().getDataFolder().toPath().toString(), "boxes.yml").toFile();
    private FileConfiguration config;

    static {
        Map<ArtaBox.Rarity, Double> m = new LinkedHashMap<>();
        m.put(ArtaBox.Rarity.RARE, 0.6);
        m.put(ArtaBox.Rarity.EPIC, 0.2);
        m.put(ArtaBox.Rarity.MYTHIC, 0.15);
        m.put(ArtaBox.Rarity.LEGENDARY, 0.05);
        CHANCE_PER_RARITY = Collections.unmodifiableMap(m);

        ARTABOX_RARE.addItem(Material.BREAD.name(), 0.5);
        ARTABOX_RARE.addItem(Material.EMERALD.name(), 0.2);
        ARTABOX_RARE.addItem(Material.GOLDEN_APPLE.name(), 0.1);
        ARTABOX_RARE.addItem(Material.CACTUS.name(), 0.1);
        ARTABOX_RARE.addItem(Material.DIAMOND_HOE.name(), 0.01);
        ARTABOX_RARE.addItem(Material.AMETHYST_BLOCK.name(), 0.09);

        ARTABOX_EPIC.addItem(Material.DIAMOND.name(), 0.1);
        ARTABOX_EPIC.addItem(Material.IRON_SWORD.name(), 0.4);
        ARTABOX_EPIC.addItem(Material.BAKED_POTATO.name(), 0.4);
        ARTABOX_EPIC.addItem(Material.NETHER_STAR.name(), 0.1);

        ARTA_BOXES = Set.of(ARTABOX_RARE, ARTABOX_EPIC, ARTABOX_MYTHIC, ARTABOX_LEGENDARY);
    }

    public ArtaBoxService() {
        config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        for (String key : config.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            PlayerOpenedBoxes data = new PlayerOpenedBoxes(uuid);
            data.openedDaily = config.getInt(key + ".openedDaily", 0);
            long last = config.getLong(key + ".lastOpened", System.currentTimeMillis());
            data.lastOpened = Instant.ofEpochMilli(last);
            playerOpenedBoxesMap.put(uuid, data);
        }
    }

    private void save() {
        for (Map.Entry<UUID, PlayerOpenedBoxes> entry : playerOpenedBoxesMap.entrySet()) {
            String path = entry.getKey().toString();
            config.set(path + ".openedDaily", entry.getValue().openedDaily);
            config.set(path + ".lastOpened", entry.getValue().lastOpened);
        }
        try {
            config.save(file);
        } catch (IOException e) {
            ArtaPlugin.getInstance().getLogger().log(Level.SEVERE, "Failed to save boxes.yml", e);
        }
    }

    public OpenResult openBox(Player player) {
        PlayerOpenedBoxes playerOpenedBoxes = playerOpenedBoxesMap.computeIfAbsent(player.getUniqueId(), PlayerOpenedBoxes::new);

        Duration elapsed = Duration.between(playerOpenedBoxes.lastOpened, Instant.now());

        if (elapsed.toMillis() < DAY.toMillis() && playerOpenedBoxes.openedDaily >= MAX_BOX_PER_DAY) {
            return OpenResult.REACHED_MAX_BOX_OPENED;
        } else if (elapsed.toMillis() >= DAY.toMillis()) {
            playerOpenedBoxes.openedDaily = 0;
        }

        double chance = r.nextDouble();
        double cumulative = 0;

        Optional<ArtaBox.Rarity> found = Optional.empty();
        for (Map.Entry<ArtaBox.Rarity, Double> entry : CHANCE_PER_RARITY.entrySet()) {
            cumulative += entry.getValue();
            if (chance < cumulative) {
                found = Optional.of(entry.getKey());
                break;
            }
        }

        ArtaBox.Rarity rarity = found.orElse(ArtaBox.Rarity.RARE);

        Optional<ArtaBox> box = ARTA_BOXES
                .stream()
                .filter(artaBox -> artaBox.getRarity().equals(rarity))
                .findFirst();

        if (box.isEmpty()) return OpenResult.UNKNOWN_FAILURE;

        double itemChance = r.nextDouble();
        double cumulativeItem = 0;
        Optional<String> itemId = Optional.empty();
        for (Map.Entry<String, Double> entry : box.get().getItemChances().entrySet()) {
            cumulativeItem += entry.getValue();

            if (itemChance < cumulativeItem) {
                String key = entry.getKey();
                itemId = Optional.of(key);
                break;
            }
        }

        if (itemId.isEmpty()) return OpenResult.UNKNOWN_FAILURE;

        Material item = Material.valueOf(itemId.get());

        playerOpenedBoxes.openedDaily = Math.min(MAX_BOX_PER_DAY, playerOpenedBoxes.openedDaily + 1);
        playerOpenedBoxes.lastOpened = Instant.now();
        save();

        Bukkit.getServer().getPluginManager().callEvent(new ArtaBoxResultEvent(player, box.get(), item));
        return OpenResult.SUCCESS;
    }

    public enum OpenResult {
        REACHED_MAX_BOX_OPENED,
        SUCCESS,
        UNKNOWN_FAILURE
    }

    public static class PlayerOpenedBoxes {
        private final UUID uuid;
        private final Map<String, BoxResult> openedBoxes = new HashMap<>();
        private int openedDaily;
        private Instant lastOpened;

        public PlayerOpenedBoxes(UUID uuid) {
            this.uuid = uuid;
            this.lastOpened = Instant.now();
        }

        public UUID getUuid() {
            return uuid;
        }

        public record BoxResult(String wonItem, int amountOfOpenedBoxes) {

        }
    }

    public static class ArtaBox {
        private final String id;
        private final String displayName;
        private final Map<String, Double> itemChances = new LinkedHashMap<>();
        private final Rarity rarity;

        public ArtaBox(String id, String displayName, Rarity rarity) {
            this.id = id;
            this.displayName = displayName;
            this.rarity = rarity;
        }

        public Map<String, Double> getItemChances() {
            return itemChances;
        }

        public void setItemChances(Map<String, Double> itemChances) {
            this.itemChances.clear();
            this.itemChances.putAll(itemChances);
        }

        public void addItem(String id, double chance) {
            itemChances.put(id, chance);
        }

        public String getId() {
            return id;
        }

        public Optional<Double> getItemChance(String id) {
            return Optional.ofNullable(itemChances.get(id));
        }

        public Rarity getRarity() {
            return rarity;
        }

        public String getDisplayName() {
            return displayName;
        }

        public enum Rarity {
            RARE,
            EPIC,
            MYTHIC,
            LEGENDARY
        }
    }
}
