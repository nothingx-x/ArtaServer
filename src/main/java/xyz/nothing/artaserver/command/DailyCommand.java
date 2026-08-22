package xyz.nothing.artaserver.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import xyz.nothing.artaserver.DailyManager;

public class DailyCommand implements CommandExecutor {
    private final DailyManager dailyManager;

    public DailyCommand(DailyManager dailyManager) {
        this.dailyManager = dailyManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        dailyManager.claimDaily(player);
        return true;
    }
}
