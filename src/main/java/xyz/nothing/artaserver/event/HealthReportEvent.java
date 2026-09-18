package xyz.nothing.artaserver.event;


import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import xyz.nothing.artaserver.HealthReportService;

public class HealthReportEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final HealthReportService.HealthReport healthReport;

    public HealthReportEvent(HealthReportService.HealthReport healthReport) {
        this.healthReport = healthReport;
    }

    public HealthReportService.HealthReport getHealthReport() {
        return healthReport;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
