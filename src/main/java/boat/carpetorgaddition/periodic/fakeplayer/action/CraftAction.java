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
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public abstract class CraftAction extends AbstractPlayerAction {
    /**
     * 物品合成所使用的物品栏
     */
    protected final ItemStackPredicate[] predicates;

    public CraftAction(EntityPlayerMPFake fakePlayer, ItemStackPredicate[] predicates) {
        super(fakePlayer);
        int size = this.getCraftGridSize();
        if (predicates.length != size) {
            throw new IllegalArgumentException();
        }
        this.predicates = new ItemStackPredicate[size];
        System.arraycopy(predicates, 0, this.predicates, 0, this.predicates.length);
    }

    @Override
    protected void tick() {
        AutoGrowInventory inventory = new AutoGrowInventory();
        this.craft(inventory);
        FakePlayerUtils.mergeEmptyShulkerBox(this.getFakePlayer());
        // 丢弃合成输出
        for (ItemStack itemStack : inventory) {
            this.getFakePlayer().drop(itemStack, false, true);
        }
    }

    protected void craft(AutoGrowInventory inventory) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        AbstractContainerMenu screenHandler = this.getScreenHandler();
        if (screenHandler == null) {
            return;
        }
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
            int materialsCount = 0;
            // 遍历4x4合成格
            for (int craftGridIndex = this.getCraftGridStart(); craftGridIndex <= this.getCraftGridEnd(); craftGridIndex++) {
                // 获取每一个合成材料
                ItemStackPredicate matcher = this.predicates[craftGridIndex - 1];
                Slot slot = screenHandler.getSlot(craftGridIndex);
                // 检查合成格上是否已经有物品
                if (slot.hasItem()) {
                    // 如果有并且物品是正确的合成材料，直接结束本轮循环，即跳过该物品
                    if (matcher.test(slot.getItem())) {
                        materialsCount++;
                        continue;
                    } else {
                        // 如果不是，丢出该物品
                        FakePlayerUtils.throwItem(screenHandler, craftGridIndex, fakePlayer);
                    }
                } else if (matcher.isEmpty()) {
                    materialsCount++;
                    continue;
                }
                if (takeItemFromInventory(screenHandler, matcher, craftGridIndex, fakePlayer)) {
                    materialsCount++;
                }
            }
            // 找到了所有的合成材料，尝试输出物品
            if (materialsCount == this.getCraftGridSize()) {
                // 如果输出槽有物品，则丢出该物品
                if (screenHandler.getSlot(0).hasItem()) {
                    FakePlayerUtils.collectItem(screenHandler, 0, inventory, fakePlayer);
                    // 合成成功，合成计数器自增
                    craftCount++;
                    // 避免在一个游戏刻内合成太多物品造成巨量卡顿
                    if (this.shouldStop(craftCount)) {
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
                // 遍历完物品栏后，如果没有找到足够多的合成材料，认为玩家身上没有足够的合成材料了，直接结束方法
                return;
            }
        }
    }

    private boolean takeItemFromInventory(AbstractContainerMenu screenHandler, ItemStackPredicate matcher, int craftIndex, EntityPlayerMPFake fakePlayer) {
        final int start = this.getInventoryStart();
        final int end = this.getInventoryEnd();
        // 所有包含潜影盒物品的槽位索引
        IntList shulkerSlotIndex = new IntArrayList(end - start);
        // 遍历物品栏，包括盔甲槽和副手槽
        for (int index = start; index < end; index++) {
            ItemStack itemStack = screenHandler.getSlot(index).getItem();
            // 如果该槽位是正确的合成材料，将该物品移动到合成格，然后增加找到正确合成材料的次数
            if (matcher.test(itemStack)) {
                if (FakePlayerUtils.withKeepPickupAndMoveItemStack(screenHandler, index, craftIndex, fakePlayer)) {
                    return true;
                }
            } else if (InventoryUtils.isShulkerBoxItem(itemStack)) {
                shulkerSlotIndex.add(index);
            }
        }
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_SHULKER_BOX_ITEM_HANDLING.value()) {
            IntList stackedNonEmptyShulkerIndex = new IntArrayList(shulkerSlotIndex.size());
            // 优先从未堆叠的非空潜影盒中拿取物品
            for (int i = 0; i < shulkerSlotIndex.size(); i++) {
                int index = shulkerSlotIndex.getInt(i);
                ItemStack itemStack = screenHandler.getSlot(index).getItem();
                if (InventoryUtils.containsShulkerStackable(itemStack, matcher) && itemStack.getCount() > 1) {
                    stackedNonEmptyShulkerIndex.add(index);
                } else if (InventoryUtils.isOperableSulkerBox(itemStack)) {
                    ItemStack content = InventoryUtils.pickItemFromShulkerBox(itemStack, matcher);
                    if (moveItemToInputSlot(screenHandler, craftIndex, fakePlayer, content)) {
                        return true;
                    }
                }
            }
            // 从堆叠的潜影盒中拿取物品
            for (int i = 0; i < stackedNonEmptyShulkerIndex.size(); i++) {
                int index = stackedNonEmptyShulkerIndex.getInt(i);
                ItemStack itemStack = screenHandler.getSlot(index).getItem();
                ItemStack content = InventoryUtils.tryPickItemFromStackedNonEmptyShulkerBox(fakePlayer, itemStack, matcher);
                if (moveItemToInputSlot(screenHandler, craftIndex, fakePlayer, content)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean moveItemToInputSlot(AbstractContainerMenu screenHandler, int craftIndex, EntityPlayerMPFake fakePlayer, ItemStack content) {
        if (content.isEmpty()) {
            return false;
        }
        // 如果光标上的物品则丢弃
        FakePlayerUtils.dropCursorStack(screenHandler, fakePlayer);
        screenHandler.setCarried(content);
        FakePlayerUtils.pickupCursorStack(screenHandler, craftIndex, fakePlayer);
        return true;
    }

    /**
     * @return 合成方格的大小
     */
    protected abstract int getCraftGridSize();

    /**
     * @return 合成方格的起始索引
     */
    protected abstract int getCraftGridStart();

    /**
     * @return 合成方格的结束索引
     */
    protected abstract int getCraftGridEnd();

    /**
     * @return 物品栏（能存储物品的部分，包括盔甲槽、副手）的起始索引
     */
    protected abstract int getInventoryStart();

    /**
     * @return 物品栏的结束索引
     */
    protected abstract int getInventoryEnd();

    /**
     * @return 用于合成物品的屏幕界面
     */
    @Nullable
    protected abstract AbstractContainerMenu getScreenHandler();

    /**
     * 是否应该因为合成次数过多而停止合成
     *
     * @param craftCount 当前合成次数
     * @return 是否应该停止
     */
    private boolean shouldStop(int craftCount) {
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_MAX_ITEM_OPERATION_COUNT.value() < 0) {
            return false;
        }
        return craftCount >= CarpetOrgAdditionSettings.FAKE_PLAYER_MAX_ITEM_OPERATION_COUNT.value();
    }

    /**
     * 获取指定配方的输出物品
     *
     * @param predicates  合成配方
     * @param widthHeight 合成方格的宽高，工作台是3，物品栏是2
     * @param fakePlayer  合成该物品的假玩家
     * @return 如果能够合成物品，返回合成输出物品，否则返回空物品，如果配方中包含不能转换为物品的元素，也返回空物品
     */
    protected ItemStack getCraftOutput(ItemStackPredicate[] predicates, int widthHeight, EntityPlayerMPFake fakePlayer) {
        ArrayList<ItemStack> list = new ArrayList<>();
        for (ItemStackPredicate predicate : predicates) {
            Optional<Item> optional = predicate.getConvert();
            if (optional.isEmpty()) {
                // 存在非物品谓词，无法推断输出物品
                return ItemStack.EMPTY;
            }
            list.add(optional.get().getDefaultInstance());
        }
        CraftingInput input = CraftingInput.of(widthHeight, widthHeight, list);
        Level world = ServerUtils.getWorld(fakePlayer);
        Optional<RecipeHolder<CraftingRecipe>> optional = ServerUtils.getServer(fakePlayer).getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, world);
        return optional.map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
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
    protected LocalizationKey getLocalizationKey() {
        return CraftingTableCraftAction.KEY;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CraftAction that = (CraftAction) o;
        return Objects.deepEquals(predicates, that.predicates);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(predicates);
    }
}
