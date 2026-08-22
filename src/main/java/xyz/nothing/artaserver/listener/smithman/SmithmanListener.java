package xyz.nothing.artaserver.listener.smithman;

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

    // Enchantment table boost constants
    private static final int MAX_LEVEL_BONUS = 4;
    private static final double BONUS_LEVEL_CHANCE_PER_SLOT = 0.02;
    private static final double BONUS_ENCHANTMENT_BASE_CHANCE = 0.05;
    private static final double BONUS_ENCHANTMENT_LEVEL_SCALE = 0.003;

    public SmithmanListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    // ---- Furnace XP ----

    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        PlayerJobData data = jobManager.getPlayerJob(player.getUniqueId());

        if (data == null || data.getJob() != JobType.SMITHMAN) return;

        int xp = event.getItemAmount() * XP_PER_ITEM;
        jobManager.grantXp(player, xp);
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

            int boostedLevel = Math.min(currentLevel + levelBonus, maxLevel);

            if (boostedLevel < maxLevel && random.nextDouble() < BONUS_LEVEL_CHANCE_PER_SLOT * data.getLevel()) {
                boostedLevel = Math.min(boostedLevel + 1, maxLevel);
            }

            if (boostedLevel > currentLevel) {
                offers[i].setEnchantmentLevel(boostedLevel);
                offers[i].setCost(Math.min(offers[i].getCost() + 1, 50));
            }
        }
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        PlayerJobData data = jobManager.getPlayerJob(player.getUniqueId());

        if (data == null || data.getJob() != JobType.SMITHMAN) return;

        int xp = event.getExpLevelCost() * 2;
        jobManager.grantXp(player, xp);

        // Chance to add a bonus enchantment at higher Smithman levels
        double bonusChance = BONUS_ENCHANTMENT_BASE_CHANCE + (data.getLevel() * BONUS_ENCHANTMENT_LEVEL_SCALE);
        if (random.nextDouble() < bonusChance) {
            addBonusEnchantment(event.getItem(), event.getEnchantsToAdd(), data.getLevel());
        }
    }

    private int calculateLevelBonus(int jobLevel) {
        if (jobLevel >= 90) return MAX_LEVEL_BONUS;
        if (jobLevel >= 60) return 3;
        if (jobLevel >= 30) return 2;
        if (jobLevel >= 10) return 1;
        return 0;
    }

    private void addBonusEnchantment(ItemStack item, Map<Enchantment, Integer> existingEnchants, int jobLevel) {
        for (Enchantment ench : Enchantment.values()) {
            if (!ench.canEnchantItem(item)) continue;
            if (existingEnchants.containsKey(ench)) continue;
            if (ench.isTreasure()) continue;
            if (!ench.isDiscoverable()) continue;

            int maxPossible = Math.min(ench.getMaxLevel(), 1 + (jobLevel / 25));
            if (maxPossible < 1) continue;

            int bonusLevel = 1 + random.nextInt(maxPossible);
            existingEnchants.put(ench, bonusLevel);
            return;
        }
    }
}
