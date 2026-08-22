package xyz.nothing.artaserver.job;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public enum JobType {
    FARMER("Farmer", Material.GOLDEN_HOE, NamedTextColor.GREEN),
    WARRIOR("Warrior", Material.IRON_SWORD, NamedTextColor.RED),
    SMITHMAN("Smithman", Material.ANVIL, NamedTextColor.AQUA);

    private final String displayName;
    private final Material icon;
    private final NamedTextColor color;

    JobType(String displayName, Material icon, NamedTextColor color) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public Component getComponent() {
        return Component.text(displayName, color);
    }

    public static JobType fromName(String name) {
        for (JobType job : values()) {
            if (job.name().equalsIgnoreCase(name) || job.displayName.equalsIgnoreCase(name)) {
                return job;
            }
        }
        return null;
    }
}
