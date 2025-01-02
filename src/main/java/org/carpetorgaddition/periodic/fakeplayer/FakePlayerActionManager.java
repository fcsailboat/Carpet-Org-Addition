package org.carpetorgaddition.periodic.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.periodic.fakeplayer.actiondata.*;

public class FakePlayerActionManager {
    private final EntityPlayerMPFake fakePlayer;
    private final ActionFunction function = new ActionFunction();

    public FakePlayerActionManager(EntityPlayerMPFake fakePlayer) {
        this.fakePlayer = fakePlayer;
    }

    // 执行动作
    public void executeAction() {
        switch (function.getAction()) {
            case STOP -> {
                // 什么也不做
            }
            // 物品分拣
            case SORTING -> FakePlayerSorting.sorting((SortingData) function.getActionData(), fakePlayer);
            // 清空潜影盒
            case CLEAN -> FakePlayerClean.clean((CleanData) function.getActionData(), fakePlayer);
            // 填充潜影盒
            case FILL -> FakePlayerFill.fill((FillData) function.getActionData(), fakePlayer);
            // 在生存模式物品栏合成物品
            case INVENTORY_CRAFT ->
                    FakePlayerCraft.inventoryCraft((InventoryCraftData) function.getActionData(), fakePlayer);
            // 在工作台合成物品
            case CRAFTING_TABLE_CRAFT ->
                    FakePlayerCraft.craftingTableCraft((CraftingTableCraftData) function.getActionData(), fakePlayer);
            // 重命名物品
            case RENAME -> FakePlayerRename.rename((RenameData) function.getActionData(), fakePlayer);
            // 使用切石机
            case STONECUTTING ->
                    FakePlayerStonecutting.stonecutting((StonecuttingData) function.actionData, fakePlayer);
            // 自动交易
            case TRADE -> FakePlayerTrade.trade((TradeData) function.actionData, fakePlayer);
            // 自动钓鱼
            case FISHING -> FakePlayerFishing.fishing((FishingData) function.getActionData(), fakePlayer);
            default -> {
                CarpetOrgAddition.LOGGER.error("{}的行为没有预先定义", this.function.getAction());
                this.stop();
            }
        }
    }

    public FakePlayerAction getAction() {
        return function.getAction();
    }

    public AbstractActionData getActionData() {
        return this.function.getActionData();
    }

    // 设置假玩家当前的动作，类型必须与数据对应
    public void setAction(FakePlayerAction action, AbstractActionData data) {
        this.function.setAction(action, data);
    }

    // 让假玩家停止当前的动作
    public void stop() {
        this.function.setAction(FakePlayerAction.STOP, StopData.STOP);
    }

    // 从另一个玩家浅拷贝此动作管理器
    public void copyActionData(EntityPlayerMPFake oldPlayer) {
        FakePlayerActionManager actionManager = ((FakePlayerActionInterface) oldPlayer).getActionManager();
        this.setAction(actionManager.getAction(), actionManager.getActionData());
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        String action = switch (this.getAction()) {
            case STOP -> "stop";
            case SORTING -> "sorting";
            case CLEAN -> "clean";
            case FILL -> "fill";
            case INVENTORY_CRAFT -> "inventory_crafting";
            case CRAFTING_TABLE_CRAFT -> "crafting_table_craft";
            case RENAME -> "rename";
            case STONECUTTING -> "stonecutting";
            case TRADE -> "trade";
            case FISHING -> "fishing";
        };
        json.add(action, this.getActionData().toJson());
        return json;
    }

    /**
     * 将动作类型和动作数据封装起来，保证类型与数据对应，类中所以成员变量和成员方法全部为私有，防止外部其他类直接调用
     */
    private static class ActionFunction {
        private FakePlayerAction action = FakePlayerAction.STOP;
        private AbstractActionData actionData = StopData.STOP;

        // 动作类型必须和动作数据一起修改来保证类型与数据对应
        private void setAction(FakePlayerAction action, AbstractActionData actionData) {
            // 检查动作类型是否与数据匹配
            action.checkActionData(actionData.getClass());
            this.action = action;
            this.actionData = actionData;
        }

        private FakePlayerAction getAction() {
            return action;
        }

        private AbstractActionData getActionData() {
            return actionData;
        }
    }
}
