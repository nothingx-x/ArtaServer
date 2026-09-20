package xyz.nothing.artaserver.listener;


import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import xyz.nothing.artaserver.event.ArtaBoxResultEvent;
import xyz.nothing.artaserver.service.WebhookService;

public class ArtaBoxListener implements Listener {
    private final WebhookService webhookService;

    public ArtaBoxListener(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @EventHandler
    public void onArtaBoxResult(ArtaBoxResultEvent event) {
        Player player = event.getPlayer();
        Material item = event.getItem();

        player.getInventory()
                .addItem(new ItemStack(item))
                .forEach((slot, leftover) -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));

        // The builder is handed straight to sendMessage, which takes a ComponentLike: calling
        // .build() here would tie the plugin to the Adventure 4 signature of that method,
        // which no longer exists on Adventure 5 servers.
        player.sendMessage(Component.text()
                .append(Component.text("شما از "))
                .append(Component.text(event.getArtaBox().getDisplayName()))
                .append(Component.text(" دریافت کردید: "))
                .append(Component.translatable(item.translationKey())));

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        webhookService.notifyArtaBox(event);
    }
}
