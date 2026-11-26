package boat.carpetorgaddition.wheel.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public interface PlayerDecomposedContainer extends Container {
    ServerPlayer getPlayer();

    Container getPlayerInventory();

    /**
     * 获取玩家物品栏主要存储部分
     */
    default Container getStorage() {
        return new SubInventory(this.getPlayerInventory(), 9, 36);
    }

    /**
     * 获取玩家物品栏快捷栏部分
     */
    default Container getHotbar() {
        return new SubInventory(this.getPlayerInventory(), 0, 9);
    }

    /**
     * 获取玩家物品栏存储和快捷栏部分
     */
    default Container getMain() {
        return new SubInventory(this.getPlayerInventory(), 0, 36);
    }

    /**
     * 获取玩家物品栏盔甲部分
     */
    default Container getArmor() {
        return new ReverseInventory(new SubInventory(this.getPlayerInventory(), 36, 40)) {
            @Override
            public void setItem(int slot, @NonNull ItemStack itemStack) {
                ItemStack oldStack = this.getItem(slot);
                super.setItem(slot, itemStack);
                ServerPlayer player = PlayerDecomposedContainer.this.getPlayer();
                EquipmentSlot equipment = switch (slot) {
                    case 0 -> EquipmentSlot.HEAD;
                    case 1 -> EquipmentSlot.CHEST;
                    case 2 -> EquipmentSlot.LEGS;
                    case 3 -> EquipmentSlot.FEET;
                    default -> null;
                };
                if (equipment != null) {
                    player.onEquipItem(equipment, oldStack, itemStack);
                }
            }
        };
    }

    /**
     * 获取玩家物品栏副手部分
     */
    default Container getOffHand() {
        return new SubInventory(this.getPlayerInventory(), 40, 41);
    }
}
