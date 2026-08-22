package xyz.nothing.artaserver.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.ItemStack;
import xyz.nothing.artaserver.job.JobManager;
import xyz.nothing.artaserver.job.JobType;
import xyz.nothing.artaserver.job.PlayerJobData;

import java.util.Map;
import java.util.Random;

public class SmithmanListener implements Listener {
    private final JobManager jobManager;
    private final Random random = new Random();

    private static final int XP_PER_ITEM = 2;

    // Enchanting table boost constants
    // At level 1: +0 (no boost), level 10: +1, level 30: +2, level 60: +3, level 100: +4
    private static final int MAX_LEVEL_BONUS = 4;

    // Chance per enchantment slot to gain a bonus level, scaled by job level
    private static final double BONUS_LEVEL_CHANCE_PER_SLOT = 0.02;

    // Chance to add a bonus enchantment (second enchantment on the item)
    private static final double BONUS_ENCHANTMENT_BASE_CHANCE = 0.05;
    private static final double BONUS_ENCHANTMENT_LEVEL_SCALE = 0.003;

    public SmithmanListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    // ---- Furnace XP (existing) ----

    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        PlayerJobData data = jobManager.getPlayerJob(player.getUniqueId());

        if (data == null || data.getJob() != JobType.SMITHMAN) return;

        int itemsSmelted = event.getItemAmount();
        int xp = itemsSmelted * XP_PER_ITEM;

        int levelsGained = data.addXp(xp);
        jobManager.saveProgress(player.getUniqueId());

        sendXpMessage(player, xp, data);

        if (levelsGained > 0) {
            sendLevelUpMessage(player, data);
        }
    }

    // ---- Enchantment table boost ----

    @EventHandler
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        Player player = event.getEnchanter();
        PlayerJobData data = jobManager.getPlayerJob(player.getUniqueId());

        if (data == null || data.getJob() != JobType.SMITHMAN) return;

        int levelBonus = calculateLevelBonus(data.getLevel());

        EnchantmentOffer[] offers = event.getOffers();
        for (int i = 0; i < offers.length; i++) {
            if (offers[i] == null) continue;

            Enchantment ench = offers[i].getEnchantment();
            int currentLevel = offers[i].getEnchantmentLevel();
            int maxLevel = ench.getMaxLevel();

            // Always boost by levelBonus, capped at max
            int boostedLevel = Math.min(currentLevel + levelBonus, maxLevel);

            // Extra chance for an additional +1 based on job level
            if (boostedLevel < maxLevel && random.nextDouble() < BONUS_LEVEL_CHANCE_PER_SLOT * data.getLevel()) {
                boostedLevel = Math.min(boostedLevel + 1, maxLevel);
            }

            if (boostedLevel > currentLevel) {
                offers[i].setEnchantmentLevel(boostedLevel);
                // Slightly increase cost to keep it balanced
                offers[i].setCost(Math.min(offers[i].getCost() + 1, 50));
            }
        }
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        PlayerJobData data = jobManager.getPlayerJob(player.getUniqueId());

        if (data == null || data.getJob() != JobType.SMITHMAN) return;

        // Give Smithman XP for enchanting
        int xp = event.getExpLevelCost() * 2;
        int levelsGained = data.addXp(xp);
        jobManager.saveProgress(player.getUniqueId());

        sendXpMessage(player, xp, data);

        if (levelsGained > 0) {
            sendLevelUpMessage(player, data);
        }

        // Chance to add a bonus enchantment at higher Smithman levels
        double bonusChance = BONUS_ENCHANTMENT_BASE_CHANCE + (data.getLevel() * BONUS_ENCHANTMENT_LEVEL_SCALE);
        if (random.nextDouble() < bonusChance) {
            addBonusEnchantment(event.getItem(), event.getEnchantsToAdd(), data.getLevel());
        }
    }

    /**
     * Calculate the level bonus for enchantment offers based on Smithman job level.
     * Level 1-9: +0, Level 10-29: +1, Level 30-59: +2, Level 60-89: +3, Level 90+: +4
     */
    private int calculateLevelBonus(int jobLevel) {
        if (jobLevel >= 90) return MAX_LEVEL_BONUS;
        if (jobLevel >= 60) return 3;
        if (jobLevel >= 30) return 2;
        if (jobLevel >= 10) return 1;
        return 0;
    }

    /**
     * Add a random bonus enchantment to the item, picked from enchantments compatible
     * with the item type that aren't already present.
     */
    private void addBonusEnchantment(ItemStack item, Map<Enchantment, Integer> existingEnchants, int jobLevel) {
        for (Enchantment ench : Enchantment.values()) {
            if (!ench.canEnchantItem(item)) continue;
            if (existingEnchants.containsKey(ench)) continue;
            if (ench.isTreasure()) continue; // Don't add treasure enchants
            if (!ench.isDiscoverable()) continue;

            // Pick a reasonable level: 1 to half of max, scaled by job level
            int maxPossible = Math.min(ench.getMaxLevel(), 1 + (jobLevel / 25));
            if (maxPossible < 1) continue;

            int bonusLevel = 1 + random.nextInt(maxPossible);
            existingEnchants.put(ench, bonusLevel);
            return; // Only add one bonus enchantment
        }
    }

    // ---- Messages ----

    private void sendXpMessage(Player player, int xp, PlayerJobData data) {
        player.sendActionBar(Component.text("+" + xp + " Job XP [" + data.getXp() + "/" + data.getXpToNextLevel() + "]",
                NamedTextColor.AQUA));
    }

    private void sendLevelUpMessage(Player player, PlayerJobData data) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("★ JOB LEVEL UP! ★", NamedTextColor.GOLD));
        player.sendMessage(Component.text("You are now Smithman level ", NamedTextColor.AQUA)
                .append(Component.text(data.getLevel(), NamedTextColor.WHITE))
                .append(Component.text("!", NamedTextColor.AQUA)));
        player.sendMessage(Component.empty());
    }
}
