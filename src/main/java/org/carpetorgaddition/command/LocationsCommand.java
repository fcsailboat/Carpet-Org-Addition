package org.carpetorgaddition.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.*;
import org.carpetorgaddition.util.wheel.Waypoint;
import org.carpetorgaddition.util.wheel.WorldFormat;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

//路径点管理器
public class LocationsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("locations")
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandLocations))
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .executes(context -> addWayPoint(context, null))
                                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                        .executes(context -> addWayPoint(context, BlockPosArgumentType.getBlockPos(context, "pos"))))))
                .then(CommandManager.literal("list")
                        .executes(context -> listWayPoint(context, null)).then(CommandManager.argument("filter", StringArgumentType.string())
                                .executes(context -> listWayPoint(context, StringArgumentType.getString(context, "filter")))))
                .then(CommandManager.literal("supplement")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests(suggestion())
                                .then(CommandManager.literal("illustrate")
                                        .executes(context -> addIllustrateText(context, null))
                                        .then(CommandManager.argument("illustrate", StringArgumentType.string())
                                                .executes(context -> addIllustrateText(context, StringArgumentType.getString(context, "illustrate")))))
                                .then(CommandManager.literal("another_pos")
                                        .executes(context -> addAnotherPos(context, null))
                                        .then(CommandManager.argument("anotherPos", BlockPosArgumentType.blockPos())
                                                .executes(context -> addAnotherPos(context, BlockPosArgumentType.getBlockPos(context, "anotherPos")))))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests(suggestion())
                                .executes(LocationsCommand::deleteWayPoint)))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests(suggestion())
                                .executes(context -> setWayPoint(context, null))
                                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                        .executes(context -> setWayPoint(context, BlockPosArgumentType.getBlockPos(context, "pos")))))));
    }

    // 用来自动补全路径点名称
    public static SuggestionProvider<ServerCommandSource> suggestion() {
        return (context, builder) -> {
            WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), Waypoint.WAYPOINT);
            return CommandSource.suggestMatching(worldFormat.toImmutableFileList().stream().map(File::getName)
                    .filter(name -> name.endsWith(IOUtils.JSON_EXTENSION)).map(IOUtils::removeExtension)
                    .map(StringArgumentType::escapeIfRequired), builder);
        };
    }

    // 添加路径点
    private static int addWayPoint(CommandContext<ServerCommandSource> context, @Nullable BlockPos blockPos) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 获取路径点名称和位置对象
        String name = StringArgumentType.getString(context, "name");
        if (blockPos == null) {
            blockPos = player.getBlockPos();
        }
        // 获取服务器对象
        MinecraftServer server = context.getSource().getServer();
        WorldFormat worldFormat = new WorldFormat(server, Waypoint.WAYPOINT);
        // 检查文件是否已存在
        if (worldFormat.fileExists(name)) {
            throw CommandUtils.createException("carpet.commands.locations.add.fail.already_exists", name);
        }
        // 创建一个路径点对象
        Waypoint waypoint = new Waypoint(blockPos, name, WorldUtils.getDimensionId(context.getSource().getWorld()), player.getName().getString());
        try {
            // 将路径点写入本地文件
            waypoint.save(server);
            // 成功添加路径点
            MessageUtils.sendMessage(context.getSource(), "carpet.commands.locations.add.success", name, WorldUtils.toPosString(blockPos));
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.error("{}在尝试将路径点写入本地文件时出现意外问题:", GameUtils.getPlayerName(player), e);
        }
        return 1;
    }

    // 列出所有路径点
    private static int listWayPoint(CommandContext<ServerCommandSource> context, @Nullable String filter) {
        MinecraftServer server = context.getSource().getServer();
        WorldFormat worldFormat = new WorldFormat(server, Waypoint.WAYPOINT);
        List<File> list = worldFormat.toImmutableFileList();
        if (list.isEmpty()) {
            MessageUtils.sendMessage(context, "carpet.commands.locations.list.no_waypoint");
            return 0;
        }
        MutableText dividerLine = TextUtils.createText("------------------------------");
        // 显示分隔线
        MessageUtils.sendMessage(context.getSource(), dividerLine);
        int count = 0;
        // 遍历文件夹下的所有文件
        for (File file : list) {
            String name = file.getName();
            // 只显示包含指定字符串的路径点，如果为null，显示所有路径点
            if (filter != null && !name.contains(filter)) {
                continue;
            }
            Optional<Waypoint> optional;
            try {
                optional = Waypoint.load(server, name);
            } catch (IOException | NullPointerException e) {
                //无法解析坐标
                CarpetOrgAddition.LOGGER.warn("无法解析路径点[{}]", IOUtils.removeExtension(name), e);
                continue;
            }
            // 显示路径点
            if (optional.isPresent()) {
                optional.get().show(context.getSource());
                count++;
            }
        }
        MessageUtils.sendMessage(context.getSource(), dividerLine);
        return count;
    }

    // 添加说明文本
    private static int addIllustrateText(CommandContext<ServerCommandSource> context, @Nullable String illustrate) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = context.getSource().getServer();
        // 获取路径点的名称
        String name = StringArgumentType.getString(context, "name");
        try {
            // 从本地文件中读取路径点对象
            Optional<Waypoint> optional = Waypoint.load(server, name);
            if (optional.isPresent()) {
                boolean remove = false;
                if (illustrate == null || illustrate.isEmpty()) {
                    illustrate = null;
                    remove = true;
                }
                Waypoint waypoint = optional.get();
                waypoint.setIllustrate(illustrate);
                // 将路径点对象重新写入本地文件
                waypoint.save(server);
                if (remove) {
                    // 移除路径点的说明文本
                    MessageUtils.sendMessage(source, "carpet.commands.locations.illustrate.remove", name);
                } else {
                    // 为路径点添加说明文本
                    MessageUtils.sendMessage(source, "carpet.commands.locations.illustrate.add", illustrate, name);
                }
            }
        } catch (IOException | NullPointerException e) {
            //无法添加说明文本
            CarpetOrgAddition.LOGGER.error("无法为路径点[{}]添加说明文本", name, e);
            throw CommandUtils.createException("carpet.commands.locations.illustrate.io", name);
        }
        return 1;
    }

    // 添加另一个坐标
    private static int addAnotherPos(CommandContext<ServerCommandSource> context, @Nullable BlockPos blockPos) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        ServerCommandSource source = context.getSource();
        // 路径点的另一个坐标
        if (blockPos == null) {
            blockPos = player.getBlockPos();
        }
        MinecraftServer server = context.getSource().getServer();
        // 路径点的名称
        String name = StringArgumentType.getString(context, "name");
        try {
            // 从文件中读取路径点对象
            Optional<Waypoint> optional = Waypoint.load(server, name);
            if (optional.isPresent()) {
                Waypoint waypoint = optional.get();
                if (waypoint.canAddAnother()) {
                    waypoint.setAnotherBlockPos(blockPos);
                } else {
                    // 不能为末地添加对向坐标
                    throw CommandUtils.createException("carpet.commands.locations.another.add.fail");
                }
                // 将修改后的路径点重新写入本地文件
                waypoint.save(server);
                //添加对向坐标
                MessageUtils.sendMessage(source, "carpet.commands.locations.another.add");
            }
        } catch (IOException | NullPointerException e) {
            CarpetOrgAddition.LOGGER.error("无法解析路径点[{}]:", name, e);
            throw CommandUtils.createException("carpet.commands.locations.another.io", name);
        }
        return 1;
    }

    // 删除路径点
    private static int deleteWayPoint(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        //获取路径点文件名
        String name = StringArgumentType.getString(context, "name");
        //获取路径点文件对象
        WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), Waypoint.WAYPOINT);
        // 获取路径点文件的对象
        File file = worldFormat.jsonFile(name);
        //从本地文件删除路径点
        if (file.delete()) {
            // 成功删除
            MessageUtils.sendMessage(source, "carpet.commands.locations.remove.success", name);
        } else {
            // 删除失败
            throw CommandUtils.createException("carpet.commands.locations.remove.fail", name);
        }
        return 1;
    }

    // 修改路径点
    private static int setWayPoint(CommandContext<ServerCommandSource> context, @Nullable BlockPos blockPos) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        ServerCommandSource source = context.getSource();
        if (blockPos == null) {
            blockPos = player.getBlockPos();
        }
        String fileName = StringArgumentType.getString(context, "name");
        try {
            Optional<Waypoint> optional = Waypoint.load(context.getSource().getServer(), fileName);
            if (optional.isPresent()) {
                Waypoint waypoint = optional.get();
                waypoint.setBlockPos(blockPos);
                // 将修改完坐标的路径点对象重新写入本地文件
                waypoint.save(context.getSource().getServer());
                //发送命令执行后的反馈
                MessageUtils.sendMessage(source, "carpet.commands.locations.set", fileName);
            }
        } catch (IOException | NullPointerException e) {
            throw CommandUtils.createException("carpet.commands.locations.set.io", fileName);
        }
        return 1;
    }
}
