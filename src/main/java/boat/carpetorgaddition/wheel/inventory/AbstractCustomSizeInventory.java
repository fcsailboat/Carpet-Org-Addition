package boat.carpetorgaddition.wheel.inventory;

import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NullMarked;

import java.util.function.Supplier;

@NullMarked
public abstract class AbstractCustomSizeInventory implements Container {
    /**
     * 用来占位的物品
     */
    public static final ItemStack PLACEHOLDER;

    static {
        ItemStack itemStack = new ItemStack(Items.STAINED_GLASS_PANE.red());
        TextBuilder builder = TextBuilder.of(LocalizationKeys.Item.PLACEHOLDER.translate());
        builder.setColor(ChatFormatting.RED);
        itemStack.set(DataComponents.CUSTOM_NAME, builder.build());
        PLACEHOLDER = itemStack;
    }

    /**
     * 占位符物品栏
     *
     * @apiNote 使用 {@code Supplier} 包装，推迟变量的初始化到构造方法之后
     */
    private final Supplier<NonNullList<ItemStack>> stacks = () -> NonNullList.withSize(this.getContainerSize() - this.getActualSize(), PLACEHOLDER);

    /**
     * @return 物品栏的实际大小，超出此大小的索引都是在GUI中用来占位的，没有实际用途
     */
    protected int getActualSize() {
        return this.getInventory().getContainerSize();
    }

    /**
     * @return 实际可用的物品栏
     */
    protected abstract Container getInventory();

    @Override
    public boolean isEmpty() {
        return this.getInventory().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < this.getActualSize()) {
            return this.getInventory().getItem(slot);
        }
        return stacks.get().get(this.getAmendSlotIndex(slot));
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < this.getActualSize()) {
            return this.getInventory().removeItem(slot, amount);
        }
        ItemStack itemStack = ContainerHelper.removeItem(this.stacks.get(), getAmendSlotIndex(slot), amount);
        if (!itemStack.isEmpty()) {
            this.setChanged();
        }
        return itemStack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < this.getActualSize()) {
            return this.getInventory().removeItemNoUpdate(slot);
        }
        ItemStack itemStack = this.stacks.get().get(getAmendSlotIndex(slot));
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        this.stacks.get().set(getAmendSlotIndex(slot), ItemStack.EMPTY);
        return itemStack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < this.getActualSize()) {
            this.getInventory().setItem(slot, stack);
            return;
        }
        this.stacks.get().set(getAmendSlotIndex(slot), stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public void setChanged() {
        this.getInventory().setChanged();
    }

    @Override
    public void clearContent() {
        // 只清空物品栏即可，不需要清空用来占位的stacks
        this.getInventory().clearContent();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < this.getActualSize();
    }

    // 丢弃多余的槽位中的物品
    public void dropExcess(Player player) {
        for (ItemStack itemStack : stacks.get()) {
            // 不丢弃占位用的物品
            if (itemStack == PLACEHOLDER) {
                continue;
            }
            player.drop(itemStack, false, false);
        }
    }

    private int getAmendSlotIndex(int slotIndex) {
        return slotIndex - this.getActualSize();
    }
}
