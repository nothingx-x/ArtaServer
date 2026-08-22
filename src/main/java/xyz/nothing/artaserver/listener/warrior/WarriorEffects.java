package xyz.nothing.artaserver.listener.warrior;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import xyz.nothing.artaserver.job.JobConstants;
import xyz.nothing.artaserver.job.JobType;
import xyz.nothing.artaserver.job.annotation.OnJob;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Iterator;
import java.util.Set;

public class WarriorEffects {

    private static final double WARRIOR_HOSTILE_DAMAGE_MULTIPLIER = 2;
    private static final double TOOL_CRAFT_BREAK_CHANCE = 0.5;
    private static final double ORE_CRAFT_DROP_LOSS = 0.7;

    private static final Set<Material> ORES = Set.of(
            Material.COAL, Material.RAW_IRON, Material.RAW_GOLD, Material.RAW_COPPER,
            Material.DIAMOND, Material.EMERALD, Material.LAPIS_LAZULI, Material.REDSTONE,
            Material.AMETHYST_SHARD, Material.QUARTZ,
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.COPPER_INGOT,
            Material.NETHERITE_SCRAP
    );

    private static final Set<Material> TOOLS = Set.of(
            Material.WOODEN_SWORD, Material.WOODEN_PICKAXE, Material.WOODEN_AXE, Material.WOODEN_SHOVEL, Material.WOODEN_HOE,
            Material.STONE_SWORD, Material.STONE_PICKAXE, Material.STONE_AXE, Material.STONE_SHOVEL, Material.STONE_HOE,
            Material.IRON_SWORD, Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_SHOVEL, Material.IRON_HOE,
            Material.GOLDEN_SWORD, Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE, Material.GOLDEN_SHOVEL, Material.GOLDEN_HOE,
            Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL, Material.DIAMOND_HOE,
            Material.NETHERITE_SWORD, Material.NETHERITE_PICKAXE, Material.NETHERITE_AXE, Material.NETHERITE_SHOVEL, Material.NETHERITE_HOE
    );

    @OnJob(JobType.WARRIOR)
    public void onWarriorCraft(CraftItemEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getRecipe().getResult();
        Inventory craftGrid = event.getInventory();
        Location loc = player.getLocation();

        // 20% chance to break tool on craft
        if (TOOLS.contains(result.getType()) && Math.random() < TOOL_CRAFT_BREAK_CHANCE) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Your tool broke while crafting!", NamedTextColor.RED));

            // Drop 40% of ore ingredients
            boolean droppedAny = false;
            for (int i = 0; i < craftGrid.getSize(); i++) {
                ItemStack slot = craftGrid.getItem(i);
                if (slot == null || !ORES.contains(slot.getType())) continue;

                int amount = slot.getAmount();
                int lost = (int) Math.ceil(amount * ORE_CRAFT_DROP_LOSS);
                if (lost < 1 && Math.random() < ORE_CRAFT_DROP_LOSS) lost = 1;
                if (lost < 1) continue;

                ItemStack dropped = slot.clone();
                dropped.setAmount(lost);
                loc.getWorld().dropItemNaturally(loc, dropped);
                droppedAny = true;
            }
            if (droppedAny) {
                player.sendActionBar(Component.text("Some ores were dropped while crafting!", NamedTextColor.YELLOW));
            }
        }
    }

    @OnJob(JobType.WARRIOR)
    public void onWarriorHostileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Monster)) return;
        event.setDamage(event.getDamage() * WARRIOR_HOSTILE_DAMAGE_MULTIPLIER);
    }

    @OnJob(JobType.WARRIOR)
    public void onWarriorHunger(FoodLevelChangeEvent event) {
        int oldLevel = event.getEntity().getFoodLevel();
        int newLevel = event.getFoodLevel();

        if (newLevel >= oldLevel) return;

        int loss = oldLevel - newLevel;
        int extraLoss = (int) Math.ceil(loss * 0.5);
        event.setFoodLevel(Math.max(0, newLevel - extraLoss));
    }

    @OnJob(JobType.WARRIOR)
    public void onWarriorCropDrop(BlockDropItemEvent event) {
        if (!JobConstants.CROPS.contains(event.getBlockState().getType())) return;

        Iterator<org.bukkit.entity.Item> it = event.getItems().iterator();
        while (it.hasNext()) {
            org.bukkit.entity.Item item = it.next();
            ItemStack stack = item.getItemStack();
            int amount = stack.getAmount();

            if (amount <= 1) {
                if (Math.random() > 0.4) {
                    it.remove();
                }
            } else {
                int keep = (int) Math.ceil(amount * 0.4);
                if (keep < 1 && Math.random() <= 0.4) {
                    keep = 1;
                }
                if (keep < 1) {
                    it.remove();
                } else {
                    stack.setAmount(keep);
                }
            }
        }
    }
}
