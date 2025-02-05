package org.carpetorgaddition.util;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;

import java.util.UUID;

public class CommandUtils {
    public static final String PLAYER = "player";

    private CommandUtils() {
    }

    /**
     * 根据命令执行上下文获取命令执行者玩家对象
     *
     * @param context 用来获取玩家的命令执行上下文
     * @return 命令的执行玩家
     * @throws CommandSyntaxException 如果命令执行者不是玩家，则抛出该异常
     */
    public static ServerPlayerEntity getSourcePlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return getSourcePlayer(context.getSource());
    }

    /**
     * 根据命令源获取命令执行者玩家对象
     *
     * @param source 用来获取玩家的命令源
     * @return 命令的执行玩家
     * @throws CommandSyntaxException 如果命令执行者不是玩家，则抛出该异常
     */
    public static ServerPlayerEntity getSourcePlayer(ServerCommandSource source) throws CommandSyntaxException {
        return source.getPlayerOrThrow();
    }

    /**
     * 获取命令参数中的玩家对象
     */
    public static ServerPlayerEntity getArgumentPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return EntityArgumentType.getPlayer(context, PLAYER);
    }

    /**
     * 获取命令参数中的玩家对象，并检查是不是假玩家
     */
    public static EntityPlayerMPFake getArgumentFakePlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, PLAYER);
        checkFakePlayer(player);
        return (EntityPlayerMPFake) player;
    }

    /**
     * 创建一个命令语法参数异常对象
     *
     * @param key 异常信息的翻译键
     * @return 命令语法参数异常
     */
    public static CommandSyntaxException createException(String key, Object... obj) {
        return new SimpleCommandExceptionType(TextUtils.translate(key, obj)).create();
    }

    public static CommandSyntaxException createException(Throwable e, String key, Object... obj) {
        String exceptionMessage = GameUtils.getExceptionString(e);
        MutableText message = TextUtils.translate(key, obj);
        return new SimpleCommandExceptionType(TextUtils.hoverText(message, exceptionMessage)).create();
    }

    /**
     * 判断指定玩家是否为假玩家，如果不是会直接抛出异常。<br/>
     *
     * @param fakePlayer 要检查是否为假玩家的玩家对象
     * @throws CommandSyntaxException 如果指定玩家不是假玩家抛出异常
     */
    public static void checkFakePlayer(PlayerEntity fakePlayer) throws CommandSyntaxException {
        if (fakePlayer instanceof EntityPlayerMPFake) {
            return;
        }
        // 不是假玩家时抛出异常
        throw createException("carpet.command.not_fake_player", fakePlayer.getDisplayName());
    }

    /**
     * 从字符串解析一个UUID
     */
    public static UUID parseUuidFromString(String uuid) throws CommandSyntaxException {
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw createException("carpet.command.uuid.parse.fail");
        }
    }

    /**
     * 让一名玩家执行一条命令，命令的前缀“/”可有可无，但不建议有
     */
    public static void execute(ServerPlayerEntity player, String command) {
        CommandUtils.execute(player.getCommandSource(), command);
    }

    public static void execute(ServerCommandSource source, String command) {
        CommandManager commandManager = source.getServer().getCommandManager();
        commandManager.executeWithPrefix(source, command);
    }
}
