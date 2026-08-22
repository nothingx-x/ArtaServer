package xyz.nothing.artaserver.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import xyz.nothing.artaserver.job.JobManager;
import xyz.nothing.artaserver.job.JobType;
import xyz.nothing.artaserver.job.PlayerJobData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JobCommand implements CommandExecutor, TabCompleter {
    private final JobManager jobManager;

    public JobCommand(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "pick" -> handlePick(player, args);
            case "info" -> handleInfo(player);
            default -> sendHelp(player);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("pick", "info"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("pick")) {
            completions.addAll(Arrays.asList("farmer", "warrior", "smithman"));
        }

        String partial = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.startsWith(partial))
                .collect(Collectors.toList());
    }

    private void handlePick(Player player, String[] args) {
        if (jobManager.hasJob(player.getUniqueId())) {
            player.sendMessage(Component.text("You already have a job! You cannot change it.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /job pick <farmer|warrior|smithman>", NamedTextColor.YELLOW));
            return;
        }

        JobType job = JobType.fromName(args[1]);
        if (job == null) {
            player.sendMessage(Component.text("Unknown job! Available: farmer, warrior, smithman", NamedTextColor.RED));
            return;
        }

        jobManager.assignJob(player.getUniqueId(), job);
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("You are now a ", NamedTextColor.GREEN)
                .append(job.getComponent())
                .append(Component.text("!", NamedTextColor.GREEN)));
        player.sendMessage(Component.text("You cannot change your job later.", NamedTextColor.GRAY));
    }

    private void handleInfo(Player player) {
        PlayerJobData data = jobManager.getPlayerJob(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Component.text("You don't have a job yet. Use /job pick <job>", NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Your Job: ", NamedTextColor.GOLD)
                .append(data.getJob().getComponent()));
        player.sendMessage(Component.text("Level: ", NamedTextColor.GOLD)
                .append(Component.text(data.getLevel(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("XP: ", NamedTextColor.GOLD)
                .append(Component.text(data.getXp() + "/" + data.getXpToNextLevel(), NamedTextColor.WHITE)));
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("--- Job System ---", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/job pick <job>", NamedTextColor.YELLOW)
                .append(Component.text(" - Choose your job (permanent)", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/job info", NamedTextColor.YELLOW)
                .append(Component.text(" - View your job & level", NamedTextColor.GRAY)));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Available jobs: ", NamedTextColor.GRAY)
                .append(Component.text("farmer, warrior, smithman", NamedTextColor.WHITE)));
    }
}
