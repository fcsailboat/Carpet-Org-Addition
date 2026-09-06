package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.wheel.MenuController;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class RenameAction extends AbstractPlayerAction {
    /**
     * 要进行重命名的物品
     */
    private final ItemStackPredicate predicate;
    /**
     * 物品的新名称
     */
    private final String name;
    private boolean notified = false;
    public static final String ITEM = "item";
    public static final String NAME = "name";
    private static final int FIRST_INPUT = 0;
    private static final int SECOND_INPUT = 1;
    private static final int OUTPUT = 2;
    public static final LocalizationKey KEY = PlayerActionCommand.KEY.then("rename");

    public RenameAction(EntityPlayerMPFake fakePlayer, ItemStackPredicate predicate, String name) {
        super(fakePlayer);
        this.predicate = predicate;
        this.name = name;
    }

    @Override
    protected void tick() {
        // 如果假玩家对铁砧持续按住右键，就会一直打开新的铁砧界面，同时旧的铁砧界面会自动关闭，关闭旧的铁砧界面时，铁砧内的物品会回到玩家物品栏
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        if (fakePlayer.containerMenu instanceof AnvilMenu menu) {
            // 如果假玩家没有足够的经验，直接结束方法，创造玩家给物品重命名不需要消耗经验
            if (fakePlayer.experienceLevel < 1 && !fakePlayer.hasInfiniteMaterials()) {
                if (this.notified) {
                    return;
                }
                LocalizationKey key = KEY.then("wait");
                MessageUtils.sendMessage(this.getServer(), key.translate(fakePlayer.getDisplayName(), this.getDisplayName()));
                this.notified = true;
                return;
            }
            MenuController<AnvilMenu> controller = new MenuController<>(menu, fakePlayer);
            this.rename(controller);
        }
    }

    private void rename(MenuController<AnvilMenu> controller) {
        Slot inputSlot = controller.getSlot(FIRST_INPUT);
        // 第一个槽位的物品是否正确：是指定物品，没有被正确重命名，已经最大堆叠
        boolean oneSlotCorrect = false;
        // 判断第一个槽位是否有物品
        if (inputSlot.hasItem()) {
            ItemStack itemStack = inputSlot.getItem();
            // 判断该槽位的物品是否已经正确重命名
            if ((this.predicate.test(itemStack)) && !Objects.equals(itemStack.getHoverName().getString(), this.name)) {
                oneSlotCorrect = InventoryUtils.isItemStackFull(inputSlot.getItem());
            } else {
                // 如果已经重命名，或者当前槽位不是指定物品，放回该槽位的物品
                // 因为该槽位的物品被丢弃，所以该槽位已经没有物品，没有必要继续判断，直接结束方法
                controller.moveSlotStackToInventory(FIRST_INPUT);
                return;
            }
        }
        if (oneSlotCorrect || this.switchItem(controller, inputSlot)) {
            // 获取铁砧第二个输入槽
            Slot secondInputSlot = controller.getSlot(SECOND_INPUT);
            if (secondInputSlot.hasItem()) {
                controller.moveSlotStackToInventory(SECOND_INPUT);
            }
            controller.getMenu().setItemName(this.name);
            Slot outputSlot = controller.getSlot(OUTPUT);
            // 让物品最大堆叠后才能重命名，节省经验
            if (outputSlot.hasItem() && this.canTakeOutput(controller) && InventoryUtils.isItemStackFull(inputSlot.getItem())) {
                controller.dropAllByPickup(OUTPUT);
            }
        }
    }

    private boolean switchItem(MenuController<AnvilMenu> controller, Slot inputSlot) {
        int size = controller.getSlots().size();
        IntList shulkerSlotIndex = new IntArrayList(size - 3);
        for (int index = 3; index < size; index++) {
            ItemStack itemStack = controller.getSlot(index).getItem();
            if (itemStack.isEmpty()) {
                continue;
            }
            boolean passed = this.predicate.test(itemStack);
            if (passed) {
                if (Objects.equals(itemStack.getHoverName().getString(), this.name)) {
                    controller.dropAll(index);
                    continue;
                }
            } else if (InventoryUtils.isShulkerBoxItem(itemStack)) {
                shulkerSlotIndex.add(index);
            }
            if (inputSlot.hasItem() ? InventoryUtils.canMerge(inputSlot.getItem(), itemStack) : passed) {
                if (controller.moveItemStack(index, FIRST_INPUT)) {
                    if (!controller.getCursorStack().isEmpty()) {
                        controller.leftClick(index);
                    }
                    return true;
                }
            }
        }
        Predicate<ItemStack> predicate = stack -> inputSlot.hasItem() ? InventoryUtils.canMerge(inputSlot.getItem(), stack) : this.predicate.test(stack);
        int count = inputSlot.hasItem() ? inputSlot.getItem().getMaxStackSize() - inputSlot.getItem().getCount() : -1;
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_SHULKER_BOX_ITEM_HANDLING.value()) {
            IntList stackedNonEmptyShulkerIndex = new IntArrayList(shulkerSlotIndex.size());
            // 优先从未堆叠的非空潜影盒中拿取物品
            for (int i = 0; i < shulkerSlotIndex.size(); i++) {
                int index = shulkerSlotIndex.getInt(i);
                ItemStack itemStack = controller.getSlotStack(index);
                if (InventoryUtils.containsShulkerStackable(itemStack, predicate) && itemStack.getCount() > 1) {
                    stackedNonEmptyShulkerIndex.add(index);
                } else if (InventoryUtils.isOperableSulkerBox(itemStack)) {
                    ItemStack content = InventoryUtils.pickItemFromShulkerBox(itemStack, predicate, count);
                    if (content.isEmpty()) {
                        continue;
                    }
                    this.moveItemToInputSlot(controller, content);
                    return true;
                }
            }
            // 从堆叠的潜影盒中拿取物品
            for (int i = 0; i < stackedNonEmptyShulkerIndex.size(); i++) {
                int index = stackedNonEmptyShulkerIndex.getInt(i);
                ItemStack itemStack = controller.getSlotStack(index);
                ItemStack content = InventoryUtils.tryPickItemFromStackedNonEmptyShulkerBox(controller.getFakePlayer(), itemStack, predicate, count);
                if (content.isEmpty()) {
                    continue;
                }
                this.moveItemToInputSlot(controller, content);
                return true;
            }
        }
        return false;
    }

    private void moveItemToInputSlot(MenuController<AnvilMenu> controller, ItemStack content) {
        controller.moveCursorStackToInventory();
        controller.setCursorStack(content);
        controller.leftClick(FIRST_INPUT);
    }

    // 判断是否可以输出物品
    private boolean canTakeOutput(MenuController<AnvilMenu> controller) {
        AnvilMenu menu = controller.getMenu();
        EntityPlayerMPFake fakePlayer = controller.getFakePlayer();
        return (fakePlayer.hasInfiniteMaterials() || fakePlayer.experienceLevel >= menu.getCost()) && menu.getCost() > 0;
    }

    @Override
    public void onFakePlayerLogout() {
        this.getFakePlayerNullable().ifPresent(PlayerUtils::closeScreen);
    }

    @Override
    public List<Component> info() {
        ArrayList<Component> list = new ArrayList<>();
        // 获取假玩家的显示名称
        Component playerName = getFakePlayer().getDisplayName();
        // 将假玩家要重命名的物品和物品新名称的信息添加到集合
        LocalizationKey key = this.getInfoLocalizationKey();
        list.add(key.translate(playerName, this.predicate.getDisplayName(), name));
        // 将假玩家剩余经验的信息添加到集合
        list.add(key.then("xp").translate(getFakePlayer().experienceLevel));
        if (getFakePlayer().containerMenu instanceof AnvilMenu anvilScreenHandler) {
            // 将铁砧GUI上的物品信息添加到集合
            list.add(TextBuilder.combineAll("    ",
                    AbstractPlayerAction.getWithCountHoverText(anvilScreenHandler.getSlot(0).getItem()), " ",
                    AbstractPlayerAction.getWithCountHoverText(anvilScreenHandler.getSlot(1).getItem()), " -> ",
                    AbstractPlayerAction.getWithCountHoverText(anvilScreenHandler.getSlot(2).getItem())));
        } else {
            // 将假玩家没有打开铁砧的信息添加到集合
            list.add(key.then("no_anvil").translate(playerName, Blocks.ANVIL.getName()));
        }
        return list;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(ITEM, this.predicate.toString());
        json.addProperty(NAME, this.name);
        return json;
    }

    @Override
    public LocalizationKey getLocalizationKey() {
        return KEY;
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.RENAME;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RenameAction that = (RenameAction) o;
        return notified == that.notified && Objects.equals(predicate, that.predicate) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, name, notified);
    }
}
