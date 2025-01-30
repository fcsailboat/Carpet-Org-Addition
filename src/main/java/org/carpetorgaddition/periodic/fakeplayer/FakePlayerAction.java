package org.carpetorgaddition.periodic.fakeplayer;

import net.minecraft.text.MutableText;
import org.carpetorgaddition.periodic.fakeplayer.actioncontext.*;
import org.carpetorgaddition.util.TextUtils;

public enum FakePlayerAction {
    /**
     * 停止操作
     */
    STOP("carpet.commands.playerAction.action.stop"),
    /**
     * 物品分拣
     */
    SORTING("carpet.commands.playerAction.action.sorting"),
    /**
     * 清空潜影盒
     */
    CLEAN("carpet.commands.playerAction.action.clean"),
    /**
     * 填充潜影盒
     */
    FILL("carpet.commands.playerAction.action.fill"),
    /**
     * 在工作台合成物品
     */
    CRAFTING_TABLE_CRAFT("carpet.commands.playerAction.action.crafting_table_craft"),
    /**
     * 在生存模式物品栏合成物品
     */
    INVENTORY_CRAFT("carpet.commands.playerAction.action.inventory_craft"),
    /**
     * 自动重命名物品
     */
    RENAME("carpet.commands.playerAction.action.rename"),
    /**
     * 自动使用切石机
     */
    STONECUTTING("carpet.commands.playerAction.action.stonecutting"),
    /**
     * 自动交易
     */
    TRADE("carpet.commands.playerAction.action.trade"),
    /**
     * 自动钓鱼
     */
    FISHING("carpet.commands.playerAction.action.fishing"),
    /**
     * 自动种植
     */
    FARMING("carpet.commands.playerAction.action.Farming");

    private final MutableText displayName;

    FakePlayerAction(String key) {
        this.displayName = TextUtils.translate(key);
    }

    // 检查当前动作是否与指定动作数据匹配
    public void checkActionData(Class<? extends AbstractActionContext> clazz) {
        if (clazz != switch (this) {
            case STOP -> StopContext.class;
            case SORTING -> SortingContext.class;
            case CLEAN -> CleanContext.class;
            case FILL -> FillContext.class;
            case INVENTORY_CRAFT -> InventoryCraftContext.class;
            case CRAFTING_TABLE_CRAFT -> CraftingTableCraftContext.class;
            case RENAME -> RenameContext.class;
            case STONECUTTING -> StonecuttingContext.class;
            case TRADE -> TradeContext.class;
            case FISHING -> FishingContext.class;
            case FARMING -> FarmingContext.class;
        }) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * @return 当前动作类型的显示名称
     */
    public MutableText getDisplayName() {
        return this.displayName;
    }

    @Override
    public String toString() {
        return switch (this) {
            case STOP -> "停止";
            case SORTING -> "分拣";
            case CLEAN -> "清空潜影盒";
            case FILL -> "填充潜影盒";
            case CRAFTING_TABLE_CRAFT -> "在工作台合成物品";
            case INVENTORY_CRAFT -> "在生存模式物品栏合成物品";
            case RENAME -> "重命名";
            case STONECUTTING -> "切石";
            case TRADE -> "交易";
            case FISHING -> "钓鱼";
            case FARMING -> "种植";
        };
    }
}
