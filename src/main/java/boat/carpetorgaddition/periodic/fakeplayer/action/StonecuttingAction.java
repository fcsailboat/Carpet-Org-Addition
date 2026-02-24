package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.exception.InfiniteLoopException;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.inventory.AutoGrowInventory;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StonecuttingAction extends AbstractPlayerAction {
    /**
     * 要使用切石机切制的物品
     */
    private final Item item;
    /**
     * 切石机内按钮的索引
     */
    private final int button;
    public static final String ITEM = "item";
    public static final String BUTTON = "button";
    public static final LocalizationKey KEY = PlayerActionCommand.KEY.then("stonecutting");

    // TODO 支持物品谓词
    public StonecuttingAction(EntityPlayerMPFake fakePlayer, Item item, int button) {
        super(fakePlayer);
        this.item = item;
        this.button = button;
    }

    @Override
    protected void tick() {
        /*
         * （注释内容可能不适用于新版本）
         * 切石机的输出槽不能使用Ctrl+Q一次性丢出整组物品，只能一个个丢出。在合成
         * 物品时，会有大量物品产生，因为是一个个丢出的，所以物品不会立即合并。例如，
         * 一组石头合成石砖时，不会生成一整组的石砖，而是生成64个未堆叠的石砖，合成时，
         * 会因为物品实体过多，导致巨量卡顿，甚至游戏卡死。因此，这里加一个物品栏用来
         * 临时保存合成输出的物品，在物品栏内完成物品的合并，并在本tick的合成结束时
         * 一次性丢出物品栏内的所有物品，这样丢出的物品就是已经合并的，并显著减少卡顿
         */
        AutoGrowInventory inventory = new AutoGrowInventory();
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        // 合成物品
        this.stonecutting(inventory);
        FakePlayerUtils.mergeEmptyShulkerBox(fakePlayer);
        // 丢弃合成输出
        for (ItemStack itemStack : inventory) {
            fakePlayer.drop(itemStack, false, true);
        }
    }

    private void stonecutting(AutoGrowInventory inventory) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        if (fakePlayer.containerMenu instanceof StonecutterMenu stonecutterMenu) {
            // 合成物品的此处
            int craftCount = 0;
            // 循环次数
            int loopCount = 0;
            while (true) {
                loopCount++;
                if (loopCount > 1000) {
                    throw new InfiniteLoopException();
                }
                boolean hasMaterials = false;
                Slot inputSlot = stonecutterMenu.getSlot(0);
                if (inputSlot.hasItem()) {
                    ItemStack itemStack = inputSlot.getItem();
                    if (itemStack.is(this.item)) {
                        hasMaterials = true;
                    } else {
                        FakePlayerUtils.throwItem(stonecutterMenu, 0, fakePlayer);
                    }
                }
                // 如果输入槽没有材料，尝试从物品栏中获取合成材料
                if (hasMaterials || this.takeItemFromInventory(stonecutterMenu)) {
                    // 模拟单击切石机按钮
                    stonecutterMenu.clickMenuButton(fakePlayer, this.button);
                    Slot outputSlot = stonecutterMenu.getSlot(1);
                    if (outputSlot.hasItem()) {
                        FakePlayerUtils.collectItem(stonecutterMenu, 1, inventory, fakePlayer);
                        craftCount++;
                        // 限制每个游戏刻合成次数
                        int maxCount = CarpetOrgAdditionSettings.fakePlayerMaxItemOperationCount.value();
                        if (maxCount > 0 && craftCount >= maxCount) {
                            return;
                        }
                    } else {
                        // 切石机未输出物品，可能是配方指定有误
                        this.stop();
                        MinecraftServer server = ServerUtils.getServer(fakePlayer);
                        MessageUtils.sendMessage(server, KEY.then("error").translate(fakePlayer.getDisplayName(), this.getDisplayName()));
                        return;
                    }
                } else {
                    return;
                }
            }
        }
    }

    /**
     * 将物品移动到切石机输入槽位
     *
     * @return 物品栏是否有材料
     */
    private boolean takeItemFromInventory(StonecutterMenu screenHandler) {
        int start = 2;
        int end = screenHandler.slots.size();
        IntList shulkerSlotIndex = new IntArrayList(end - start);
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        for (int index = start; index < end; index++) {
            ItemStack itemStack = screenHandler.getSlot(index).getItem();
            if (itemStack.is(this.item)) {
                if (FakePlayerUtils.withKeepPickupAndMoveItemStack(screenHandler, index, 0, fakePlayer)) {
                    return true;
                }
            } else if (InventoryUtils.isShulkerBoxItem(itemStack)) {
                if (itemStack.getCount() == 1 && InventoryUtils.isEmptyShulkerBox(itemStack)) {
                    continue;
                }
                shulkerSlotIndex.add(index);
            }
        }
        if (CarpetOrgAdditionSettings.fakePlayerPickItemFromShulkerBox.value()) {
            IntList stackedNonEmptyShulkerIndex = new IntArrayList(shulkerSlotIndex.size());
            for (int i = 0; i < shulkerSlotIndex.size(); i++) {
                int index = shulkerSlotIndex.getInt(i);
                ItemStack itemStack = screenHandler.getSlot(index).getItem();
                if (InventoryUtils.containsShulkerStackable(itemStack, stack -> stack.is(this.item)) && itemStack.getCount() > 1) {
                    stackedNonEmptyShulkerIndex.add(index);
                } else if (InventoryUtils.isOperableSulkerBox(itemStack)) {
                    // 从潜影盒中查找指定物品
                    ItemStack content = InventoryUtils.pickItemFromShulkerBox(itemStack, stack -> stack.is(this.item));
                    if (content.isEmpty()) {
                        continue;
                    }
                    this.moveItemToInputSlot(screenHandler, content, fakePlayer);
                    return true;
                }
            }
            for (int i = 0; i < stackedNonEmptyShulkerIndex.size(); i++) {
                int index = stackedNonEmptyShulkerIndex.getInt(i);
                ItemStack itemStack = screenHandler.getSlot(index).getItem();
                // 仅在合成结束时合并一次空潜影盒，可能导致在合并潜影盒之前，空潜影盒把空槽位占满，进而导致无法从堆叠的非空潜影盒中取物，但不考虑这种情况
                ItemStack content = InventoryUtils.tryPickItemFromStackedNonEmptyShulkerBox(fakePlayer, itemStack, stack -> stack.is(this.item));
                if (content.isEmpty()) {
                    continue;
                }
                this.moveItemToInputSlot(screenHandler, content, fakePlayer);
                return true;
            }
        }
        // 物品栏中没有该物品
        return false;
    }

    // 将物品移动到切石机输入槽
    private void moveItemToInputSlot(StonecutterMenu screenHandler, ItemStack itemStack, EntityPlayerMPFake fakePlayer) {
        // 丢弃光标上的物品（如果有）
        FakePlayerUtils.dropCursorStack(screenHandler, fakePlayer);
        // 将光标上的物品设置为从潜影盒中取出来的物品
        screenHandler.setCarried(itemStack);
        // 将光标上的物品放在切石机输入槽位上
        FakePlayerUtils.pickupCursorStack(screenHandler, 0, fakePlayer);
    }

    @Override
    public List<Component> info() {
        // 创建一个物品栏对象用来获取配方的输出物品
        SingleRecipeInput input = new SingleRecipeInput(this.item.getDefaultInstance());
        // 获取假玩家所在的世界对象
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        Level world = ServerUtils.getWorld(fakePlayer);
        ItemStack output;
        try {
            // 获取与配方对应的物品
            output = this.getRecipeResult(fakePlayer, world, input);
        } catch (IndexOutOfBoundsException e) {
            // 如果索引越界了，将输出物品设置为空
            output = ItemStack.EMPTY;
        }
        // 获取输出物品的名称
        ArrayList<Component> list = new ArrayList<>();
        LocalizationKey key = this.getInfoLocalizationKey();
        list.add(key.translate(
                        fakePlayer.getDisplayName(),
                        ServerUtils.getName(Items.STONECUTTER),
                        this.item.getDefaultInstance().getDisplayName(),
                        output.getDisplayName()
                )
        );
        if (fakePlayer.containerMenu instanceof StonecutterMenu stonecutterScreenHandler) {
            // 将按钮索引的信息添加到集合，按钮在之前减去了1，这里再加回来
            list.add(key.then("button").translate(this.button + 1));
            // 将切石机当前输入输出槽位的状态
            list.add(TextBuilder.combineAll("    ",
                    FakePlayerUtils.getWithCountHoverText(stonecutterScreenHandler.getSlot(0).getItem()), " -> ",
                    FakePlayerUtils.getWithCountHoverText(stonecutterScreenHandler.getSlot(1).getItem())));
        } else {
            // 假玩家没有打开切石机
            list.add(key.then("no_stonecutter").translate(fakePlayer.getDisplayName(), ServerUtils.getName(Items.STONECUTTER)));
        }
        return list;
    }

    // 获取切石机配方输出
    private ItemStack getRecipeResult(EntityPlayerMPFake fakePlayer, Level world, SingleRecipeInput input) {
        // TODO 配方结果错误
        for (SelectableRecipe.SingleInputEntry<StonecutterRecipe> entry : world.recipeAccess().stonecutterRecipes().entries()) {
            Optional<RecipeHolder<StonecutterRecipe>> optional = entry.recipe().recipe();
            if (optional.isEmpty()) {
                continue;
            }
            StonecutterRecipe recipe = optional.get().value();
            if (recipe.matches(input, fakePlayer.level())) {
                return recipe.assemble(input);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(ITEM, BuiltInRegistries.ITEM.getKey(this.item).toString());
        json.addProperty(BUTTON, this.button);
        return json;
    }

    @Override
    public LocalizationKey getLocalizationKey() {
        return KEY;
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.STONECUTTING;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StonecuttingAction that = (StonecuttingAction) o;
        return button == that.button && Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, button);
    }
}
