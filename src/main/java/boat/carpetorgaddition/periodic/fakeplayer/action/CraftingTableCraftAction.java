package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.PlayerActionCommand;
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
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.*;

public class CraftingTableCraftAction extends AbstractPlayerAction {
    /**
     * 合成配方
     */
    private final ItemStackPredicate[] predicates = new ItemStackPredicate[9];
    public static final LocalizationKey KEY = PlayerActionCommand.KEY.then("craft");

    public CraftingTableCraftAction(EntityPlayerMPFake fakePlayer, ItemStackPredicate[] predicates) {
        super(fakePlayer);
        if (predicates.length != 9) {
            throw new IllegalArgumentException();
        }
        System.arraycopy(predicates, 0, this.predicates, 0, this.predicates.length);
    }

    /**
     * 获取指定配方的输出物品
     *
     * @param predicates  合成配方
     * @param widthHeight 合成方格的宽高，工作台是3，物品栏是2
     * @param fakePlayer  合成该物品的假玩家
     * @return 如果能够合成物品，返回合成输出物品，否则返回空物品，如果配方中包含不能转换为物品的元素，也返回空物品
     */
    public static ItemStack getCraftOutput(ItemStackPredicate[] predicates, int widthHeight, EntityPlayerMPFake fakePlayer) {
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
    protected void tick() {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        if (fakePlayer.containerMenu instanceof CraftingMenu craftingScreenHandler) {
            AutoGrowInventory inventory = new AutoGrowInventory();
            this.craftingTableCraft(inventory, craftingScreenHandler);
            FakePlayerUtils.mergeEmptyShulkerBox(fakePlayer);
            // 丢弃合成输出
            for (ItemStack itemStack : inventory) {
                fakePlayer.drop(itemStack, false, true);
            }
        }
    }

    private void craftingTableCraft(AutoGrowInventory inventory, CraftingMenu screenHandler) {
        // 定义变量记录成功完成合成的次数
        int craftCount = 0;
        // 记录循环次数用来在游戏可能进入死循环时抛出异常
        int loopCount = 0;
        while (true) {
            // 检查循环次数，在循环次数过多时抛出异常
            loopCount++;
            if (loopCount > FakePlayerUtils.MAX_LOOP_COUNT) {
                throw new InfiniteLoopException();
            }
            // 定义变量记录找到正确合成材料的次数
            int successCount = 0;
            // 依次获取每一个合成材料和遍历合成格
            EntityPlayerMPFake fakePlayer = this.getFakePlayer();
            for (int index = 1; index <= 9; index++) {
                //依次获取每一个合成材料
                ItemStackPredicate predicate = this.predicates[index - 1];
                Slot slot = screenHandler.getSlot(index);
                // 如果合成格的指定槽位不是所需要合成材料，则丢出该物品
                if (slot.hasItem()) {
                    ItemStack itemStack = slot.getItem();
                    if (predicate.test(itemStack)) {
                        // 合成表格上已经有正确的合成材料，找到正确的合成材料次数自增
                        successCount++;
                    } else {
                        FakePlayerUtils.throwItem(screenHandler, index, fakePlayer);
                    }
                } else {
                    // 如果指定合成材料是空气，则不需要遍历物品栏，直接跳过该物品，并增加找到正确合成材料的次数
                    if (predicate.isEmpty()) {
                        successCount++;
                        continue;
                    }
                    // 遍历物品栏找到需要的合成材料
                    int size = screenHandler.slots.size();
                    for (int inventoryIndex = 10; inventoryIndex < size; inventoryIndex++) {
                        ItemStack itemStack = screenHandler.getSlot(inventoryIndex).getItem();
                        if (predicate.test(itemStack)) {
                            // 光标拾取和移动物品
                            if (FakePlayerUtils.withKeepPickupAndMoveItemStack(screenHandler,
                                    inventoryIndex, index, fakePlayer)) {
                                // 找到正确合成材料的次数自增
                                successCount++;
                                break;
                            }
                        } else if (CarpetOrgAdditionSettings.fakePlayerPickItemFromShulkerBox.value() && InventoryUtils.isShulkerBoxItem(itemStack)) {
                            ItemStack contentItemStack = InventoryUtils.tryPickItemFromStackedNonEmptyShulkerBox(fakePlayer, itemStack, predicate);
                            if (!contentItemStack.isEmpty()) {
                                // 丢弃光标上的物品（如果有）
                                FakePlayerUtils.dropCursorStack(screenHandler, fakePlayer);
                                // 将光标上的物品设置为从潜影盒中取出来的物品
                                screenHandler.setCarried(contentItemStack);
                                // 将光标上的物品放在合成方格的槽位上
                                FakePlayerUtils.pickupCursorStack(screenHandler, index, fakePlayer);
                                successCount++;
                                break;
                            }
                        }
                        // 合成格没有遍历完毕，继续查找下一个合成材料
                        // 合成格遍历完毕，并且物品栏找不到需要的合成材料，结束方法
                        if (index == 9 && inventoryIndex == size - 1) {
                            return;
                        }
                    }
                }
            }
            // 正确材料找到的次数等于9说明全部找到，可以合成
            if (successCount == 9) {
                // 工作台输出槽里有物品，说明配方正确并且前面的合成没有问题，可以取出合成的物品
                if (screenHandler.getSlot(0).hasItem()) {
                    FakePlayerUtils.collectItem(screenHandler, 0, inventory, fakePlayer);
                    // 合成成功，合成计数器自增
                    craftCount++;
                    // 避免在一个游戏刻内合成太多物品造成巨量卡顿
                    if (shouldStop(craftCount)) {
                        return;
                    }
                } else {
                    // 如果没有输出物品，说明之前的合成步骤有误，停止合成
                    this.stop();
                    MessageUtils.sendMessage(this.getServer(), KEY.then("error").translate(fakePlayer.getDisplayName(), this.getDisplayName()));
                    return;
                }
            } else {
                if (successCount > 9) {
                    // 找到正确合成材料的次数不应该大于合成槽位数量，如果超过了说明前面的操作出了问题，抛出异常结束方法
                    throw new IllegalStateException("Incorrect number of correct crafting materials found");
                }
                // 遍历完物品栏后，如果找到正确合成材料小于9，认为玩家身上没有足够的合成材料了，直接结束方法
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
        // 将可变文本“<玩家>正在合成物品，配方:”添加到集合
        ItemStack craftOutput = getCraftOutput(this.predicates, 3, this.getFakePlayer());
        // 如果可以合成物品，返回合成的结果物品，否则返回固定文本“物品”
        Component itemText = craftOutput.isEmpty() ? LocalizationKeys.Item.ITEM.translate() : ServerUtils.getName(craftOutput.getItem());
        Component displayName = this.getFakePlayer().getDisplayName();
        LocalizationKey key = this.getInfoLocalizationKey();
        joiner.newline(key.translate(displayName, itemText));
        joiner.enter(() -> this.addCraftRecipe(joiner, craftOutput));
        // 判断假玩家是否打开了一个工作台
        if (this.getFakePlayer().containerMenu instanceof CraftingMenu currentScreenHandler) {
            // 将可变文本“<玩家>当前合成物品的状态:”添加到集合中
            joiner.newline(key.then("state").translate(displayName));
            // 如果打开了，将每一个合成槽位（包括输出槽位）中的物品的名称和堆叠数组装成一个可变文本对象并添加到集合
            joiner.enter(() -> this.addCraftGridState(currentScreenHandler, joiner));
        } else {
            // 如果没有打开工作台，将未打开工作台的信息添加到集合
            joiner.newline(key.then("no_crafting_table").translate(displayName, ServerUtils.getName(Items.CRAFTING_TABLE)));
        }
        return joiner.collect();
    }

    private void addCraftRecipe(TextJoiner joiner, ItemStack craftOutput) {
        // 配方第一排
        joiner.newline()
                .append(this.predicates[0].getInitialUpperCase())
                .space()
                .append(this.predicates[1].getInitialUpperCase())
                .space()
                .append(this.predicates[2].getInitialUpperCase());
        // 配方第二排
        joiner.newline()
                .append(this.predicates[3].getInitialUpperCase())
                .space()
                .append(this.predicates[4].getInitialUpperCase())
                .space()
                .append(this.predicates[5].getInitialUpperCase());
        if (!craftOutput.isEmpty()) {
            joiner.append(" -> ").append(FakePlayerUtils.getWithCountHoverText(craftOutput));
        }
        // 配方第三排
        joiner.newline()
                .append(this.predicates[6].getInitialUpperCase())
                .space()
                .append(this.predicates[7].getInitialUpperCase())
                .space()
                .append(this.predicates[8].getInitialUpperCase());
    }

    // 添加当前合成方格的状态
    private void addCraftGridState(CraftingMenu screenHandler, TextJoiner joiner) {
        // 合成格第一排
        joiner.newline()
                .append(FakePlayerUtils.getWithCountHoverText(screenHandler.getSlot(1).getItem()))
                .space()
                .append(FakePlayerUtils.getWithCountHoverText(screenHandler.getSlot(2).getItem()))
                .space()
                .append(FakePlayerUtils.getWithCountHoverText(screenHandler.getSlot(3).getItem()));
        // 合成格第二排和输出槽
        joiner.newline()
                .append(FakePlayerUtils.getWithCountHoverText(screenHandler.getSlot(4).getItem()))
                .space()
                .append(FakePlayerUtils.getWithCountHoverText(screenHandler.getSlot(5).getItem()))
                .space()
                .append(FakePlayerUtils.getWithCountHoverText(screenHandler.getSlot(6).getItem()))
                .append(" -> ")
                .append(FakePlayerUtils.getWithCountHoverText(screenHandler.getSlot(0).getItem()));
        // 合成格第三排
        joiner.newline()
                .append(FakePlayerUtils.getWithCountHoverText(screenHandler.getSlot(7).getItem()))
                .space()
                .append(FakePlayerUtils.getWithCountHoverText(screenHandler.getSlot(8).getItem()))
                .space()
                .append(FakePlayerUtils.getWithCountHoverText(screenHandler.getSlot(9).getItem()));
    }

    /**
     * 是否应该因为合成次数过多而停止合成
     *
     * @param craftCount 当前合成次数
     * @return 是否应该停止
     */
    public static boolean shouldStop(int craftCount) {
        if (CarpetOrgAdditionSettings.fakePlayerMaxItemOperationCount.value() < 0) {
            return false;
        }
        return craftCount >= CarpetOrgAdditionSettings.fakePlayerMaxItemOperationCount.value();
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        for (int i = 0; i < predicates.length; i++) {
            json.addProperty(String.valueOf(i), predicates[i].toString());
        }
        return json;
    }

    @Override
    public Component getDisplayName() {
        return this.getLocalizationKey().then("crafting_table").translate();
    }

    @Override
    public LocalizationKey getLocalizationKey() {
        return KEY;
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.CRAFTING_TABLE_CRAFT;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CraftingTableCraftAction that = (CraftingTableCraftAction) o;
        return Objects.deepEquals(predicates, that.predicates);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(predicates);
    }
}
