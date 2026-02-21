package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.exception.InfiniteLoopException;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.inventory.AutoGrowInventory;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class InventoryCraftAction extends AbstractPlayerAction {
    /**
     * 物品合成所使用的物品栏
     */
    private final ItemStackPredicate[] predicates = new ItemStackPredicate[4];

    public InventoryCraftAction(EntityPlayerMPFake fakePlayer, ItemStackPredicate[] predicates) {
        super(fakePlayer);
        if (predicates.length != 4) {
            throw new IllegalArgumentException();
        }
        System.arraycopy(predicates, 0, this.predicates, 0, this.predicates.length);
    }

    @Override
    protected void tick() {
        AutoGrowInventory inventory = new AutoGrowInventory();
        this.inventoryCraft(inventory);
        FakePlayerUtils.mergeEmptyShulkerBox(this.getFakePlayer());
        // 丢弃合成输出
        for (ItemStack itemStack : inventory) {
            this.getFakePlayer().drop(itemStack, false, true);
        }
    }

    private void inventoryCraft(AutoGrowInventory inventory) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        InventoryMenu playerScreenHandler = fakePlayer.inventoryMenu;
        // 定义变量记录成功完成合成的次数
        int craftCount = 0;
        // 记录循环次数用来在游戏可能进入死循环时抛出异常
        int loopCount = 0;
        while (true) {
            // 检查循环次数
            loopCount++;
            if (loopCount > FakePlayerUtils.MAX_LOOP_COUNT) {
                throw new InfiniteLoopException();
            }
            // 定义变量记录找到正确合成材料的次数
            int successCount = 0;
            // 遍历4x4合成格
            for (int craftIndex = 1; craftIndex <= 4; craftIndex++) {
                // 获取每一个合成材料
                ItemStackPredicate matcher = this.predicates[craftIndex - 1];
                Slot slot = playerScreenHandler.getSlot(craftIndex);
                // 检查合成格上是否已经有物品
                if (slot.hasItem()) {
                    // 如果有并且物品是正确的合成材料，直接结束本轮循环，即跳过该物品
                    if (matcher.test(slot.getItem())) {
                        successCount++;
                        continue;
                    } else {
                        // 如果不是，丢出该物品
                        FakePlayerUtils.throwItem(playerScreenHandler, craftIndex, fakePlayer);
                    }
                } else if (matcher.isEmpty()) {
                    successCount++;
                    continue;
                }
                int size = playerScreenHandler.slots.size();
                // 遍历物品栏，包括盔甲槽和副手槽
                for (int inventoryIndex = 5; inventoryIndex < size; inventoryIndex++) {
                    ItemStack itemStack = playerScreenHandler.getSlot(inventoryIndex).getItem();
                    // 如果该槽位是正确的合成材料，将该物品移动到合成格，然后增加找到正确合成材料的次数
                    if (matcher.test(itemStack)) {
                        if (FakePlayerUtils.withKeepPickupAndMoveItemStack(playerScreenHandler,
                                inventoryIndex, craftIndex, fakePlayer)) {
                            successCount++;
                            break;
                        }
                    } else if (CarpetOrgAdditionSettings.fakePlayerPickItemFromShulkerBox.value() && InventoryUtils.isShulkerBoxItem(itemStack)) {
                        ItemStack contentItemStack = InventoryUtils.tryPickItemFromStackedNonEmptyShulkerBox(fakePlayer, itemStack, matcher);
                        if (!contentItemStack.isEmpty()) {
                            // 丢弃光标上的物品（如果有）
                            FakePlayerUtils.dropCursorStack(playerScreenHandler, fakePlayer);
                            // 将光标上的物品设置为从潜影盒中取出来的物品
                            playerScreenHandler.setCarried(contentItemStack);
                            // 将光标上的物品放在合成方格的槽位上
                            FakePlayerUtils.pickupCursorStack(playerScreenHandler, craftIndex, fakePlayer);
                            successCount++;
                            break;
                        }
                    }
                    // 如果遍历完物品栏还没有找到指定物品，认为玩家身上已经没有该物品，结束方法
                    if (craftIndex == 4 && inventoryIndex == size - 1) {
                        return;
                    }
                }
            }
            // 如果找到正确合成材料的次数为4，认为找到了所有的合成材料，尝试输出物品
            if (successCount == 4) {
                // 如果输出槽有物品，则丢出该物品
                if (playerScreenHandler.getSlot(0).hasItem()) {
                    FakePlayerUtils.collectItem(playerScreenHandler, 0, inventory, fakePlayer);
                    // 合成成功，合成计数器自增
                    craftCount++;
                    // 避免在一个游戏刻内合成太多物品造成巨量卡顿
                    if (CraftingTableCraftAction.shouldStop(craftCount)) {
                        return;
                    }
                } else {
                    // 如果输出槽没有物品，认为前面的合成操作有误，停止合成
                    this.stop();
                    LocalizationKey key = this.getLocalizationKey();
                    MessageUtils.sendMessage(this.getServer(), key.then("error").translate(fakePlayer.getDisplayName(), this.getDisplayName()));
                    return;
                }
            } else {
                if (successCount > 4) {
                    // 找到正确合成材料的次数不应该大于合成槽位数量，如果超过了说明前面的操作出了问题，抛出异常结束方法
                    throw new IllegalStateException("Incorrect number of correct crafting materials found");
                }
                // 遍历完物品栏后，如果没有找到足够多的合成材料，认为玩家身上没有足够的合成材料了，直接结束方法
                return;
            }
        }
    }

    @Override
    public List<Component> info() {
        // 创建一个集合用来存储可变文本对象，这个集合用来在聊天栏输出多行聊天信息，集合中的每个元素单独占一行
        TextJoiner joiner = new TextJoiner();
        joiner.unsetBullet();
        joiner.setIndent(4);
        // 获取假玩家的显示名称
        Component name = this.getFakePlayer().getDisplayName();
        // 将可变文本“<玩家>正在合成物品，配方:”添加到集合
        ItemStack craftOutput = CraftingTableCraftAction.getCraftOutput(this.predicates, 2, this.getFakePlayer());
        // 如果可以合成物品，返回合成的结果物品，否则返回固定文本“物品”
        Component itemText = craftOutput.isEmpty() ? LocalizationKeys.Item.ITEM.translate() : ServerUtils.getName(craftOutput.getItem());
        LocalizationKey key = this.getInfoLocalizationKey();
        joiner.newline(key.translate(name, itemText));
        joiner.enter(() -> this.addCraftRecipe(joiner, craftOutput));
        // 将可变文本“<玩家>当前合成物品的状态:”添加到集合中
        joiner.newline(key.then("state").translate(name));
        // 获取玩家的生存模式物品栏对象
        InventoryMenu playerScreenHandler = this.getFakePlayer().inventoryMenu;
        // 将每一个合成槽位（包括输出槽位）中的物品的名称和堆叠数组装成一个可变文本对象并添加到集合
        joiner.enter(() -> this.addCraftGridState(joiner, playerScreenHandler));
        return joiner.collect();
    }

    // 添加合成配方文本
    private void addCraftRecipe(TextJoiner joiner, ItemStack craftOutput) {
        // 配方第一排
        joiner.newline()
                .append(this.predicates[0].getInitialUpperCase())
                .space()
                .append(this.predicates[1].getInitialUpperCase());
        // 配方第二排
        joiner.newline()
                .append(this.predicates[2].getInitialUpperCase())
                .space()
                .append(this.predicates[3].getInitialUpperCase());
        if (!craftOutput.isEmpty()) {
            joiner.append(" -> ").append(FakePlayerUtils.getWithCountHoverText(craftOutput));
        }
    }

    // 合成方格内的物品状态
    private void addCraftGridState(TextJoiner joiner, InventoryMenu screenHandler) {
        // 合成格第一排
        joiner.newline()
                .append(FakePlayerUtils.getWithCountHoverText(screenHandler.getSlot(1).getItem()))
                .space()
                .append(FakePlayerUtils.getWithCountHoverText(screenHandler.getSlot(2).getItem()));
        // 合成格第二排和输出槽
        joiner.newline()
                .append(FakePlayerUtils.getWithCountHoverText(screenHandler.getSlot(3).getItem()))
                .space()
                .append(FakePlayerUtils.getWithCountHoverText(screenHandler.getSlot(4).getItem()))
                .append(" -> ")
                .append(FakePlayerUtils.getWithCountHoverText(screenHandler.getSlot(0).getItem()));
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        for (int i = 0; i < this.predicates.length; i++) {
            json.addProperty(String.valueOf(i), this.predicates[i].toString());
        }
        return json;
    }

    @Override
    public Component getDisplayName() {
        return this.getLocalizationKey().then("inventory").translate();
    }

    @Override
    protected LocalizationKey getLocalizationKey() {
        return CraftingTableCraftAction.KEY;
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.INVENTORY_CRAFT;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InventoryCraftAction that = (InventoryCraftAction) o;
        return Objects.deepEquals(predicates, that.predicates);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(predicates);
    }
}
