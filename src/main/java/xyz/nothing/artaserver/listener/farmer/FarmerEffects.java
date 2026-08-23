package xyz.nothing.artaserver.listener.farmer;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import xyz.nothing.artaserver.job.JobType;
import xyz.nothing.artaserver.job.annotation.OnJob;

public class FarmerEffects {

    // Farmers lose hunger 50% slower
    private static final double HUNGER_LOSS_MULTIPLIER = 0.5;

    @OnJob(JobType.FARMER)
    public void onFarmerDamage(EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 0.5);
    }

    @OnJob(JobType.FARMER)
    public void onFarmerHunger(FoodLevelChangeEvent event) {
        int oldLevel = event.getEntity().getFoodLevel();
        int newLevel = event.getFoodLevel();

        // Only affect hunger loss (not gain)
        if (newLevel >= oldLevel) return;

        int loss = oldLevel - newLevel;
        int reducedLoss = (int) Math.ceil(loss * HUNGER_LOSS_MULTIPLIER);
        event.setFoodLevel(Math.max(0, oldLevel - reducedLoss));
    }
}
