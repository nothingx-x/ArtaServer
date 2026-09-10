package xyz.nothing.artaserver.listener;


import dev.aurelium.auraskills.api.event.skill.SkillLevelUpEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import xyz.nothing.artaserver.WebhookService;

public class AuraListener implements Listener {
    private final WebhookService webhookService;

    public AuraListener(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @EventHandler
    public void onSkillLevelUp(SkillLevelUpEvent event) {
        webhookService.notifyAurakills(event);
    }
}
