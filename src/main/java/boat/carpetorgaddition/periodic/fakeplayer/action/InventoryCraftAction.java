package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class InventoryCraftAction extends AbstractCraftAction {
    public InventoryCraftAction(EntityPlayerMPFake fakePlayer, ItemStackPredicate[] predicates) {
        super(fakePlayer, predicates);
    }

    @Override
    protected int getCraftGridSize() {
        return 4;
    }

    @Override
    protected int getCraftGridEnd() {
        return 4;
    }

    @Override
    protected int getInventoryStart() {
        return 5;
    }

    @Override
    protected int getInventoryEnd() {
        return Objects.requireNonNull(this.getScreenHandler()).slots.size();
    }

    @Override
    protected @Nullable AbstractContainerMenu getScreenHandler() {
        return this.getFakePlayer().inventoryMenu;
    }

    @Override
    public List<Component> info() {
        TextJoiner joiner = new TextJoiner();
        joiner.unsetBullet();
        joiner.setIndent(4);
        Component name = this.getFakePlayer().getDisplayName();
        // 物品配方
        ItemStack craftOutput = this.getCraftOutput(this.predicates, 2, this.getFakePlayer());
        Component itemText = craftOutput.isEmpty() ? LocalizationKeys.Item.ITEM.translate() : ServerUtils.getName(craftOutput.getItem());
        LocalizationKey key = this.getInfoLocalizationKey();
        joiner.newline(key.translate(name, itemText));
        joiner.enter(() -> this.addCraftRecipe(joiner, craftOutput));
        // 合成方格状态
        joiner.newline(key.then("state").translate(name));
        InventoryMenu playerScreenHandler = this.getFakePlayer().inventoryMenu;
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
    public Component getDisplayName() {
        return this.getLocalizationKey().then("inventory").translate();
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.INVENTORY_CRAFT;
    }
}
