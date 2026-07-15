package boat.carpetorgaddition.wheel.inventory;

import boat.carpetorgaddition.mixin.accessor.carpet.EntityPlayerActionPackAccessor;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.screen.QuickShulkerScreenHandler;
import boat.carpetorgaddition.wheel.screen.WithButtonPlayerInventoryScreenHandler.ClickType;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import carpet.helpers.EntityPlayerActionPack;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntObjectBiConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

public class WithButtonPlayerInventory implements Container {
    /**
     * 玩家正在操作的物品栏
     */
    private final CombinedInventory inventory;
    private final ButtonInventory intervalAttack;
    private final ButtonInventory continuousAttack;
    private final ButtonInventory continuousUse;
    private final ButtonInventory hotbar;
    /**
     * 被打开物品栏的玩家
     */
    private final ServerPlayer player;
    private final EntityPlayerActionPack actionPack;
    private static final ItemStack ON_STACK;
    private static final ItemStack OFF_STACK;
    private static final Component ON_TEXT = LocalizationKeys.Button.ON.builder().setBold().setColor(ChatFormatting.GREEN).build();
    private static final Component OFF_TEXT = LocalizationKeys.Button.OFF.builder().setBold().setColor(ChatFormatting.RED).build();
    /**
     * 左键单击间隔
     */
    private static final int ATTACK_INTERVAL = 12;
    /**
     * 没有任何作用，仅为与{@code Gugle Carpet Addition}和一些物品整理模组兼容
     */
    private static final String GCA_CLEAR = "GcaClear";
    /**
     * 没有任何作用，仅表示按钮功能
     */
    private static final String BUTTON_ITEM = ServerUtils.ofIdentifier("button_item").toString();
    /**
     * 所有按钮的索引
     */
    public static final IntList BUTTON_INDEX_LIST = IntList.of(0, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18);
    private static final List<Consumer<ServerPlayer>> HOTBAR_RIGHT_CLICK_ACTION = List.of(
            player -> PlayerUtils.setSneaking(player, !PlayerUtils.isSneaking(player)),
            PlayerUtils::closeScreen
    );

    static {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(GCA_CLEAR, true);
        tag.putBoolean(BUTTON_ITEM, true);
        CustomData customData = CustomData.of(tag);
        // 苹果物品使用了结构空位和屏障的模型，即便物品被玩家通过某种方式取出来了，那也只是一个苹果
        ON_STACK = new ItemStack(Items.APPLE);
        OFF_STACK = new ItemStack(Items.APPLE);
        ON_STACK.set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(Items.BARRIER));
        OFF_STACK.set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(Items.STRUCTURE_VOID));
        ON_STACK.set(DataComponents.CUSTOM_DATA, customData);
        OFF_STACK.set(DataComponents.CUSTOM_DATA, customData);
    }

    public WithButtonPlayerInventory(ServerPlayer player) {
        this.player = player;
        PlayerDecomposedContainer decomposer = PlayerStorageInventory.of(player);
        SortableInventory sortable = new SortableInventory(List.of(decomposer.getStorage(), decomposer.getHotbar()), player, () -> 27 + player.getInventory().getSelectedSlot());
        this.actionPack = PlayerUtils.getActionPack(this.player);
        ButtonInventory stopAll = new StopButtonInventory(player, _ -> {
            ItemStack itemStack = OFF_STACK.copy();
            Component first = LocalizationKeys.Button.Action.Stop.LEFT.builder()
                    .setItalic(false)
                    .setColor(ChatFormatting.WHITE)
                    .setBold()
                    .build();
            Component second = LocalizationKeys.Button.Action.Stop.RIGHT.builder()
                    .setGrayItalic()
                    .build();
            itemStack.set(DataComponents.CUSTOM_NAME, first);
            itemStack.set(DataComponents.LORE, new ItemLore(List.of(second)));
            return Map.entry(itemStack, itemStack);
        }, (_, pack) -> pack.stopAll(), sortable::sort);
        this.intervalAttack = new ButtonInventory(player, _ -> {
            ItemStack on = ON_STACK.copy();
            ItemStack off = OFF_STACK.copy();
            LocalizationKey key = LocalizationKeys.Button.Action.Attack.INTERVAL;
            on.set(DataComponents.CUSTOM_NAME, key.builder(ATTACK_INTERVAL, ON_TEXT).setItalic(false).setColor(ChatFormatting.WHITE).setBold().build());
            off.set(DataComponents.CUSTOM_NAME, key.builder(ATTACK_INTERVAL, OFF_TEXT).setItalic(false).setColor(ChatFormatting.WHITE).setBold().build());
            return Map.entry(on, off);
        }, Map.entry(
                (_, pack) -> pack.start(EntityPlayerActionPack.ActionType.ATTACK, EntityPlayerActionPack.Action.interval(ATTACK_INTERVAL)),
                (_, pack) -> pack.start(EntityPlayerActionPack.ActionType.ATTACK, EntityPlayerActionPack.Action.once())
        ), null);
        this.continuousAttack = new ButtonInventory(player, _ -> {
            ItemStack on = ON_STACK.copy();
            ItemStack off = OFF_STACK.copy();
            Component second = LocalizationKeys.Button.Action.Attack.CONTINUOUS_RIGHT.builder().setGrayItalic().build();
            ItemLore itemLore = new ItemLore(List.of(second));
            LocalizationKey key = LocalizationKeys.Button.Action.Attack.CONTINUOUS;
            on.set(DataComponents.CUSTOM_NAME, key.builder(ON_TEXT).setItalic(false).setColor(ChatFormatting.WHITE).setBold().build());
            off.set(DataComponents.CUSTOM_NAME, key.builder(OFF_TEXT).setItalic(false).setColor(ChatFormatting.WHITE).setBold().build());
            on.set(DataComponents.LORE, itemLore);
            off.set(DataComponents.LORE, itemLore);
            return Map.entry(on, off);
        }, Map.entry(
                (_, pack) -> pack.start(EntityPlayerActionPack.ActionType.ATTACK, EntityPlayerActionPack.Action.continuous()),
                (_, pack) -> pack.start(EntityPlayerActionPack.ActionType.ATTACK, EntityPlayerActionPack.Action.once())
        ), () -> PlayerUtils.attack(player));
        this.continuousUse = new ButtonInventory(player, _ -> {
            ItemStack on = ON_STACK.copy();
            ItemStack off = OFF_STACK.copy();
            LocalizationKey key = LocalizationKeys.Button.Action.Use.CONTINUOUS;
            Component second = LocalizationKeys.Button.Action.Use.CONTINUOUS_RIGHT.builder().setGrayItalic().build();
            ItemLore itemLore = new ItemLore(List.of(second));
            on.set(DataComponents.CUSTOM_NAME, key.builder(ON_TEXT).setItalic(false).setColor(ChatFormatting.WHITE).setBold().build());
            off.set(DataComponents.CUSTOM_NAME, key.builder(OFF_TEXT).setItalic(false).setColor(ChatFormatting.WHITE).setBold().build());
            on.set(DataComponents.LORE, itemLore);
            off.set(DataComponents.LORE, itemLore);
            return Map.entry(on, off);
        }, Map.entry(
                (_, pack) -> pack.start(EntityPlayerActionPack.ActionType.USE, EntityPlayerActionPack.Action.continuous()),
                (_, pack) -> pack.start(EntityPlayerActionPack.ActionType.USE, EntityPlayerActionPack.Action.once())
        ), () -> PlayerUtils.use(player));
        this.hotbar = new HotbarButtonInventory(player, index -> {
            ItemStack off = OFF_STACK.copy();
            int ordinal = index + 1;
            off.setCount(ordinal);
            off.set(DataComponents.CUSTOM_NAME, LocalizationKeys.Button.HOTBAR.builder(ordinal).setItalic(false).setColor(ChatFormatting.WHITE).setBold().build());
            ItemStack on = ON_STACK.copy();
            on.setCount(ordinal);
            on.set(DataComponents.CUSTOM_NAME, LocalizationKeys.Button.HOTBAR.builder(ordinal).setItalic(false).setColor(ChatFormatting.WHITE).setBold().build());
            if (HOTBAR_RIGHT_CLICK_ACTION.size() > index) {
                Component second = LocalizationKeys.Button.Action.Hotbar.RIGHT.then(String.valueOf(ordinal)).builder().setGrayItalic().build();
                ItemLore itemLore = new ItemLore(List.of(second));
                on.set(DataComponents.LORE, itemLore);
                off.set(DataComponents.LORE, itemLore);
            }
            return Map.entry(on, off);
        }, (index, pack) -> pack.setSlot(index + 1), index -> HOTBAR_RIGHT_CLICK_ACTION.get(index).accept(player));
        stopAll.addMutualExclusion(this.intervalAttack);
        stopAll.addMutualExclusion(this.continuousAttack);
        stopAll.addMutualExclusion(this.continuousUse);
        this.intervalAttack.addMutualExclusion(this.continuousAttack);
        this.continuousAttack.addMutualExclusion(this.intervalAttack);
        ArrayList<Container> list = new ArrayList<>();
        list.add(stopAll);
        list.add(decomposer.getArmor());
        list.add(this.intervalAttack);
        list.add(this.continuousAttack);
        list.add(decomposer.getOffHand());
        list.add(this.continuousUse);
        list.add(this.hotbar);
        list.add(decomposer.getStorage());
        list.add(decomposer.getHotbar());
        this.inventory = new CombinedInventory(list);
        this.updateButton();
    }

    private void updateButton() {
        EntityPlayerActionPackAccessor accessor = (EntityPlayerActionPackAccessor) this.actionPack;
        Map<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action> actions = accessor.getActions();
        EntityPlayerActionPack.Action attack = actions.get(EntityPlayerActionPack.ActionType.ATTACK);
        if (attack == null) {
            this.intervalAttack.setState(0, false);
            this.continuousAttack.setState(0, false);
        } else {
            this.intervalAttack.setState(0, attack.interval == ATTACK_INTERVAL);
            this.continuousAttack.setState(0, ((EntityPlayerActionPackAccessor.ActionAccessor) attack).isContinuous());
        }
        EntityPlayerActionPack.Action use = actions.get(EntityPlayerActionPack.ActionType.USE);
        if (use == null) {
            this.continuousUse.setState(0, false);
        } else {
            this.continuousUse.setState(0, ((EntityPlayerActionPackAccessor.ActionAccessor) use).isContinuous());
        }
        int slot = this.player.getInventory().getSelectedSlot();
        this.hotbar.setState(slot, true);
    }

    public void tick() {
        this.updateButton();
    }

    @Override
    public int getContainerSize() {
        return this.inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty();
    }

    @NonNull
    @Override
    public ItemStack getItem(int slot) {
        return this.inventory.getItem(slot);
    }

    @NonNull
    @Override
    public ItemStack removeItem(int slot, int count) {
        return this.inventory.removeItem(slot, count);
    }

    @NonNull
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.inventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack itemStack) {
        this.inventory.setItem(slot, itemStack);
    }

    @Override
    public void setChanged() {
        this.inventory.setChanged();
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return !this.player.isDeadOrDying() && !this.player.isRemoved();
    }

    @Override
    public void clearContent() {
        this.inventory.clearContent();
    }

    public Container getSubInventory(int slotIndex) {
        return this.inventory.getSubInventory(slotIndex);
    }

    public ButtonInventory getHotbar() {
        return this.hotbar;
    }

    public EntityPlayerActionPack getActionPack() {
        return this.actionPack;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    public static class SortableInventory extends CombinedInventory implements SortableContainer {
        private final ServerPlayer player;
        private final IntSupplier mainHandSlotIndex;

        /**
         * @param mainHandSlotIndex 主手槽位的索引，用于在整理物品时忽略主手物品
         */
        public SortableInventory(List<Container> containers, ServerPlayer player, IntSupplier mainHandSlotIndex) {
            super(containers);
            this.player = player;
            this.mainHandSlotIndex = mainHandSlotIndex;
        }

        @Override
        public boolean isValidSlot(int index) {
            if (index == this.mainHandSlotIndex.getAsInt()) {
                return false;
            }
            ItemStack itemStack = this.getItem(index);
            if (QuickShulkerScreenHandler.isOpenedShulkerBox(this.player, itemStack)) {
                return false;
            }
            return SortableContainer.super.isValidSlot(index);
        }
    }

    public static class ButtonInventory extends SimpleContainer {
        protected final Map.Entry<IntObjectBiConsumer<EntityPlayerActionPack>, IntObjectBiConsumer<EntityPlayerActionPack>> consumer;
        protected final ServerPlayer player;
        private final List<ItemStack> buttonOn;
        private final List<ItemStack> buttonOff;
        @Nullable
        private final IntConsumer rightClickEvent;
        protected final List<ButtonInventory> mutualExclusion = new ArrayList<>();

        protected ButtonInventory(int size, ServerPlayer player, IntFunction<Map.Entry<ItemStack, ItemStack>> function, IntObjectBiConsumer<EntityPlayerActionPack> consumer, IntConsumer rightClickEvent) {
            Map.Entry<IntObjectBiConsumer<EntityPlayerActionPack>, IntObjectBiConsumer<EntityPlayerActionPack>> entry = Map.entry(consumer, consumer);
            this(size, player, function, entry, rightClickEvent);
        }

        public ButtonInventory(
                ServerPlayer player,
                IntFunction<Map.Entry<ItemStack, ItemStack>> function,
                Map.Entry<IntObjectBiConsumer<EntityPlayerActionPack>, IntObjectBiConsumer<EntityPlayerActionPack>> consumer,
                @Nullable Runnable rightClickEvent
        ) {
            this(1, player, function, consumer, rightClickEvent == null ? null : _ -> rightClickEvent.run());
        }

        private ButtonInventory(
                int size,
                ServerPlayer player,
                IntFunction<Map.Entry<ItemStack, ItemStack>> function,
                Map.Entry<IntObjectBiConsumer<EntityPlayerActionPack>, IntObjectBiConsumer<EntityPlayerActionPack>> consumer,
                @Nullable IntConsumer rightClickEvent
        ) {
            ArrayList<ItemStack> on = new ArrayList<>(size);
            ArrayList<ItemStack> off = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                Map.Entry<ItemStack, ItemStack> entry = function.apply(i);
                on.add(entry.getKey());
                off.add(entry.getValue());
            }
            this.player = player;
            this.buttonOn = List.copyOf(on);
            this.buttonOff = List.copyOf(off);
            super(off.toArray(ItemStack[]::new));
            this.consumer = consumer;
            this.rightClickEvent = rightClickEvent;
        }

        public final void onClicked(ClickType clickType, int index, EntityPlayerActionPack actionPack) {
            if (this.rightClicked(clickType, index)) {
                return;
            }
            this.onClicked(index, actionPack);
        }

        protected boolean rightClicked(ClickType clickType, int index) {
            if (clickType == ClickType.RIGHT_CLICK && this.rightClickEvent != null) {
                this.rightClickEvent.accept(index);
                return true;
            }
            return false;
        }

        protected void onClicked(int index, EntityPlayerActionPack actionPack) {
            ItemStack current = this.getItem(index);
            if (current == this.buttonOff.get(index)) {
                this.consumer.getKey().accept(index, actionPack);
                this.setState(index, true);
            } else {
                this.consumer.getValue().accept(index, actionPack);
                this.setState(index, false);
            }
            for (ButtonInventory inventory : this.mutualExclusion) {
                inventory.setState(0, false);
            }
        }

        public void setState(int index, boolean state) {
            this.setItem(index, state ? this.buttonOn.get(index) : this.buttonOff.get(index));
        }

        public void addMutualExclusion(ButtonInventory inventory) {
            this.mutualExclusion.add(inventory);
        }
    }

    public static class StopButtonInventory extends ButtonInventory {
        public StopButtonInventory(ServerPlayer player, IntFunction<Map.Entry<ItemStack, ItemStack>> function, IntObjectBiConsumer<EntityPlayerActionPack> consumer, Runnable rightClickEvent) {
            super(1, player, function, consumer, _ -> rightClickEvent.run());
        }

        @Override
        public void onClicked(int index, EntityPlayerActionPack actionPack) {
            this.consumer.getKey().accept(index, actionPack);
            for (ButtonInventory inventory : this.mutualExclusion) {
                inventory.setState(0, false);
            }
        }
    }

    public static class HotbarButtonInventory extends ButtonInventory {
        public HotbarButtonInventory(ServerPlayer player, IntFunction<Map.Entry<ItemStack, ItemStack>> function, IntObjectBiConsumer<EntityPlayerActionPack> consumer, IntConsumer rightClickEvent) {
            super(9, player, function, consumer, rightClickEvent);
        }

        @Override
        protected boolean rightClicked(ClickType clickType, int index) {
            return HOTBAR_RIGHT_CLICK_ACTION.size() > index && super.rightClicked(clickType, index);
        }

        @Override
        public void onClicked(int index, EntityPlayerActionPack actionPack) {
            this.setState(index, true);
            this.consumer.getValue().accept(index, actionPack);
        }

        @Override
        public void setState(int index, boolean state) {
            for (int i = 0; i < this.getContainerSize(); i++) {
                super.setState(i, i == index);
            }
        }
    }
}
