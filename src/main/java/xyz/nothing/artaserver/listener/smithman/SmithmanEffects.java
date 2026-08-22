package xyz.nothing.artaserver.listener.smithman;

import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import xyz.nothing.artaserver.job.JobConstants;
import xyz.nothing.artaserver.job.JobType;
import xyz.nothing.artaserver.job.annotation.OnJob;

import java.util.Iterator;

import static xyz.nothing.artaserver.job.JobConstants.ORES;
public class SmithmanEffects {

    private static final double ORE_DROP_MULTIPLIER = 1.5;

    @OnJob(JobType.SMITHMAN)
    public void onSmithmanOreDrop(BlockDropItemEvent event) {
        if (!ORES.contains(event.getBlockState().getType())) return;

        for (org.bukkit.entity.Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();
            int newAmount = (int) Math.ceil(stack.getAmount() * ORE_DROP_MULTIPLIER);
            stack.setAmount(newAmount);
        }
    }

    @OnJob(JobType.SMITHMAN)
    public void onSmithmanCropDrop(BlockDropItemEvent event) {
        if (!JobConstants.CROPS.contains(event.getBlockState().getType())) return;

        Iterator<org.bukkit.entity.Item> it = event.getItems().iterator();
        while (it.hasNext()) {
            org.bukkit.entity.Item item = it.next();
            ItemStack stack = item.getItemStack();
            int amount = stack.getAmount();

            if (amount <= 1) {
                if (Math.random() > 0.2) {
                    it.remove();
                }
            } else {
                int keep = (int) Math.ceil(amount * 0.2);
                if (keep < 1 && Math.random() <= 0.2) {
                    keep = 1;
                }
                if (keep < 1) {
                    it.remove();
                } else {
                    stack.setAmount(keep);
                }
            }
        }
    }
}
