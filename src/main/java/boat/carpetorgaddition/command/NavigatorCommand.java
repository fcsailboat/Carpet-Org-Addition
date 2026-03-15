package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.Waypoint;
import boat.carpetorgaddition.wheel.permission.PermissionLevel;
import boat.carpetorgaddition.wheel.permission.PermissionManager;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class NavigatorCommand extends AbstractServerCommand {
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("navigate");
    /**
     * 开始导航文本
     */
    private static final LocalizationKey START_NAVIGATION = KEY.then("start");
    public static final LocalizationKey NAME = KEY.then("name");
    public static final LocalizationKey HUD = KEY.then("hud");

    public NavigatorCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.COMMAND_NAVIGATE))
                .then(Commands.literal("entity")
                        .then(Commands.argument("entity", EntityArgument.entity())
                                .executes(context -> navigateToEntity(context, false, "entity"))
                                .then(Commands.literal("continue")
                                        .executes(context -> navigateToEntity(context, true, "entity")))))
                .then(Commands.literal("player")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> navigateToEntity(context, false, "player"))
                                .then(Commands.literal("continue")
                                        .executes(context -> navigateToEntity(context, true, "player")))))
                .then(Commands.literal("waypoint")
                        .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.COMMAND_LOCATIONS))
                        .then(Commands.argument("waypoint", StringArgumentType.string())
                                .suggests(LocationsCommand.suggestion())
                                .executes(this::navigateToWaypoint)))
                .then(Commands.literal("stop")
                        .executes(this::stopNavigate))
                .then(Commands.literal("uuid")
                        .then(Commands.argument("uuid", UuidArgument.uuid())
                                .executes(this::navigateToEntityForUUID)))
                .then(Commands.literal("blockPos")
                        .then(Commands.argument("blockPos", BlockPosArgument.blockPos())
                                .executes(this::navigateToBlock)))
                .then(Commands.literal("spawnpoint")
                        .executes(this::navigateToSpawnPoint))
                .then(Commands.literal("death")
                        .requires(PermissionManager.register("navigate.death", PermissionLevel.PASS))
                        .executes(context -> navigateToLastDeathLocation(context, true))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> navigateToLastDeathLocation(context, false)))));
    }

    // 开始导航实体
    private int navigateToEntity(CommandContext<CommandSourceStack> context, boolean isContinue, String arguments) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        Entity entity = EntityArgument.getEntity(context, arguments);
        // 如果目标是玩家，广播消息
        TextBuilder builder = TextBuilder.of(START_NAVIGATION.translate(player.getDisplayName(), entity.getDisplayName()));
        PlayerComponentCoordinator.getCoordinator(player).getNavigatorManager().setNavigator(entity, isContinue);
        if (shouldBeBroadcast(entity, player)) {
            // 设置为斜体淡灰色
            builder.setGrayItalic();
            MessageUtils.sendMessage(context.getSource().getServer(), builder.build());
        } else {
            MessageUtils.sendMessage(context.getSource(), builder.build());
        }
        return 1;
    }

    // 开始导航到路径点
    private int navigateToWaypoint(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        MinecraftServer server = context.getSource().getServer();
        String waypointArgument = StringArgumentType.getString(context, "waypoint");
        try {
            Waypoint waypoint = Waypoint.load(server, waypointArgument);
            PlayerComponentCoordinator.getCoordinator(player).getNavigatorManager().setNavigator(waypoint);
            MessageUtils.sendMessage(context, START_NAVIGATION.translate(player.getDisplayName(), "[" + waypointArgument + "]"));
        } catch (IOException | RuntimeException e) {
            throw CommandUtils.createException(LocationsCommand.KEY.then("list").then("unable_to_parse").translate(waypointArgument));
        }
        return 1;
    }

    // 根据UUID获取实体并导航
    private int navigateToEntityForUUID(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        UUID uuid = UuidArgument.getUuid(context, "uuid");
        // 从服务器寻找这个UUID的实体
        MinecraftServer server = context.getSource().getServer();
        for (ServerLevel world : server.getAllLevels()) {
            Entity entity = world.getEntity(uuid);
            if (entity == null) {
                continue;
            }
            PlayerComponentCoordinator.getCoordinator(player).getNavigatorManager().setNavigator(entity, false);
            TextBuilder builder = TextBuilder.of(START_NAVIGATION.translate(player.getDisplayName(), entity.getDisplayName()));
            if (shouldBeBroadcast(entity, player)) {
                // 将字体设置为灰色斜体
                builder.setGrayItalic();
                MessageUtils.sendMessage(context.getSource().getServer(), builder.build());
            } else {
                MessageUtils.sendMessage(context.getSource(), builder.build());
            }
            return 1;
        }
        // 未找到实体
        throw EntityArgument.NO_ENTITIES_FOUND.create();
    }

    // 是否应该广播导航消息
    private boolean shouldBeBroadcast(Entity entity, ServerPlayer player) {
        if (entity == player || entity instanceof EntityPlayerMPFake) {
            return false;
        }
        return entity instanceof ServerPlayer;
    }

    // 停止导航
    private int stopNavigate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        PlayerComponentCoordinator.getCoordinator(player).getNavigatorManager().clearNavigator();
        MessageUtils.sendMessageToHud(player, HUD.then("stop").translate());
        return 1;
    }

    // 导航到指定坐标
    private int navigateToBlock(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        BlockPos blockPos = BlockPosArgument.getBlockPos(context, "blockPos");
        Level world = ServerUtils.getWorld(player);
        // 设置导航器，维度为玩家当前所在维度
        PlayerComponentCoordinator.getCoordinator(player).getNavigatorManager().setNavigator(blockPos, world);
        // 发送命令反馈
        Component pos = TextProvider.blockPos(blockPos, ServerUtils.getColor(world));
        Component name = player.getDisplayName();
        MessageUtils.sendMessage(context, START_NAVIGATION.translate(name, pos));
        return 1;
    }

    // 导航到重生点
    private int navigateToSpawnPoint(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        Component spawnPoint = NAME.then("spawnpoint").translate();
        try {
            LevelData.RespawnData respawnData = Objects.requireNonNull(player.getRespawnConfig()).respawnData();
            BlockPos respawnPos = respawnData.pos();
            ServerLevel world = ServerUtils.getServer(player).getLevel(respawnData.dimension());
            PlayerComponentCoordinator.getCoordinator(player).getNavigatorManager().setNavigator(respawnPos, world, spawnPoint);
        } catch (NullPointerException e) {
            throw CommandUtils.createException(KEY.then("unable_to_find").translate(player.getDisplayName(), spawnPoint));
        }
        MessageUtils.sendMessage(context, START_NAVIGATION.translate(player.getDisplayName(), spawnPoint));
        return 1;
    }

    // 导航到上一次死亡位置
    private int navigateToLastDeathLocation(CommandContext<CommandSourceStack> context, boolean self) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        ServerPlayer target = self ? player : CommandUtils.getArgumentPlayer(context);
        Optional<GlobalPos> lastDeathPos = target.getLastDeathLocation();
        // 导航器目标的名称
        Component death = NAME.then("death").translate();
        // 非空判断
        if (lastDeathPos.isEmpty()) {
            throw CommandUtils.createException(KEY.then("unable_to_find").translate(target.getDisplayName(), death));
        }
        Component name = self ? death : HUD.then("of").translate(target.getDisplayName(), death);
        // 获取死亡坐标和死亡维度
        GlobalPos globalPos = lastDeathPos.get();
        PlayerComponentCoordinator.getCoordinator(player).getNavigatorManager().setNavigator(globalPos.pos(),
                context.getSource().getServer().getLevel(globalPos.dimension()), name);
        TextBuilder builder = START_NAVIGATION.builder(player.getDisplayName(), name);
        if (self || player == target) {
            MessageUtils.sendMessage(player, builder.build());
        } else {
            builder.setGrayItalic();
            MessageUtils.sendMessage(context.getSource().getServer(), builder.build());
        }
        return 1;
    }

    @Override
    public String getDefaultName() {
        return "navigate";
    }
}
