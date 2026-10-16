package xyz.nothing.artaserver.cmd;


import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NonNull;
import xyz.nothing.artaserver.ArtaPlugin;
import xyz.nothing.artaserver.service.ArtaBoxService;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ArtaBoxCommand implements BasicCommand {
    private static final int COUNTDOWN_SECONDS = 5;

    private final ArtaBoxService service;
    private final Set<UUID> opening = new HashSet<>();

    public ArtaBoxCommand(ArtaBoxService service) {
        this.service = service;
    }

    @Override
    public void execute(@NonNull CommandSourceStack stack, String @NonNull [] args) {
        if (!(stack.getSender() instanceof Player player)) {
            return;
        }

        if (opening.contains(player.getUniqueId())) {
            player.sendMessage(Component.text("آرتا باکس شما در حال باز شدن است..."));
            return;
        }

        if (!service.hasAvailableBox(player)) {
            player.sendMessage(Component.text("شما حداکثر آرتاباکس های خودتان را باز کرده اید. فردا تلاش کنید."));
            return;
        }

        Plugin plugin = ArtaPlugin.getInstance();
        OpeningSequence sequence = new OpeningSequence(player);
        opening.add(player.getUniqueId());
        sequence.task = plugin.getServer().getScheduler().runTaskTimer(plugin, sequence, 0L, 20L);
    }

    /**
     * Counts down from {@link #COUNTDOWN_SECONDS} with a title and a click, then opens the box.
     * Runs on the main thread, once per second.
     */
    private final class OpeningSequence implements Runnable {
        private final Player player;
        private int remaining = COUNTDOWN_SECONDS;
        private BukkitTask task;

        private OpeningSequence(Player player) {
            this.player = player;
        }

        @Override
        public void run() {
            if (!player.isOnline()) {
                stop();
                return;
            }

            if (remaining > 0) {
                player.showTitle(Title.title(
                        Component.text(String.valueOf(remaining)),
                        Component.text("آرتا باکس در حال باز شدن است..."),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(700), Duration.ofMillis(200))));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                remaining--;
                return;
            }

            stop();
            ArtaBoxService.OpenResult result = service.openBox(player);
            if (result == ArtaBoxService.OpenResult.UNKNOWN_FAILURE) {
                player.sendMessage(Component.text("خطا هنگام بازکردن آرتا باکس."));
            }
        }

        private void stop() {
            opening.remove(player.getUniqueId());
            task.cancel();
        }
    }
}
