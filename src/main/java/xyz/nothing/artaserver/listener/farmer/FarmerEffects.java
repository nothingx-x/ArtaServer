package xyz.nothing.artaserver.listener.farmer;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import xyz.nothing.artaserver.job.JobType;
import xyz.nothing.artaserver.job.annotation.OnJob;

public class FarmerEffects {

    @OnJob(JobType.FARMER)
    public void onFarmerDamage(EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 0.5);
    }
}
