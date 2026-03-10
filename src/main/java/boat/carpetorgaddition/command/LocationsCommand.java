package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.Waypoint;
import boat.carpetorgaddition.wheel.WorldFormat;
import boat.carpetorgaddition.wheel.page.PageManager;
import boat.carpetorgaddition.wheel.page.PagedCollection;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// 路径点管理器
public class LocationsCommand extends AbstractServerCommand {
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("locations");

    public LocationsCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandLocations))
                .then(Commands.literal("add")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(context -> addWayPoint(context, null))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> addWayPoint(context, BlockPosArgument.getBlockPos(context, "pos"))))))
                .then(Commands.literal("list")
                        .executes(context -> listWayPoint(context, null))
                        .then(Commands.argument("filter", StringArgumentType.string())
                                .executes(context -> listWayPoint(context, StringArgumentType.getString(context, "filter")))))
                .then(Commands.literal("supplement")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(suggestion())
                                .then(Commands.literal("comment")
                                        .executes(context -> addIllustrateText(context, null))
                                        .then(Commands.argument("comment", StringArgumentType.string())
                                                .executes(context -> addIllustrateText(context, StringArgumentType.getString(context, "comment")))))
                                .then(Commands.literal("another_pos")
                                        .executes(context -> addAnotherPos(context, null))
                                        .then(Commands.argument("anotherPos", BlockPosArgument.blockPos())
                                                .executes(context -> addAnotherPos(context, BlockPosArgument.getBlockPos(context, "anotherPos")))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(suggestion())
                                .executes(this::deleteWayPoint)))
                .then(Commands.literal("set")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(suggestion())
                                .executes(context -> setWayPoint(context, null))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> setWayPoint(context, BlockPosArgument.getBlockPos(context, "pos"))))))
                .then(Commands.literal("here")
                        .executes(this::sendSelfLocation)));
    }

    // 用来自动补全路径点名称
    public static SuggestionProvider<CommandSourceStack> suggestion() {
        return (context, builder) -> {
            WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), Waypoint.WAYPOINT);
            return SharedSuggestionProvider.suggest(worldFormat.listFiles()
                    .stream()
                    .map(File::getName)
                    .filter(name -> name.endsWith(IOUtils.JSON_EXTENSION))
                    .map(s -> IOUtils.removeExtension(s, IOUtils.JSON_EXTENSION))
                    .map(StringArgumentType::escapeIfRequired), builder);
        };
    }

    // 添加路径点
    private int addWayPoint(CommandContext<CommandSourceStack> context, @Nullable BlockPos blockPos) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        // 获取路径点名称和位置对象
        String name = StringArgumentType.getString(context, "name");
        if (IOUtils.isValidFileName(name)) {
            throw CommandUtils.createException(LocalizationKeys.File.INVALID_NAME.translate());
        }
        if (blockPos == null) {
            blockPos = player.blockPosition();
        }
        // 获取服务器对象
        MinecraftServer server = context.getSource().getServer();
        WorldFormat worldFormat = new WorldFormat(server, Waypoint.WAYPOINT);
        LocalizationKey key = KEY.then("add");
        // 检查文件是否已存在
        if (worldFormat.file(name + IOUtils.JSON_EXTENSION).exists()) {
            throw CommandUtils.createException(key.then("fail").translate(name));
        }
        // 创建一个路径点对象
        Waypoint waypoint = new Waypoint(blockPos, name, player);
        try {
            // 将路径点写入本地文件
            waypoint.save();
            // 成功添加路径点
            MessageUtils.sendMessage(context.getSource(), key.then("success").translate(name, ServerUtils.toPosString(blockPos)));
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.error("{} encountered an unexpected issue while attempting to write waypoints to the local file: ", PlayerUtils.getName(player), e);
        }
        return 1;
    }

    // 列出所有路径点
    private int listWayPoint(CommandContext<CommandSourceStack> context, @Nullable String filter) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        WorldFormat worldFormat = new WorldFormat(server, Waypoint.WAYPOINT);
        List<File> list = worldFormat.listFiles();
        LocalizationKey key = KEY.then("list").then("empty");
        if (list.isEmpty()) {
            MessageUtils.sendMessage(context, key.translate());
            return 0;
        }
        PageManager pageManager = ServerComponentCoordinator.getCoordinator(server).getPageManager();
        PagedCollection collection = pageManager.newPagedCollection(context.getSource());
        ArrayList<Supplier<Component>> messages = new ArrayList<>();
        int count = 0;
        // 遍历文件夹下的所有文件
        for (File file : list) {
            String name = file.getName();
            // 只显示包含指定字符串的路径点，如果为null，显示所有路径点
            if (filter != null && !name.contains(filter)) {
                continue;
            }
            try {
                Waypoint waypoint = Waypoint.load(server, name);
                // 显示路径点
                messages.add(waypoint::line);
                count++;
            } catch (IOException | RuntimeException e) {
                // 无法解析坐标
                CarpetOrgAddition.LOGGER.warn("Failed to parse waypoint [{}]", IOUtils.removeExtension(name), e);
            }
        }
        if (messages.isEmpty()) {
            MessageUtils.sendMessage(context, key.translate());
            return 0;
        }
        collection.addContent(messages);
        MessageUtils.sendEmptyMessage(context);
        collection.print();
        return count;
    }

    // 添加说明文本
    private int addIllustrateText(CommandContext<CommandSourceStack> context, @Nullable String comment) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = context.getSource().getServer();
        // 获取路径点的名称
        String name = StringArgumentType.getString(context, "name");
        LocalizationKey key = KEY.then("comment");
        try {
            // 从本地文件中读取路径点对象
            Waypoint waypoint = Waypoint.load(server, name);
            boolean remove = false;
            if (comment == null || comment.isEmpty()) {
                comment = null;
                remove = true;
            }
            waypoint.setComment(comment);
            // 将路径点对象重新写入本地文件
            waypoint.save();
            if (remove) {
                // 移除路径点的说明文本
                MessageUtils.sendMessage(source, key.then("remove").translate(name));
            } else {
                // 为路径点添加说明文本
                MessageUtils.sendMessage(source, key.then("add").translate(comment, name));
            }
        } catch (IOException | NullPointerException e) {
            //无法添加说明文本
            CarpetOrgAddition.LOGGER.error("Failed to add description text for waypoint [{}]", name, e);
            throw CommandUtils.createException(key.then("add").then("error").translate(name));
        }
        return 1;
    }

    // 添加另一个坐标
    private int addAnotherPos(CommandContext<CommandSourceStack> context, @Nullable BlockPos blockPos) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        CommandSourceStack source = context.getSource();
        // 路径点的另一个坐标
        if (blockPos == null) {
            blockPos = player.blockPosition();
        }
        MinecraftServer server = context.getSource().getServer();
        // 路径点的名称
        String name = StringArgumentType.getString(context, "name");
        LocalizationKey key = KEY.then("another").then("add");
        try {
            // 从文件中读取路径点对象
            Waypoint waypoint = Waypoint.load(server, name);
            if (waypoint.canAddAnother()) {
                waypoint.setAnotherBlockPos(blockPos);
            } else {
                // 不能为末地添加对向坐标
                throw CommandUtils.createException(key.then("fail").translate());
            }
            // 将修改后的路径点重新写入本地文件
            waypoint.save();
            // 添加对向坐标
            MessageUtils.sendMessage(source, key.translate());
        } catch (IOException | NullPointerException e) {
            CarpetOrgAddition.LOGGER.error("Failed to parse waypoint [{}]", name, e);
            throw CommandUtils.createException(key.then("error").translate(name));
        }
        return 1;
    }

    // 删除路径点
    private int deleteWayPoint(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        //获取路径点文件名
        String name = StringArgumentType.getString(context, "name");
        //获取路径点文件对象
        WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), Waypoint.WAYPOINT);
        // 获取路径点文件的对象
        File file = worldFormat.file(name + IOUtils.JSON_EXTENSION);
        LocalizationKey key = KEY.then("remove");
        // 从本地文件删除路径点
        if (file.delete()) {
            // 成功删除
            MessageUtils.sendMessage(source, key.then("success").translate(name));
        } else {
            // 删除失败
            throw CommandUtils.createException(key.then("fail").translate(name));
        }
        return 1;
    }

    // 修改路径点
    private int setWayPoint(CommandContext<CommandSourceStack> context, @Nullable BlockPos blockPos) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        CommandSourceStack source = context.getSource();
        if (blockPos == null) {
            blockPos = player.blockPosition();
        }
        String fileName = StringArgumentType.getString(context, "name");
        LocalizationKey key = KEY.then("modify");
        try {
            Waypoint waypoint = Waypoint.load(context.getSource().getServer(), fileName);
            waypoint.setBlockPos(blockPos);
            // 将修改完坐标的路径点对象重新写入本地文件
            waypoint.save();
            // 发送命令执行后的反馈
            MessageUtils.sendMessage(source, key.translate(fileName));
        } catch (IOException | NullPointerException e) {
            throw CommandUtils.createException(key.then("error").translate(fileName));
        }
        return 1;
    }

    // 发送自己的位置
    private int sendSelfLocation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        BlockPos blockPos = player.blockPosition();
        Level world = ServerUtils.getWorld(player);
        LocalizationKey key = KEY.then("here");
        Component text = switch (ServerUtils.getWorldIdAsString(world)) {
            case ServerUtils.OVERWORLD -> key.then("cross").translate(
                    player.getDisplayName(),
                    LocalizationKeys.Dimension.OVERWORLD.translate(),
                    TextProvider.blockPos(blockPos, ChatFormatting.GREEN),
                    TextProvider.blockPos(MathUtils.getTheNetherPos(player), ChatFormatting.RED)
            );
            case ServerUtils.THE_NETHER -> key.then("cross").translate(
                    player.getDisplayName(),
                    LocalizationKeys.Dimension.THE_NETHER.translate(),
                    TextProvider.blockPos(blockPos, ChatFormatting.RED),
                    TextProvider.blockPos(MathUtils.getOverworldPos(player), ChatFormatting.GREEN)
            );
            case ServerUtils.THE_END -> key.translate(
                    player.getDisplayName(),
                    LocalizationKeys.Dimension.THE_END.translate(),
                    TextProvider.blockPos(blockPos, ChatFormatting.DARK_PURPLE)
            );
            default -> key.translate(
                    player.getDisplayName(),
                    ServerUtils.getWorldIdAsString(world),
                    TextProvider.blockPos(blockPos, null)
            );
        };
        MessageUtils.sendMessage(context.getSource().getServer(), text);
        return 1;
    }

    @Override
    public String getDefaultName() {
        return "locations";
    }
}
