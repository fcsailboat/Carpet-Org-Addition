package boat.carpetorgaddition.periodic.task.search;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.FinderCommand;
import boat.carpetorgaddition.exception.TaskExecutionException;
import boat.carpetorgaddition.mixin.accessor.AbstractHorseEntityAccessor;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.task.ServerTask;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.ItemStackStatistics;
import boat.carpetorgaddition.wheel.inventory.ImmutableInventory;
import boat.carpetorgaddition.wheel.page.PageManager;
import boat.carpetorgaddition.wheel.page.PagedCollection;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.traverser.BlockEntityTraverser;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class ItemSearchTask extends ServerTask {
    private final Level world;
    private final BlockEntityTraverser blockEntities;
    private Iterator<Entity> entitySearchIterator;
    private Iterator<BlockEntity> blockEntitySearchIterator;
    private FindState findState;
    private int count;
    private boolean shulkerBox;
    private final ItemStackPredicate predicate;
    private final ArrayList<Result> results = new ArrayList<>();
    private final PagedCollection pagedCollection;
    public static final LocalizationKey KEY = FinderCommand.KEY.then("item");

    public ItemSearchTask(Level world, ItemStackPredicate predicate, BlockEntityTraverser blockEntities, CommandSourceStack source) {
        super(source);
        this.world = world;
        this.blockEntities = blockEntities;
        this.findState = FindState.BLOCK;
        this.predicate = predicate;
        MinecraftServer server = source.getServer();
        PageManager pageManager = ServerComponentCoordinator.getCoordinator(server).getPageManager();
        this.pagedCollection = pageManager.newPagedCollection(this.source);
    }

    @Override
    public void tick() {
        this.checkTimeout();
        while (this.isTimeRemaining()) {
            try {
                switch (this.findState) {
                    case BLOCK -> searchFromContainer();
                    case ENTITY -> searchFromEntity();
                    case SORT -> sort();
                    case FEEDBACK -> feedback();
                    default -> {
                        return;
                    }
                }
            } catch (TaskExecutionException e) {
                e.disposal();
                this.findState = FindState.END;
                return;
            }
        }
    }

    // 从容器查找
    private void searchFromContainer() {
        if (this.blockEntitySearchIterator == null) {
            this.blockEntitySearchIterator = blockEntities.iterator();
        }
        while (this.blockEntitySearchIterator.hasNext()) {
            if (this.isTimeExpired()) {
                return;
            }
            BlockEntity blockEntity = this.blockEntitySearchIterator.next();
            BlockPos blockPos = blockEntity.getBlockPos();
            if (blockEntity instanceof Container inventory) {
                // 获取容器名称
                Component containerName
                        = inventory instanceof BaseContainerBlockEntity lockableContainer
                        ? lockableContainer.getName()
                        : this.world.getBlockState(blockPos).getBlock().getName();
                this.count(inventory, blockPos, containerName);
            }
        }
        this.findState = FindState.ENTITY;
    }

    // 从实体查找
    private void searchFromEntity() {
        AABB box = blockEntities.toBox();
        if (this.entitySearchIterator == null) {
            this.entitySearchIterator = this.world.getEntitiesOfClass(Entity.class, box).iterator();
        }
        while (this.entitySearchIterator.hasNext()) {
            if (this.isTimeExpired()) {
                return;
            }
            Entity entity = this.entitySearchIterator.next();
            switch (entity) {
                // 掉落物
                case ItemEntity itemEntity -> {
                    Component drops = LocalizationKeys.Item.DROPS.translate();
                    this.count(new SimpleContainer(itemEntity.getItem()), itemEntity.blockPosition(), drops);
                }
                // 假玩家
                case EntityPlayerMPFake fakePlayer -> {
                    Inventory inventory = fakePlayer.getInventory();
                    this.count(inventory, fakePlayer.blockPosition(), fakePlayer.getName());
                }
                // 容器实体
                case ContainerEntity inventory -> this.count(inventory, entity.blockPosition(), entity.getName());
                // 物品展示框
                case ItemFrame itemFrame -> {
                    ItemStack itemStack = itemFrame.getItem();
                    if (!itemStack.isEmpty()) {
                        ImmutableInventory inventory = new ImmutableInventory(itemStack);
                        this.count(inventory, itemFrame.blockPosition(), itemFrame.getName());
                    }
                }
                // 村民
                case Villager villager when CarpetOrgAdditionSettings.OPEN_VILLAGER_INVENTORY.value() -> {
                    SimpleContainer inventory = villager.getInventory();
                    this.count(inventory, villager.blockPosition(), villager.getName());
                }
                // 驴，骡和羊驼
                case AbstractChestedHorse donkey when donkey.hasChest() -> {
                    ArrayList<ItemStack> list = new ArrayList<>();
                    AbstractHorseEntityAccessor accessor = (AbstractHorseEntityAccessor) donkey;
                    SimpleContainer inventory = accessor.getDonkeyInventory();
                    for (int i = 0; i < inventory.getContainerSize(); i++) {
                        list.add(inventory.getItem(i));
                    }
                    this.count(new ImmutableInventory(list), donkey.blockPosition(), donkey.getName());
                }
                // 悦灵
                case Allay allay -> {
                    ImmutableInventory inventory = new ImmutableInventory(allay.getMainHandItem());
                    this.count(inventory, allay.blockPosition(), allay.getName());
                }
                default -> {
                }
            }
        }
        this.findState = FindState.SORT;
    }

    // 统计符合条件的物品
    private void count(Container inventory, BlockPos blockPos, Component containerName) {
        // 是否有物品是在潜影盒中找到的
        ItemStackStatistics statistics = new ItemStackStatistics(this.predicate);
        statistics.statistics(inventory);
        if (statistics.getSum() == 0) {
            return;
        }
        this.count += statistics.getSum();
        if (statistics.hasNestingItem()) {
            this.shulkerBox = true;
        }
        this.results.add(new Result(blockPos, containerName, statistics));
        if (this.results.size() > FinderCommand.MAXIMUM_STATISTICAL_COUNT) {
            // 容器太多，无法统计
            Runnable function = () -> MessageUtils.sendErrorMessage(
                    source,
                    KEY.then("too_much").translate(this.predicate.getDisplayName())
            );
            throw new TaskExecutionException(function);
        }
    }

    // 对结果进行排序
    private void sort() {
        if (this.results.isEmpty()) {
            // 在周围的容器中找不到指定物品，直接将状态设置为结束，然后结束方法
            MessageUtils.sendMessage(this.source, KEY.then("cannot_find").translate(predicate.getDisplayName()));
            this.findState = FindState.END;
            return;
        }
        this.results.sort((o1, o2) -> o2.statistics().getSum() - o1.statistics().getSum());
        this.findState = FindState.FEEDBACK;
    }

    // 发送命令反馈
    private void feedback() {
        Component itemCount;
        Optional<Item> optional = predicate.getConvert();
        if (optional.isPresent()) {
            // 为数量添加鼠标悬停效果
            itemCount = FinderCommand.showCount(optional.get().getDefaultInstance(), this.count, this.shulkerBox);
        } else {
            TextBuilder builder = TextBuilder.of(this.count);
            if (this.shulkerBox) {
                builder.setItalic();
            }
            itemCount = builder.build();
        }
        int size = this.results.size();
        MessageUtils.sendEmptyMessage(this.source);
        MessageUtils.sendMessage(this.source, KEY.then("head").translate(size, itemCount, predicate.getDisplayName()));
        this.pagedCollection.addContent(this.results);
        CommandUtils.handlingException(this.pagedCollection::print, this.source);
        this.findState = FindState.END;
    }

    @Override
    public boolean stopped() {
        return this.findState == FindState.END;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.source.getPlayer());
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass() == obj.getClass()) {
            return Objects.equals(this.source.getPlayer(), ((ItemSearchTask) obj).source.getPlayer());
        }
        return false;
    }

    @Override
    public long getMaxExecutionTime() {
        return FinderCommand.MAX_SEARCH_TIME;
    }

    @Override
    protected long getMaxTimeSlice() {
        return FinderCommand.TIME_SLICE;
    }

    public record Result(
            BlockPos blockPos,
            Component containerName,
            ItemStackStatistics statistics
    ) implements Supplier<Component> {
        @Override
        public Component get() {
            Component pos = TextProvider.blockPos(blockPos, ChatFormatting.GREEN);
            return KEY.then("each").translate(pos, containerName, statistics.getCountText());
        }
    }

    public enum FindState {
        BLOCK,
        ENTITY,
        SORT,
        FEEDBACK,
        END
    }
}
