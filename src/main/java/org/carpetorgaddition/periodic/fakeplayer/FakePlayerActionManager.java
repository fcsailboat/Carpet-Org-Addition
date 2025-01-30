package org.carpetorgaddition.periodic.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.periodic.PeriodicTaskUtils;
import org.carpetorgaddition.periodic.fakeplayer.actioncontext.*;
import org.carpetorgaddition.util.MessageUtils;

public class FakePlayerActionManager {
    private final EntityPlayerMPFake fakePlayer;
    private final ActionFunction function = new ActionFunction();

    public FakePlayerActionManager(EntityPlayerMPFake fakePlayer) {
        this.fakePlayer = fakePlayer;
    }

    public void tick() {
        try {
            // 根据假玩家动作类型执行动作
            this.executeAction();
        } catch (RuntimeException e) {
            // 将错误信息写入日志
            CarpetOrgAddition.LOGGER.error(
                    "{}在执行操作“{}”时遇到意外错误:",
                    this.fakePlayer.getName().getString(),
                    this.getAction().toString(),
                    e
            );
            MessageUtils.broadcastErrorMessage(
                    this.fakePlayer.server,
                    e,
                    "carpet.commands.playerAction.exception.runtime",
                    this.fakePlayer.getDisplayName(),
                    this.getAction().getDisplayName()
            );
            // 让假玩家停止当前操作
            this.stop();
        }
    }

    // 执行动作
    private void executeAction() {
        FakePlayerAction action = function.getAction();
        if (action == FakePlayerAction.STOP) {
            return;
        }
        switch (action) {
            // 物品分拣
            case SORTING -> FakePlayerSorting.sorting((SortingContext) function.getActionContext(), fakePlayer);
            // 清空潜影盒
            case CLEAN -> FakePlayerClean.clean((CleanContext) function.getActionContext(), fakePlayer);
            // 填充潜影盒
            case FILL -> FakePlayerFill.fill((FillContext) function.getActionContext(), fakePlayer);
            // 在生存模式物品栏合成物品
            case INVENTORY_CRAFT ->
                    FakePlayerCraft.inventoryCraft((InventoryCraftContext) function.getActionContext(), fakePlayer);
            // 在工作台合成物品
            case CRAFTING_TABLE_CRAFT ->
                    FakePlayerCraft.craftingTableCraft((CraftingTableCraftContext) function.getActionContext(), fakePlayer);
            // 重命名物品
            case RENAME -> FakePlayerRename.rename((RenameContext) function.getActionContext(), fakePlayer);
            // 使用切石机
            case STONECUTTING ->
                    FakePlayerStonecutting.stonecutting((StonecuttingContext) function.actionContext, fakePlayer);
            // 自动交易
            case TRADE -> FakePlayerTrade.trade((TradeContext) function.actionContext, fakePlayer);
            // 自动钓鱼
            case FISHING -> FakePlayerFishing.fishing((FishingContext) function.getActionContext(), fakePlayer);
            // 自动种植
            case FARM -> FakePlayerFarm.farm((FarmContext) function.getActionContext(), fakePlayer);
            default -> {
                CarpetOrgAddition.LOGGER.error("行为“{}”没有预先定义", action);
                this.stop();
            }
        }
    }

    public FakePlayerAction getAction() {
        return function.getAction();
    }

    public AbstractActionContext getActionContext() {
        return this.function.getActionContext();
    }

    // 设置假玩家当前的动作，类型必须与数据对应
    public void setAction(FakePlayerAction action, AbstractActionContext context) {
        this.function.setAction(action, context);
    }

    // 让假玩家停止当前的动作
    public void stop() {
        this.function.setAction(FakePlayerAction.STOP, StopContext.STOP);
    }

    // 从另一个玩家浅拷贝此动作管理器
    public void setActionFromOldPlayer(EntityPlayerMPFake oldPlayer) {
        FakePlayerActionManager actionManager = PeriodicTaskUtils.getFakePlayerActionManager(oldPlayer);
        this.setAction(actionManager.getAction(), actionManager.getActionContext());
    }

    /**
     * 将动作类型和动作数据封装起来，保证类型与数据对应，类中所以成员变量和成员方法全部为私有，防止外部其他类直接调用
     */
    private static class ActionFunction {
        private FakePlayerAction action = FakePlayerAction.STOP;
        private AbstractActionContext actionContext = StopContext.STOP;

        // 动作类型必须和动作数据一起修改来保证类型与数据对应
        private void setAction(FakePlayerAction action, AbstractActionContext actionContext) {
            // 检查动作类型是否与数据匹配
            if (actionContext.getClass() != action.getContextClass()) {
                throw new IllegalArgumentException();
            }
            this.action = action;
            this.actionContext = actionContext;
        }

        private FakePlayerAction getAction() {
            return action;
        }

        private AbstractActionContext getActionContext() {
            return actionContext;
        }
    }
}
