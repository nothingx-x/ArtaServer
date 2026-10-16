package xyz.nothing.artaserver.event;


import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import xyz.nothing.artaserver.service.ArtaBoxService;

public class ArtaBoxResultEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final ArtaBoxService.ArtaBox artaBox;
    private final Material item;

    public ArtaBoxResultEvent(Player player, ArtaBoxService.ArtaBox artaBox, Material item) {
        super(player);
        this.artaBox = artaBox;
        this.item = item;
    }

    public ArtaBoxService.ArtaBox getArtaBox() {
        return artaBox;
    }

    public Material getItem() {
        return item;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
