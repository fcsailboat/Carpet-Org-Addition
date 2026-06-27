package boat.carpetorgaddition.wheel.screen;

import boat.carpetorgaddition.wheel.DisabledSlot;
import boat.carpetorgaddition.wheel.inventory.AbstractCustomSizeInventory;
import boat.carpetorgaddition.wheel.inventory.VillagerInventory;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class VillagerScreenHandler extends AbstractContainerMenu implements UnavailableSlotSyncInterface {
    // 物品栏的大小
    private static final int SIZE = 8;
    private final Villager villagerEntity;
    private final VillagerInventory inventory;

    public VillagerScreenHandler(int syncId, Inventory playerInventory, Villager villagerEntity) {
        super(MenuType.GENERIC_3x3, syncId);
        this.villagerEntity = villagerEntity;
        this.inventory = new VillagerInventory(villagerEntity);
        this.inventory.startOpen(playerInventory.player);
        // 添加村民物品栏的槽位
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                if (i == 2 && j == 2) {
                    // 添加不可用的槽位
                    this.addSlot(new DisabledSlot(inventory, j + i * 3, 62 + j * 18, 17 + i * 18));
                    // 添加普通槽位
                } else {
                    this.addSlot(new Slot(inventory, j + i * 3, 62 + j * 18, 17 + i * 18));
                }
            }
        }
        // 添加玩家非快捷栏的物品栏槽位
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        // 添加快捷栏槽位
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack slotItemStack = slot.getItem();
            itemStack = slotItemStack.copy();
            // 判断当前点击的槽位是否是上方GUI的槽位而不是下方玩家物品栏的槽位
            // insertItem()方法的返回值是物品堆栈的堆叠数是否减少了
            if (slotIndex <= SIZE) {
                // 如果是GUI上半部分的槽位，将上方的物品移动到玩家物品栏的槽位中
                if (!this.moveItemStackTo(slotItemStack, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 如果是GUI下半部分的槽位，将玩家物品栏中的物品移动到上方槽位
                if (!this.moveItemStackTo(slotItemStack, 0, 8, false)) {
                    // 如果无法移动，检测槽位是不是非快捷栏的物品栏
                    if (slotIndex <= 35) {
                        // 如果物品栏槽位不是快捷栏，将物品移动到快捷栏
                        if (!this.moveItemStackTo(slotItemStack, 36, 45, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        // 如果是快捷栏，将物品移动到物品栏
                        if (!this.moveItemStackTo(slotItemStack, 9, 36, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
                if (slotItemStack.isEmpty()) {
                    slot.setByPlayer(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
                if (slotItemStack.getCount() == itemStack.getCount()) {
                    return ItemStack.EMPTY;
                }
                slot.onTake(player, slotItemStack);
            }
        }
        return itemStack;
    }

    // 村民死亡或距离玩家过远时，自动关闭GUI
    @Override
    public boolean stillValid(@NonNull Player player) {
        if (villagerEntity.isDeadOrDying() || villagerEntity.isRemoved()) {
            return false;
        }
        return player.distanceTo(villagerEntity) < 8;
    }

    // 是否可以向槽位中放入物品
    @Override
    public boolean canDragTo(@NonNull Slot slot) {
        // 槽位不能是禁用的槽位
        return !(slot instanceof DisabledSlot);
    }

    // 是否可以向槽位中放入物品
    @Override
    public boolean canTakeItemForPickAll(@NonNull ItemStack stack, @NonNull Slot slot) {
        return !(slot instanceof DisabledSlot);
    }

    @Override
    public void clicked(int slotIndex, int button, @NonNull ContainerInput input, @NonNull Player player) {
        if (slotIndex == 8) {
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public void removed(@NonNull Player player) {
        super.removed(player);
        this.inventory.dropExcess(player);
        AbstractCustomSizeInventory.PLACEHOLDER.setCount(1);
    }

    @Override
    public int from() {
        return 8;
    }

    @Override
    public int to() {
        return 8;
    }
}
