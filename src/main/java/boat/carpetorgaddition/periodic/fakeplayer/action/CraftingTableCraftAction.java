package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class CraftingTableCraftAction extends CraftAction {
    public static final LocalizationKey KEY = PlayerActionCommand.KEY.then("craft");

    public CraftingTableCraftAction(EntityPlayerMPFake fakePlayer, ItemStackPredicate[] predicates) {
        super(fakePlayer, predicates);
    }

    @Override
    protected int getCraftGridSize() {
        return 9;
    }

    @Override
    protected int getCraftGridStart() {
        return 1;
    }

    @Override
    protected int getCraftGridEnd() {
        return 9;
    }

    @Override
    protected int getInventoryStart() {
        return 10;
    }

    @Override
    protected int getInventoryEnd() {
        return Objects.requireNonNull(this.getScreenHandler()).slots.size();
    }

    @Override
    protected @Nullable CraftingMenu getScreenHandler() {
        AbstractContainerMenu menu = this.getFakePlayer().containerMenu;
        return menu instanceof CraftingMenu craftingMenu ? craftingMenu : null;
    }

    @Override
    public List<Component> info() {
        TextJoiner joiner = new TextJoiner();
        joiner.unsetBullet();
        joiner.setIndent(4);
        ItemStack craftOutput = this.getCraftOutput(this.predicates, 3, this.getFakePlayer());
        Component itemText = craftOutput.isEmpty() ? LocalizationKeys.Item.ITEM.translate() : ServerUtils.getName(craftOutput.getItem());
        Component displayName = this.getFakePlayer().getDisplayName();
        LocalizationKey key = this.getInfoLocalizationKey();
        joiner.newline(key.translate(displayName, itemText));
        joiner.enter(() -> this.addCraftRecipe(joiner, craftOutput));
        CraftingMenu screenHandler = this.getScreenHandler();
        if (screenHandler != null) {
            // 合成方格状态
            joiner.newline(key.then("state").translate(displayName));
            joiner.enter(() -> this.addCraftGridState(screenHandler, joiner));
        } else {
            // 未打开工作台
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
}
