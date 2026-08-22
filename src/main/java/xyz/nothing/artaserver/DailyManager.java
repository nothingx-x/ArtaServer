package xyz.nothing.artaserver;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.nothing.artaserver.job.JobManager;
import xyz.nothing.artaserver.job.JobType;
import xyz.nothing.artaserver.job.PlayerJobData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DailyManager {
    private final JobManager jobManager;
    private final Map<UUID, Long> lastClaim = new HashMap<>();

    private static final long COOLDOWN_MS = 24 * 60 * 60 * 1000L; // 24 hours

    public DailyManager(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    /**
     * Attempt to claim the daily reward for a player.
     * Returns true if the reward was given, false otherwise (message already sent to player).
     */
    public boolean claimDaily(Player player) {
        PlayerJobData data = jobManager.getPlayerJob(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Component.text("You don't have a job! Use /job pick <job> first.", NamedTextColor.RED));
            return false;
        }

        // Check cooldown
        UUID uuid = player.getUniqueId();
        Long last = lastClaim.get(uuid);
        if (last != null) {
            long elapsed = System.currentTimeMillis() - last;
            if (elapsed < COOLDOWN_MS) {
                long remaining = COOLDOWN_MS - elapsed;
                long hours = remaining / (1000 * 60 * 60);
                long minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60);
                player.sendMessage(Component.text("You already claimed today! Come back in " + hours + "h " + minutes + "m.", NamedTextColor.RED));
                return false;
            }
        }

        // Build reward items
        ItemStack[] rewards = buildRewards(data.getJob(), data.getLevel());
        if (rewards == null || rewards.length == 0) {
            player.sendMessage(Component.text("No rewards available for your current level.", NamedTextColor.YELLOW));
            return false;
        }

        // Check inventory space
        int freeSlots = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType() == Material.AIR) {
                freeSlots++;
            }
        }
        if (freeSlots < rewards.length) {
            player.sendMessage(Component.text("Your inventory is full! Make some space and try again.", NamedTextColor.RED));
            return false;
        }

        // Give rewards
        for (ItemStack item : rewards) {
            player.getInventory().addItem(item);
        }

        lastClaim.put(uuid, System.currentTimeMillis());

        // Feedback
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("★ Daily Reward Claimed! ★", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Rewards: ", NamedTextColor.GREEN)
                .append(Component.text(rewards.length + " items", NamedTextColor.WHITE)));
        player.sendMessage(Component.empty());

        return true;
    }

    private ItemStack[] buildRewards(JobType job, int level) {
        return switch (job) {
            case FARMER -> buildFarmerRewards(level);
            case WARRIOR -> buildWarriorRewards(level);
            case SMITHMAN -> buildSmithmanRewards(level);
        };
    }

    // ---- Farmer: food & crops ----

    private ItemStack[] buildFarmerRewards(int level) {
        if (level < 10) {
            return new ItemStack[]{
                    new ItemStack(Material.BREAD, 5 + level),
                    new ItemStack(Material.CARROT, 3 + level),
            };
        } else if (level < 30) {
            return new ItemStack[]{
                    new ItemStack(Material.BREAD, 15),
                    new ItemStack(Material.GOLDEN_CARROT, 3 + (level / 10)),
                    new ItemStack(Material.PUMPKIN_PIE, 2 + (level / 15)),
            };
        } else if (level < 60) {
            return new ItemStack[]{
                    new ItemStack(Material.GOLDEN_CARROT, 8 + (level / 10)),
                    new ItemStack(Material.GOLDEN_APPLE, 1 + (level / 30)),
                    new ItemStack(Material.BREAD, 20),
                    new ItemStack(Material.BAKED_POTATO, 10 + level / 5),
            };
        } else {
            return new ItemStack[]{
                    new ItemStack(Material.GOLDEN_CARROT, 16),
                    new ItemStack(Material.GOLDEN_APPLE, 2 + (level / 30)),
                    new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, Math.min(level / 40, 3)),
                    new ItemStack(Material.BREAD, 32),
            };
        }
    }

    // ---- Warrior: weapons & combat gear ----

    private ItemStack[] buildWarriorRewards(int level) {
        if (level < 10) {
            return new ItemStack[]{
                    new ItemStack(Material.IRON_SWORD, 1),
                    new ItemStack(Material.ARROW, 16 + level * 2),
            };
        } else if (level < 30) {
            return new ItemStack[]{
                    new ItemStack(Material.IRON_SWORD, 1),
                    new ItemStack(Material.SHIELD, 1),
                    new ItemStack(Material.ARROW, 32),
                    new ItemStack(Material.IRON_INGOT, 5 + level / 5),
            };
        } else if (level < 60) {
            return new ItemStack[]{
                    new ItemStack(Material.DIAMOND_SWORD, 1),
                    new ItemStack(Material.SHIELD, 1),
                    new ItemStack(Material.ARROW, 64),
                    new ItemStack(Material.DIAMOND, 2 + level / 20),
            };
        } else {
            return new ItemStack[]{
                    new ItemStack(Material.DIAMOND_SWORD, 1),
                    new ItemStack(Material.SHIELD, 1),
                    new ItemStack(Material.NETHERITE_INGOT, 1 + level / 60),
                    new ItemStack(Material.ARROW, 64),
                    new ItemStack(Material.TOTEM_OF_UNDYING, Math.min(level / 80, 2)),
            };
        }
    }

    // ---- Smithman: ores & smelting materials ----

    private ItemStack[] buildSmithmanRewards(int level) {
        if (level < 10) {
            return new ItemStack[]{
                    new ItemStack(Material.IRON_INGOT, 5 + level),
                    new ItemStack(Material.COAL, 10 + level * 2),
            };
        } else if (level < 30) {
            return new ItemStack[]{
                    new ItemStack(Material.IRON_INGOT, 10 + level / 3),
                    new ItemStack(Material.GOLD_INGOT, 3 + level / 10),
                    new ItemStack(Material.LAPIS_LAZULI, 10 + level),
            };
        } else if (level < 60) {
            return new ItemStack[]{
                    new ItemStack(Material.IRON_INGOT, 20),
                    new ItemStack(Material.GOLD_INGOT, 8 + level / 10),
                    new ItemStack(Material.DIAMOND, 1 + level / 20),
                    new ItemStack(Material.REDSTONE, 16 + level),
            };
        } else {
            return new ItemStack[]{
                    new ItemStack(Material.IRON_INGOT, 32),
                    new ItemStack(Material.GOLD_INGOT, 16),
                    new ItemStack(Material.DIAMOND, 3 + level / 20),
                    new ItemStack(Material.NETHERITE_SCRAP, 1 + level / 60),
                    new ItemStack(Material.EMERALD, 8 + level / 10),
            };
        }
    }
}
