package xyz.nothing.artaserver.job.annotation;

import xyz.nothing.artaserver.job.JobType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a job-specific event handler.
 * The method must have exactly one parameter: the Bukkit Event class.
 *
 * Example:
 *   @OnJob(JobType.FARMER)
 *   public void onFarmerDamage(EntityDamageByEntityEvent event) {
 *       event.setDamage(event.getDamage() * 0.5);
 *   }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnJob {
    JobType value();
}
