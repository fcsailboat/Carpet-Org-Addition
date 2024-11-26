package org.carpetorgaddition.command;

import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.carpetorgaddition.util.screen.PlayerEnderChestScreenHandler;
import org.carpetorgaddition.util.screen.PlayerInventoryScreenHandler;

@SuppressWarnings("SameReturnValue")
public class PlayerToolsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("playerTools")
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandPlayerTools))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.literal("enderChest")
                                .executes(context -> openEnderChest(context, CommandUtils.getArgumentPlayer(context))))
                        .then(CommandManager.literal("inventory")
                                .executes(PlayerToolsCommand::openFakePlayerInventory))
                        .then(CommandManager.literal("teleport")
                                .executes(PlayerToolsCommand::fakePlayerTp))
                        .then(CommandManager.literal("isFakePlayer")
                                .executes(PlayerToolsCommand::isFakePlayer))
                        .then(CommandManager.literal("position")
                                .executes(PlayerToolsCommand::getFakePlayerPos))
                        .then(CommandManager.literal("heal")
                                .executes(PlayerToolsCommand::fakePlayerHeal))));
    }

    // 假玩家治疗
    private static int fakePlayerHeal(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 计算假玩家回复的血量，这是方法和命令的返回值
        float health = fakePlayer.getMaxHealth() - fakePlayer.getHealth();
        // 回复血量
        fakePlayer.heal(fakePlayer.getMaxHealth());
        // 回复饥饿值
        fakePlayer.getHungerManager().setFoodLevel(20);
        // 发送血量回复完后的命令反馈
        MessageUtils.sendMessage(context.getSource(), "carpet.commands.playerTools.heal", fakePlayer.getDisplayName());
        return (int) health;
    }

    // 打开玩家末影箱
    private static int openEnderChest(CommandContext<ServerCommandSource> context, ServerPlayerEntity fakePlayer) throws CommandSyntaxException {
        PlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 检查玩家是否是假玩家或自己
        if (fakePlayer instanceof EntityPlayerMPFake || fakePlayer == player) {
            // 创建GUI对象
            SimpleNamedScreenHandlerFactory screen = new SimpleNamedScreenHandlerFactory((i, inventory, playerEntity1) ->
                    new PlayerEnderChestScreenHandler(i, inventory, fakePlayer), fakePlayer.getName());
            // 打开末影箱GUI
            player.openHandledScreen(screen);
        } else {
            // 只允许操作自己和假玩家
            throw CommandUtils.createException("carpet.commands.playerTools.self_or_fake_player");
        }
        return 1;
    }

    // 假玩家传送
    private static int fakePlayerTp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        ServerPlayerEntity fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 在假玩家位置播放潜影贝传送音效
        fakePlayer.getWorld().playSound(null, fakePlayer.prevX, fakePlayer.prevY, fakePlayer.prevZ,
                SoundEvents.ENTITY_SHULKER_TELEPORT, fakePlayer.getSoundCategory(), 1.0f, 1.0f);
        // 传送玩家
        fakePlayer.teleport(player.getServerWorld(), player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
        // 获取假玩家名和命令执行玩家名
        Text fakePlayerName = fakePlayer.getDisplayName();
        Text playerName = player.getDisplayName();
        // 在聊天栏显示命令反馈
        MessageUtils.sendMessage(context.getSource(), "carpet.commands.playerTools.tp.success", fakePlayerName, playerName);
        return 1;
    }

    // 判断玩家是否为假玩家
    private static int isFakePlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getArgumentPlayer(context);
        // 获取玩家名
        Text playerName = player.getDisplayName();
        if (player instanceof EntityPlayerMPFake) {
            MessageUtils.sendMessage(context, "carpet.commands.playerTools.is_fake_player", playerName);
            return 0;
        } else {
            MessageUtils.sendMessage(context, "carpet.commands.playerTools.is_player", playerName);
            return 1;
        }
    }

    // 获取假玩家位置
    private static int getFakePlayerPos(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        ServerCommandSource source = context.getSource();
        // 发送命令反馈
        MessageUtils.sendMessage(source, "carpet.commands.playerTools.pos", fakePlayer.getDisplayName(),
                getDimensionText(fakePlayer.getWorld()).getString(),
                TextConstants.blockPos(new BlockPos(fakePlayer.getBlockPos()), Formatting.GREEN));
        // 如果命令执行者是玩家，返回距离假玩家的位置
        ServerPlayerEntity player = source.getPlayer();
        if (player != null) {
            return MathUtils.getBlockIntegerDistance(player.getBlockPos(), fakePlayer.getBlockPos());
        }
        return 0;
    }

    // 获取维度名称
    private static Text getDimensionText(World world) {
        Identifier value = world.getDimension().effects();
        if (value.equals(DimensionTypes.OVERWORLD_ID)) {
            return TextUtils.translate("carpet.commands.playerTools.pos.overworld");
        } else if (value.equals(DimensionTypes.THE_NETHER_ID)) {
            return TextUtils.translate("carpet.commands.playerTools.pos.the_nether");
        } else if (value.equals(DimensionTypes.THE_END_ID)) {
            return TextUtils.translate("carpet.commands.playerTools.pos.the_end");
        }
        return TextUtils.translate("carpet.commands.playerTools.pos.default");
    }


    // 打开假玩家物品栏GUI
    private static int openFakePlayerInventory(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        ServerPlayerEntity fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        SimpleNamedScreenHandlerFactory screen = new SimpleNamedScreenHandlerFactory((syncId, playerInventory, playerEntity)
                -> new PlayerInventoryScreenHandler(syncId, playerInventory, fakePlayer), fakePlayer.getName());
        // 打开物品栏
        player.openHandledScreen(screen);
        return 1;
    }
}
