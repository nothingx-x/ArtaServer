package xyz.nothing.artaserver.service;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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
    public static final Map<ArtaBox.Rarity, Double> CHANCE_PER_RARITY;

    private static final Duration DAY = Duration.ofDays(1);
    /**
     * Records unused for longer than this are dropped from boxes.yml. A player's daily
     * counter is only meaningful for 24 hours, so anything beyond this is dead weight.
     */
    private static final Duration STALE_RECORD_RETENTION = Duration.ofDays(30);
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

        ARTABOX_MYTHIC.addItem(Material.COAL.name(), 0.1);
        ARTABOX_MYTHIC.addItem(Material.NETHERITE_INGOT.name(), 0.1);
        ARTABOX_MYTHIC.addItem(Material.CONDUIT.name(), 0.1);
        ARTABOX_MYTHIC.addItem(Material.ENCHANTED_GOLDEN_APPLE.name(), 0.1);
        ARTABOX_MYTHIC.addItem(Material.CACTUS_FLOWER.name(), 0.6);

        ARTABOX_LEGENDARY.addItem(Material.NETHERITE_INGOT.name(), 0.2);
        ARTABOX_LEGENDARY.addItem(Material.VILLAGER_SPAWN_EGG.name(), 0.2);
        ARTABOX_LEGENDARY.addItem(Material.DIAMOND.name(), 0.2);
        ARTABOX_LEGENDARY.addItem(Material.ELYTRA.name(), 0.1);
        ARTABOX_LEGENDARY.addItem(Material.DRAGON_HEAD.name(), 0.1);
        ARTABOX_LEGENDARY.addItem(Material.DRAGON_EGG.name(), 0.1);
        ARTABOX_LEGENDARY.addItem(Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE.name(), 0.1);

        ARTA_BOXES = Set.of(ARTABOX_RARE, ARTABOX_EPIC, ARTABOX_MYTHIC, ARTABOX_LEGENDARY);
    }

    public ArtaBoxService() {
        config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private static UUID parseUuid(String key) {
        try {
            return UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void load() {
        for (String key : config.getKeys(false)) {
            UUID uuid = parseUuid(key);
            if (uuid == null) {
                continue;
            }
            PlayerOpenedBoxes data = new PlayerOpenedBoxes(uuid);
            long last = config.getLong(key + ".lastOpened", System.currentTimeMillis());
            long reset = config.getLong(key + ".resetsAt", System.currentTimeMillis());
            data.openedDaily = config.getInt(key + ".openedDaily", 0);
            data.lastOpened = Instant.ofEpochMilli(last);
            data.resetsAt = Instant.ofEpochMilli(reset);
            ConfigurationSection openedBoxes = config.getConfigurationSection(key + ".openedBoxes");
            if (openedBoxes != null) {
                for (String boxId : openedBoxes.getKeys(false)) {
                    data.openedBoxes.put(boxId, openedBoxes.getInt(boxId, 0));
                }
            }
            playerOpenedBoxesMap.put(uuid, data);
        }
    }

    /**
     * Persists every tracked player and prunes records that no longer matter. Called on the
     * main thread after each successful open.
     */
    private void save() {
        Instant now = Instant.now();

        playerOpenedBoxesMap.entrySet().removeIf(entry ->
                Duration.between(entry.getValue().lastOpened, now).compareTo(STALE_RECORD_RETENTION) > 0);

        // Remove keys that no longer correspond to a tracked player (pruned above, or
        // hand-edited/leftover entries that load() refused to parse).
        int prunedKeys = 0;
        for (String key : List.copyOf(config.getKeys(false))) {
            UUID uuid = parseUuid(key);
            if (uuid != null && !playerOpenedBoxesMap.containsKey(uuid)) {
                config.set(key, null);
                prunedKeys++;
            }
        }

        for (Map.Entry<UUID, PlayerOpenedBoxes> entry : playerOpenedBoxesMap.entrySet()) {
            String path = entry.getKey().toString();
            PlayerOpenedBoxes data = entry.getValue();
            config.set(path + ".openedDaily", data.openedDaily);
            config.set(path + ".lastOpened", data.lastOpened.toEpochMilli());
            config.set(path + ".resetsAt", data.resetsAt.toEpochMilli());
            for (Map.Entry<String, Integer> openedBox : data.openedBoxes.entrySet()) {
                config.set(path + ".openedBoxes." + openedBox.getKey(), openedBox.getValue());
            }
        }

        try {
            config.save(file);
            if (prunedKeys > 0 && ArtaPlugin.isDebug()) {
                ArtaPlugin.getInstance().getLogger().info("Pruned " + prunedKeys + " stale record(s) from boxes.yml");
            }
        } catch (IOException e) {
            ArtaPlugin.getInstance().getLogger().log(Level.SEVERE, "Failed to save boxes.yml", e);
        }
    }

    /**
     * Reports whether the player may open a box right now, without consuming one. Applies the
     * lazy window reset, so it is safe to call before an animation or other delay.
     */
    public boolean hasAvailableBox(Player player) {
        PlayerOpenedBoxes data = playerOpenedBoxesMap.computeIfAbsent(player.getUniqueId(), PlayerOpenedBoxes::new);
        return data.hasAvailableBox(Instant.now());
    }

    public OpenResult openBox(Player player) {
        PlayerOpenedBoxes playerOpenedBoxes = playerOpenedBoxesMap.computeIfAbsent(player.getUniqueId(), PlayerOpenedBoxes::new);

        Instant now = Instant.now();
        if (!playerOpenedBoxes.hasAvailableBox(now)) {
            return OpenResult.REACHED_MAX_BOX_OPENED;
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

        ArtaBox artaBox = box.get();

        double itemChance = r.nextDouble();
        double cumulativeItem = 0;
        Optional<String> itemId = Optional.empty();
        for (Map.Entry<String, Double> entry : artaBox.getItemChances().entrySet()) {
            cumulativeItem += entry.getValue();

            if (itemChance < cumulativeItem) {
                String key = entry.getKey();
                itemId = Optional.of(key);
                break;
            }
        }

        if (itemId.isEmpty()) return OpenResult.UNKNOWN_FAILURE;

        Material item;
        try {
            item = Material.valueOf(itemId.get());
        } catch (IllegalArgumentException e) {
            if (ArtaPlugin.isDebug()) {
                ArtaPlugin.getInstance().getLogger().log(Level.WARNING, "Failed to find art item " + itemId.get(), e);
            }
            return OpenResult.UNKNOWN_FAILURE;
        }

        playerOpenedBoxes.openedDaily = Math.min(MAX_BOX_PER_DAY, playerOpenedBoxes.openedDaily + 1);
        playerOpenedBoxes.openedBoxes.merge(artaBox.getId(), 1, Integer::sum);
        playerOpenedBoxes.lastOpened = now;
        save();

        Bukkit.getServer().getPluginManager().callEvent(new ArtaBoxResultEvent(player, artaBox, item));
        return OpenResult.SUCCESS;
    }

    public enum OpenResult {
        REACHED_MAX_BOX_OPENED,
        SUCCESS,
        UNKNOWN_FAILURE
    }

    public static class PlayerOpenedBoxes {
        private final UUID uuid;
        private final Map<String, Integer> openedBoxes = new LinkedHashMap<>();
        private int openedDaily;
        private Instant lastOpened;
        private Instant resetsAt;

        public PlayerOpenedBoxes(UUID uuid) {
            this.uuid = uuid;
            this.lastOpened = Instant.now();
            this.resetsAt = Instant.now().plus(DAY);
        }

        /**
         * Starts a new window when the current one has expired and reports whether a box is
         * available. Does not consume a box; the caller increments {@code openedDaily} once the
         * open succeeds.
         */
        public boolean hasAvailableBox(Instant now) {
            if (!now.isBefore(resetsAt)) {
                openedDaily = 0;
                resetsAt = now.plus(DAY);
            }
            return openedDaily < MAX_BOX_PER_DAY;
        }

        public UUID getUuid() {
            return uuid;
        }

        public Map<String, Integer> getOpenedBoxes() {
            return openedBoxes;
        }

        public int getOpenedDaily() {
            return openedDaily;
        }

        public Instant getLastOpened() {
            return lastOpened;
        }

        public Instant getResetsAt() {
            return resetsAt;
        }

        public void setOpenedDaily(int openedDaily) {
            this.openedDaily = openedDaily;
        }

        public void setLastOpened(Instant lastOpened) {
            this.lastOpened = lastOpened;
        }

        public void setResetsAt(Instant resetsAt) {
            this.resetsAt = resetsAt;
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
