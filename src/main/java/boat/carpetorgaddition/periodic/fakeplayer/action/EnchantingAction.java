package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.inventory.PlayerStorageInventory;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.traverser.EntityTraverser;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class EnchantingAction extends AbstractPlayerAction {
    private final ItemStackPredicate predicate;
    private final Holder.Reference<Enchantment> enchantment;
    private boolean notified = false;
    private static final int FIRST_INPUT = 0;
    private static final int SECOND_INPUT = 1;
    private static final int OUTPUT = 2;
    private static final LocalizationKey KEY = PlayerActionCommand.KEY.then("enchanting");

    public EnchantingAction(@Nullable EntityPlayerMPFake fakePlayer, ItemStackPredicate predicate, Holder.Reference<Enchantment> enchantment) {
        super(fakePlayer);
        this.predicate = predicate;
        this.enchantment = enchantment;
    }

    @Override
    protected void tick() {
        if (this.predicate.isEmpty()) {
            return;
        }
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        AbstractContainerMenu screen = PlayerUtils.getCurrentScreen(fakePlayer);
        MinecraftServer server = ServerUtils.getServer(fakePlayer);
        long tick = ServerUtils.getCurrentGameTick(server);
        if (screen instanceof AnvilMenu menu) {
            this.enchanting(menu, server, fakePlayer);
            if (tick % 30 == 0L) {
                PlayerStorageInventory.of(fakePlayer).mergeEmptyShulkerBox();
            }
        } else if (tick % 10 == 0L) {
            ServerLevel world = ServerUtils.getWorld(fakePlayer);
            PlayerUtils.getHitResult(fakePlayer)
                    .filter(hitResult -> hitResult instanceof BlockHitResult)
                    .map(hitResult -> (BlockHitResult) hitResult)
                    .map(BlockHitResult::getBlockPos)
                    .filter(blockPos -> world.getBlockState(blockPos).getBlock() instanceof AnvilBlock)
                    .filter(blockPos -> world.getBlockState(blockPos).is(BlockTags.ANVIL))
                    .filter(blockPos -> new EntityTraverser<>(world, blockPos, blockPos.above(3), FallingBlockEntity.class).isEmpty())
                    .ifPresent(_ -> PlayerUtils.use(fakePlayer));
        }
    }

    private void enchanting(AnvilMenu menu, MinecraftServer server, EntityPlayerMPFake fakePlayer) {
        int count = 0;
        int maxCount = CarpetOrgAdditionSettings.FAKE_PLAYER_MAX_ITEM_OPERATION_COUNT.value();
        do {
            count++;
            if (this.switchItem(menu)) {
                ItemStack itemStack = menu.getSlot(OUTPUT).getItem();
                LocalizationKey key = this.getLocalizationKey();
                if (itemStack.isEmpty()) {
                    Component message = key.then("no_output").translate(this.getFakePlayer().getDisplayName(), this.getDisplayName());
                    MessageUtils.sendMessage(server, message);
                    this.stop();
                    return;
                } else if (itemStack.is(Items.ENCHANTED_BOOK) ? EnchantmentUtils.hasBookEnchantment(itemStack, this.enchantment) : EnchantmentUtils.hasEnchantment(itemStack, this.enchantment)) {
                    // TODO 测试过于昂贵
                    if (menu.getCost() >= 40 && !fakePlayer.hasInfiniteMaterials()) {
                        Component message = key.then("expensive").translate(this.getFakePlayer().getDisplayName(), this.getDisplayName());
                        MessageUtils.sendMessage(server, message);
                        this.stop();
                    } else if (this.hasExperience(menu)) {
                        FakePlayerUtils.throwItem(menu, OUTPUT, fakePlayer);
                    } else {
                        if (this.notified) {
                            return;
                        }
                        Component message = key
                                .then("lack_of_experience")
                                .translate(this.getFakePlayer().getDisplayName(), this.getDisplayName());
                        MessageUtils.sendMessage(server, message);
                        this.notified = true;
                    }
                } else {
                    Component message = key.then("unable_to_enchant").translate(this.getFakePlayer().getDisplayName(), this.getDisplayName());
                    MessageUtils.sendMessage(server, message);
                    this.stop();
                    return;
                }
            } else {
                return;
            }
        } while ((maxCount == -1 || count < maxCount) && menu.access.evaluate((world, blockPos) -> world.getBlockState(blockPos).is(BlockTags.ANVIL), true));
    }

    protected boolean hasExperience(AnvilMenu menu) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        return (fakePlayer.hasInfiniteMaterials() || fakePlayer.experienceLevel >= menu.getCost()) && menu.getCost() > 0;
    }

    private boolean switchItem(AnvilMenu menu) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        PlayerStorageInventory inventory = PlayerStorageInventory.of(fakePlayer);
        int count = 0;
        ItemStack first = menu.getSlot(FIRST_INPUT).getItem();
        if (first.isEmpty()) {
            if (this.switchItem(menu, inventory, FIRST_INPUT, this::canInput)) {
                count++;
            }
        } else if (first.getCount() == 1 && this.canInput(first)) {
            count++;
        } else {
            this.recyclingItem(menu, FIRST_INPUT, inventory);
        }
        ItemStack second = menu.getSlot(SECOND_INPUT).getItem();
        if (second.isEmpty()) {
            if (this.switchItem(menu, inventory, SECOND_INPUT, stack -> EnchantmentUtils.hasBookEnchantment(stack, this.enchantment))) {
                count++;
            }
        } else if (EnchantmentUtils.hasBookEnchantment(second, this.enchantment)) {
            count++;
        } else {
            this.recyclingItem(menu, SECOND_INPUT, inventory);
        }
        return count == 2;
    }

    private void recyclingItem(AnvilMenu menu, int slotIndex, PlayerStorageInventory inventory) {
        ItemStack itemStack = menu.getSlot(slotIndex).getItem().copyAndClear();
        inventory.insertWithInventoryPriority(itemStack);
    }

    private boolean canInput(ItemStack itemStack) {
        if (EnchantmentHelper.canStoreEnchantments(itemStack)
            && this.predicate.test(itemStack)
            && (this.enchantment.value().canEnchant(itemStack)
                || this.getFakePlayer().hasInfiniteMaterials()
                || itemStack.is(Items.ENCHANTED_BOOK))) {
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : EnchantmentHelper.getEnchantmentsForCrafting(itemStack).entrySet()) {
                Holder<Enchantment> holder = entry.getKey();
                if (holder.equals(this.enchantment) || !Enchantment.areCompatible(holder, this.enchantment)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean switchItem(AnvilMenu menu, PlayerStorageInventory inventory, int intputSlotIndex, Predicate<ItemStack> predicate) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        for (ItemStack itemStack : inventory) {
            if (itemStack.isEmpty()) {
                continue;
            }
            if (predicate.test(itemStack)) {
                if (CarpetOrgAdditionSettings.FAKE_PLAYER_ACTION_KEEP_ITEM.value() && itemStack.getMaxStackSize() != 1 && itemStack.getCount() == 1) {
                    continue;
                }
                FakePlayerUtils.dropCursorStack(menu, fakePlayer);
                menu.setCarried(itemStack.split(1));
                FakePlayerUtils.pickupCursorStack(menu, intputSlotIndex, fakePlayer);
                return true;
            }
        }
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_SHULKER_BOX_ITEM_HANDLING.value()) {
            for (ItemStack itemStack : inventory) {
                if (InventoryUtils.isShulkerBoxItem(itemStack)) {
                    ItemStack content = InventoryUtils.tryPickItemFromStackedNonEmptyShulkerBox(fakePlayer, itemStack, predicate, 1);
                    if (content.isEmpty()) {
                        continue;
                    }
                    FakePlayerUtils.dropCursorStack(menu, fakePlayer);
                    menu.setCarried(content);
                    FakePlayerUtils.pickupCursorStack(menu, intputSlotIndex, fakePlayer);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<Component> info() {
        ArrayList<Component> list = new ArrayList<>();
        LocalizationKey key = this.getLocalizationKey().then("info");
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        list.add(key.translate(fakePlayer.getDisplayName(), EnchantmentUtils.getName(this.enchantment), this.predicate.getDisplayName()));
        list.add(key.then("xp").translate(fakePlayer.experienceLevel));
        return list;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("item", this.predicate.toString());
        json.addProperty("enchantment", ServerUtils.getIdAsString(this.enchantment));
        return json;
    }

    @Override
    protected LocalizationKey getLocalizationKey() {
        return KEY;
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.ENCHANTING;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        EnchantingAction that = (EnchantingAction) obj;
        return Objects.equals(this.predicate, that.predicate) && Objects.equals(this.enchantment, that.enchantment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.predicate, this.enchantment);
    }
}
