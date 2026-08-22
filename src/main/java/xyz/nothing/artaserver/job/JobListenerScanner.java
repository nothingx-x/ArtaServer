package xyz.nothing.artaserver.job;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import xyz.nothing.artaserver.job.annotation.OnJob;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Scans a class for @OnJob annotated methods and registers them as Bukkit listeners.
 *
 * Usage:
 *   JobListenerScanner.scan(plugin, new MyJobEffects(), JobManager.getInstance());
 */
public class JobListenerScanner {

    /**
     * Scan a handler class for @OnJob methods and register each as a Bukkit listener.
     *
     * @param plugin     the plugin instance
     * @param handler    the object containing @OnJob methods
     * @param jobManager used to check the player's current job
     */
    public static void scan(Plugin plugin, Object handler, JobManager jobManager) {
        Logger logger = plugin.getLogger();

        for (Method method : handler.getClass().getDeclaredMethods()) {
            OnJob annotation = method.getAnnotation(OnJob.class);
            if (annotation == null) continue;

            JobType requiredJob = annotation.value();
            Class<?>[] params = method.getParameterTypes();

            if (params.length != 1 || !Event.class.isAssignableFrom(params[0])) {
                logger.warning("@OnJob method must have exactly one Event parameter: "
                        + handler.getClass().getSimpleName() + "#" + method.getName());
                continue;
            }

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) params[0];

            method.setAccessible(true);

            // Create a Bukkit listener with a dynamic EventExecutor
            Listener bukkitListener = new Listener() {};
            EventExecutor executor = (listener, event) -> {
                // Find the player from the event
                Player player = resolvePlayer(event);
                if (player == null) return;

                // Check if the player has the required job
                PlayerJobData data = jobManager.getPlayerJob(player.getUniqueId());
                if (data == null || data.getJob() != requiredJob) return;

                // Invoke the handler method
                try {
                    method.invoke(handler, event);
                } catch (Exception e) {
                    logger.severe("Failed to invoke @OnJob method: " + method.getName() + " - " + e.getMessage());
                }
            };

            Bukkit.getPluginManager().registerEvent(eventClass, bukkitListener, EventPriority.NORMAL, executor, plugin);
            logger.info("Registered @OnJob handler: " + requiredJob.name() + " -> "
                    + handler.getClass().getSimpleName() + "#" + method.getName()
                    + " (" + eventClass.getSimpleName() + ")");
        }
    }

    /**
     * Try to extract a Player from a Bukkit Event using common getter methods.
     */
    private static Player resolvePlayer(Event event) {
        try {
            // PlayerEvent.getPlayer()
            Method getPlayer = event.getClass().getMethod("getPlayer");
            Object result = getPlayer.invoke(event);
            if (result instanceof Player player) return player;
        } catch (Exception ignored) {}

        try {
            // EntityDamageByEntityEvent.getDamager() -> LivingEntity -> Player
            Method getDamager = event.getClass().getMethod("getDamager");
            Object damager = getDamager.invoke(event);
            if (damager instanceof Player player) return player;
        } catch (Exception ignored) {}

        try {
            // EntityDamageEvent.getEntity() -> LivingEntity -> Player
            Method getEntity = event.getClass().getMethod("getEntity");
            Object entity = getEntity.invoke(event);
            if (entity instanceof Player player) return player;
        } catch (Exception ignored) {}

        return null;
    }
}
