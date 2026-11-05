package boat.carpetorgaddition.wheel;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

/**
 * 不能向其中拿取和放置物品的槽位
 */
public class DisabledSlot extends Slot {
    public DisabledSlot(Container inventory, int index, int x, int y, Section section) {
        super(inventory, index, x, y, section);
    }

    // 设置不能向槽位中放入物品
    @Override
    public boolean mayPlace(@NonNull ItemStack stack) {
        return false;
    }

    // 设置不能向槽位中拿取物品
    @Override
    public boolean mayPickup(@NonNull Player player) {
        return false;
    }

    @Override
    public boolean allowModification(@NonNull Player player) {
        return false;
    }
}
