package xyz.nothing.artaserver.listener.warrior;

import org.bukkit.entity.Monster;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.ItemStack;
import xyz.nothing.artaserver.job.JobConstants;
import xyz.nothing.artaserver.job.JobType;
import xyz.nothing.artaserver.job.annotation.OnJob;

import java.util.Iterator;

public class WarriorEffects {

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
}
