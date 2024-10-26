package org.carpetorgaddition.command;

import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.jetbrains.annotations.Nullable;

public class XpTransferCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("xpTransfer")
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandXpTransfer))
                .then(CommandManager.argument("outputPlayer", EntityArgumentType.player())
                        .then(CommandManager.argument("inputPlayer", EntityArgumentType.player())
                                .then(CommandManager.literal("all")
                                        .executes(XpTransferCommand::xpAllTransfer))
                                .then(CommandManager.literal("half")
                                        .executes(XpTransferCommand::xpHalfTransfer))
                                .then(CommandManager.literal("points")
                                        .then(CommandManager.argument("number", IntegerArgumentType.integer(0))
                                                .executes(context -> xpPointTransfer(context, null))))
                                .then(CommandManager.literal("level")
                                        .then(CommandManager.argument("level", IntegerArgumentType.integer(0))
                                                .executes(context -> xpPointTransfer(context,
                                                        getTotalExperience(IntegerArgumentType.getInteger(context,
                                                                "level"), 0))))))));
    }

    // 转移所有经验
    private static int xpAllTransfer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        // 获取命令执行者
        ServerPlayerEntity sourcePlayer = source.getPlayer();
        // 获取输出经验的玩家
        ServerPlayerEntity outputPlayer = getOutputPlayer(context);
        // 获取输入经验的玩家
        ServerPlayerEntity inputPlayer = getInputPlayer(context);
        // 输出经验的玩家必须是假玩家或者是命令执行者自己
        if (outputPlayer instanceof EntityPlayerMPFake || outputPlayer == sourcePlayer) {
            // 获取玩家当前的经验值
            int points = MathHelper.floor(outputPlayer.experienceProgress * (float) outputPlayer.getNextLevelExperience());
            // 获取玩家的总经验值
            int totalExperience = getTotalExperience(outputPlayer.experienceLevel, points);
            // 清除输出玩家的经验
            outputPlayer.setExperienceLevel(0);
            outputPlayer.setExperiencePoints(0);
            // 把经验给输入玩家
            inputPlayer.addExperience(totalExperience);
            MessageUtils.sendMessage(source, "carpet.commands.xpTransfer.all",
                    outputPlayer.getDisplayName(), totalExperience, inputPlayer.getDisplayName());
            return totalExperience;
        } else {
            // 发送需要目标是自己或假玩家消息
            throw CommandUtils.createException("carpet.commands.xpTransfer.self_or_fake_player");
        }
    }

    // 转移一半经验
    private static int xpHalfTransfer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        // 获取命令执行者玩家
        ServerPlayerEntity sourcePlayer = source.getPlayer();
        // 获取输出经验的玩家
        ServerPlayerEntity outputPlayer = getOutputPlayer(context);
        // 获取输入经验的玩家
        ServerPlayerEntity inputPlayer = getInputPlayer(context);
        // 只能操作自己或假玩家
        if (outputPlayer instanceof EntityPlayerMPFake || outputPlayer == sourcePlayer) {
            // 获取玩家当前的经验值
            int points = MathHelper.floor(outputPlayer.experienceProgress * (float) outputPlayer.getNextLevelExperience());
            // 获取玩家的总经验值
            int totalExperience = getTotalExperience(outputPlayer.experienceLevel, points);
            // 将玩家的经验值取半
            int halfExperience = totalExperience / 2;
            // 清除两个玩家的所有经验
            outputPlayer.setExperienceLevel(0);
            outputPlayer.setExperiencePoints(0);
            // 将输出玩家一半的经验转移至输入玩家身上
            inputPlayer.addExperience(halfExperience);
            // 将另一半经验再转移回输出玩家身上
            outputPlayer.addExperience(totalExperience - halfExperience);
            MessageUtils.sendMessage(source, "carpet.commands.xpTransfer.half",
                    outputPlayer.getDisplayName(), halfExperience, inputPlayer.getDisplayName());
            return halfExperience;
        } else {
            // 发送消息：只允许操作自己或假玩家
            throw CommandUtils.createException("carpet.commands.xpTransfer.self_or_fake_player");
        }
    }

    // 转移指定数量经验
    private static int xpPointTransfer(CommandContext<ServerCommandSource> context,
                                       @Nullable Integer number) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        // 获取命令执行者玩家
        ServerPlayerEntity serverCommandSourcePlayer = source.getPlayer();
        // 获取输出经验的玩家
        ServerPlayerEntity outputPlayer = getOutputPlayer(context);
        // 获取输入经验的玩家
        PlayerEntity inputPlayer = getInputPlayer(context);
        // 获取要转移的经验数量
        int xpNumber = number == null ? IntegerArgumentType.getInteger(context, "number") : number;
        // 只能操作自己或假玩家
        if (outputPlayer instanceof EntityPlayerMPFake || outputPlayer == serverCommandSourcePlayer) {
            // 获取玩家当前的经验值，不考虑经验等级
            int points = MathHelper.floor(outputPlayer.experienceProgress
                    * (float) outputPlayer.getNextLevelExperience());
            // 获取玩家的总经验值，等级+当前经验值
            int totalExperience = getTotalExperience(outputPlayer.experienceLevel, points);
            // 要转移经验的数量不能多于玩家的总经验
            if (xpNumber > totalExperience) {
                throw CommandUtils.createException("carpet.commands.xpTransfer.point.fail",
                        outputPlayer.getDisplayName(), xpNumber, totalExperience);
            }
            // 清除两个玩家的所有经验
            outputPlayer.setExperienceLevel(0);
            outputPlayer.setExperiencePoints(0);
            // 将指定数量的经验添加给输入玩家
            inputPlayer.addExperience(xpNumber);
            // 将剩余的经验再添加回输出玩家
            outputPlayer.addExperience(totalExperience - xpNumber);
            MessageUtils.sendMessage(source, "carpet.commands.xpTransfer.point",
                    outputPlayer.getDisplayName(), xpNumber, inputPlayer.getDisplayName());
            return xpNumber;
        } else {
            // 发送消息：只允许操作自己或假玩家
            throw CommandUtils.createException("carpet.commands.xpTransfer.self_or_fake_player");
        }
    }

    // 获取要输出经验的玩家
    private static ServerPlayerEntity getOutputPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return EntityArgumentType.getPlayer(context, "outputPlayer");
    }

    // 获取要输入经验的玩家
    private static ServerPlayerEntity getInputPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return EntityArgumentType.getPlayer(context, "inputPlayer");
    }

    /**
     * 根据经验等级和经验值计算总经验值<br/>
     *
     * @param level 经验等级
     * @param xp    经验值
     * @return 总经验值
     * @author ChatGPT
     */
    private static int getTotalExperience(int level, int xp) {
        int totalExp;
        // 0-16级
        if (level <= 16) {
            totalExp = level * level + 6 * level;
        }
        // 17-31级
        else if (level <= 31) {
            totalExp = (int) (2.5 * level * level - 40.5 * level + 360);
        }
        // 32级以上
        else {
            totalExp = (int) (4.5 * level * level - 162.5 * level + 2220);
        }
        // 防止数值溢出
        int sum = totalExp + xp;
        return sum < 0 ? totalExp : sum;
    }
}
