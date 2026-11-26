package boat.carpetorgaddition.wheel.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * 反转物品栏
 */
@NullMarked
public class ReverseInventory implements Container {
    private final Container container;

    public ReverseInventory(Container container) {
        this.container = container;
    }

    @Override
    public int getContainerSize() {
        return this.container.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return this.container.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.container.getItem(getIndexMapping(slot));
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return this.container.removeItem(getIndexMapping(slot), count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.container.removeItemNoUpdate(getIndexMapping(slot));
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        this.container.setItem(getIndexMapping(slot), itemStack);
    }

    private int getIndexMapping(int slot) {
        return this.getContainerSize() - slot - 1;
    }

    @Override
    public void setChanged() {
        this.container.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void clearContent() {
        this.container.clearContent();
    }
}
