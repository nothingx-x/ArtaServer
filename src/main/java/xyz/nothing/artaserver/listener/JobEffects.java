package xyz.nothing.artaserver.listener;

import org.bukkit.entity.Monster;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import xyz.nothing.artaserver.job.JobConstants;
import xyz.nothing.artaserver.job.JobType;
import xyz.nothing.artaserver.job.annotation.OnJob;

import java.util.Iterator;
import java.util.Set;

import static xyz.nothing.artaserver.job.JobConstants.ORES;

/**
 * All job-specific debuffs and buffs in one place.
 * Registered via JobListenerScanner using reflection.
 */
public class JobEffects {

    // ─── FARMER ───

    @OnJob(JobType.FARMER)
    public void onFarmerDamage(EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 0.5);
    }

    // ─── WARRIOR ───

    private static final double WARRIOR_HOSTILE_DAMAGE_MULTIPLIER = 1.5;

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

    // Extra drops multiplier for Smithman (1.5 = 50% more)
    private static final double ORE_DROP_MULTIPLIER = 1.5;

    // ─── SMITHMAN ───

    @OnJob(JobType.SMITHMAN)
    public void onSmithmanOreDrop(BlockDropItemEvent event) {
        if (!ORES.contains(event.getBlockState().getType())) return;

        for (org.bukkit.entity.Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();
            int newAmount = (int) Math.ceil(stack.getAmount() * ORE_DROP_MULTIPLIER);
            stack.setAmount(newAmount);
        }
    }

    @OnJob(JobType.SMITHMAN)
    public void onSmithmanCropDrop(BlockDropItemEvent event) {
        if (!JobConstants.CROPS.contains(event.getBlockState().getType())) return;

        Iterator<org.bukkit.entity.Item> it = event.getItems().iterator();
        while (it.hasNext()) {
            org.bukkit.entity.Item item = it.next();
            ItemStack stack = item.getItemStack();
            int amount = stack.getAmount();

            if (amount <= 1) {
                if (Math.random() > 0.2) {
                    it.remove();
                }
            } else {
                int keep = (int) Math.ceil(amount * 0.2);
                if (keep < 1 && Math.random() <= 0.2) {
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
