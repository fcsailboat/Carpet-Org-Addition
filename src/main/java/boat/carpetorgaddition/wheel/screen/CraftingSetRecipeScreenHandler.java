package boat.carpetorgaddition.wheel.screen;

import boat.carpetorgaddition.periodic.FakePlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.fakeplayer.action.CraftingTableCraftAction;
import boat.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import boat.carpetorgaddition.periodic.fakeplayer.action.InventoryCraftAction;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

public class CraftingSetRecipeScreenHandler extends CraftingMenu implements UnavailableSlotSyncInterface {
    /**
     * 一个假玩家对象，类中所有操作都是围绕这个假玩家进行的
     */
    private final EntityPlayerMPFake fakePlayer;

    public CraftingSetRecipeScreenHandler(
            int syncId,
            Inventory playerInventory,
            EntityPlayerMPFake fakePlayer,
            ContainerLevelAccess screenHandlerContext) {
        super(syncId, playerInventory, screenHandlerContext);
        this.fakePlayer = fakePlayer;
    }

    // 阻止玩家取出输出槽位的物品
    @Override
    public void clicked(int slotIndex, int button, @NonNull ContainerInput input, @NonNull Player player) {
        if (slotIndex == 0) {
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    // 关闭GUI时，设置假玩家的合成动作和配方
    @Override
    public void removed(@NonNull Player player) {
        //如果没有给假玩家指定合成配方，结束方法
        if (craftSlots.isEmpty()) {
            return;
        }
        //修改假玩家的3x3合成配方
        Item[] items = new Item[9];
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            items[i] = craftSlots.getItem(i).getItem();
        }
        FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.of(this.fakePlayer);
        // 设置假玩家合成动作
        setCraftAction(items, coordinator.getFakePlayerActionManager());
        // 关闭GUI后，使用父类的方法让物品回到玩家背包
        super.removed(player);
    }

    // 设置假玩家合成动作
    private void setCraftAction(Item[] items, FakePlayerActionManager actionManager) {
        // 如果能在2x2合成格中合成，优先使用2x2
        if (canInventoryCraft(items, 0, 1, 2, 5, 8)) {
            actionManager.setAction(createData(items, 3, 4, 6, 7));
        } else if (canInventoryCraft(items, 0, 3, 6, 7, 8)) {
            actionManager.setAction(createData(items, 1, 2, 4, 5));
        } else if (canInventoryCraft(items, 2, 5, 6, 7, 8)) {
            actionManager.setAction(createData(items, 0, 1, 3, 4));
        } else if (canInventoryCraft(items, 0, 1, 2, 3, 6)) {
            actionManager.setAction(createData(items, 4, 5, 7, 8));
        } else {
            //将假玩家动作设置为3x3合成
            ItemStackPredicate[] predicates = new ItemStackPredicate[9];
            for (int i = 0; i < predicates.length; i++) {
                predicates[i] = new ItemStackPredicate(items[i]);
            }
            actionManager.setAction(new CraftingTableCraftAction(this.fakePlayer, predicates));
        }
    }

    // 可以在物品栏合成
    private boolean canInventoryCraft(Item[] items, int... indices) {
        for (int index : indices) {
            if (items[index] == Items.AIR) {
                continue;
            }
            return false;
        }
        return true;
    }

    // 创建合成数据
    private InventoryCraftAction createData(Item[] items, int... indices) {
        ItemStackPredicate[] predicates = new ItemStackPredicate[4];
        // 这里的index并不是indices里保存的元素
        for (int index = 0; index < 4; index++) {
            predicates[index] = new ItemStackPredicate(items[indices[index]]);
        }
        return new InventoryCraftAction(this.fakePlayer, predicates);
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return true;
    }

    @Override
    public int from() {
        return 0;
    }

    @Override
    public int to() {
        return 0;
    }
}
