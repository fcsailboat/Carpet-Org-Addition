package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.dataupdate.json.DataUpdater;
import boat.carpetorgaddition.dataupdate.json.SpectatorDataUpdater;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.WorldFormat;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.SpectateCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

// 在生存模式和旁观模式间切换
public class SpectatorCommand extends AbstractServerCommand {
    private static final int CURRENT_VERSION = 1;
    private static final String SPECTATOR = "spectator";
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("spectator");
    public static final LocalizationKey TELEPORT = KEY.then("teleport");

    public SpectatorCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(source -> CarpetOrgAdditionSettings.COMMAND_SPECTATOR.value().hasPermission(source))
                .executes(context -> setGameMode(context, false))
                .then(Commands.literal("player")
                        .then(Commands.argument(CommandUtils.PLAYER, EntityArgument.player())
                                .executes(context -> setGameMode(context, true))))
                .then(Commands.literal("teleport")
                        .then(Commands.literal("dimension")
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(this::tpToDimension)
                                        .then(Commands.argument("location", Vec3Argument.vec3())
                                                .executes(this::tpToDimensionLocation))))
                        .then(Commands.literal("entity")
                                .then(Commands.argument("entity", EntityArgument.entity())
                                        .executes(this::tpToEntity)))
                        .then(Commands.literal("location")
                                .then(Commands.argument("location", Vec3Argument.vec3())
                                        .executes(this::tpToLocation))))
                .then(Commands.literal("tp")
                        .then(Commands.argument("location", Vec3Argument.vec3())
                                .executes(this::tpToLocation))
                        .then(Commands.argument("entity", EntityArgument.entity())
                                .executes(this::tpToEntity)))
                .then(Commands.literal("spectate")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(this::spectateEntity))));
    }

    // 更改游戏模式
    private int setGameMode(CommandContext<CommandSourceStack> context, boolean fake) throws CommandSyntaxException {
        // TODO fake为true时目标玩家也不能是自己
        ServerPlayer player = fake ? CommandUtils.getArgumentFakePlayer(context) : CommandUtils.getSourcePlayer(context);
        // 如果玩家当前是旁观模式，就切换到生存模式，否则切换到旁观模式
        GameType gameMode = player.isSpectator() ? GameType.SURVIVAL : GameType.SPECTATOR;
        if (!fake) {
            MinecraftServer server = ServerUtils.getServer(player);
            if (player.isSpectator()) {
                this.loadAndTeleportPlayer(server, player);
            } else {
                this.savePlayerPos(server, player);
            }
        }
        player.setGameMode(gameMode);
        if (fake && player.isSpectator()) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(1.0, 0.0, 1.0));
        }
        // 发送命令反馈
        Component text = gameMode.getLongDisplayName();
        MessageUtils.sendMessageToHud(player, LocalizationKey.literal("commands.gamemode.success.self").translate(text));
        return gameMode == GameType.SURVIVAL ? 1 : 0;
    }

    // 传送到维度
    private int tpToDimension(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        this.requireSpectatorOrCreative(player);
        ServerLevel dimension = DimensionArgument.getDimension(context, "dimension");
        if (player.level().dimension() == Level.OVERWORLD && dimension.dimension() == Level.NETHER) {
            ServerUtils.teleport(player, dimension, player.getX() / 8, player.getY(), player.getZ() / 8, player.getYRot(), player.getXRot());
        } else if (player.level().dimension() == Level.NETHER && dimension.dimension() == Level.OVERWORLD) {
            ServerUtils.teleport(player, dimension, player.getX() * 8, player.getY(), player.getZ() * 8, player.getYRot(), player.getXRot());
        } else {
            ServerUtils.teleport(player, dimension, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        }
        // 发送命令反馈
        MessageUtils.sendMessage(context, TELEPORT.then("success").translate(player.getDisplayName(), ServerUtils.getIdAsString(dimension)));
        return 1;
    }

    // 传送到维度的指定坐标
    private int tpToDimensionLocation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        // 检查玩家是不是旁观模式
        requireSpectatorOrCreative(player);
        ServerLevel dimension = DimensionArgument.getDimension(context, "dimension");
        Vec3 location = Vec3Argument.getVec3(context, "location");
        ServerUtils.teleport(player, dimension, location.x(), location.y(), location.z(), player.getYRot(), player.getXRot());
        // 发送命令反馈
        MessageUtils.sendMessage(
                context,
                LocalizationKey.literal("commands.teleport.success.location.single").translate(
                        player.getDisplayName(),
                        formatFloat(location.x()),
                        formatFloat(location.y()),
                        formatFloat(location.z())
                )
        );
        return 1;
    }

    // 传送到实体
    private int tpToEntity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        // 检查玩家是不是旁观模式
        requireSpectatorOrCreative(player);
        Entity entity = EntityArgument.getEntity(context, "entity");
        ServerUtils.teleport(player, (ServerLevel) ServerUtils.getWorld(entity), entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
        // 发送命令反馈
        MessageUtils.sendMessage(
                context,
                LocalizationKey.literal("commands.teleport.success.entity.single").translate(
                        player.getDisplayName(),
                        entity.getDisplayName()
                )
        );
        return 1;
    }

    // 传送到指定坐标
    private int tpToLocation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        // 检查玩家是不是旁观模式
        requireSpectatorOrCreative(player);
        Vec3 location = Vec3Argument.getVec3(context, "location");
        ServerUtils.teleport(player, ServerUtils.getWorld(player), location.x(), location.y(), location.z(), player.getYRot(), player.getXRot());
        // 发送命令反馈
        MessageUtils.sendMessage(
                context,
                LocalizationKey.literal("commands.teleport.success.location.single").translate(
                        player.getDisplayName(),
                        formatFloat(location.x()),
                        formatFloat(location.y()),
                        formatFloat(location.z())
                )
        );
        return 1;
    }

    private int spectateEntity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "target");
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        if (entity == player) {
            throw SpectateCommand.ERROR_SELF.create();
        }
        if (player.isSpectator()) {
            player.setCamera(entity);
        } else {
            throw SpectateCommand.ERROR_NOT_SPECTATOR.create(player.getDisplayName());
        }
        return 1;
    }

    // 检查玩家当前是不是旁观模式
    private void requireSpectatorOrCreative(ServerPlayer player) throws CommandSyntaxException {
        if (player.isSpectator() || player.isCreative()) {
            return;
        }
        throw CommandUtils.createException(TELEPORT.then("fail")
                .translate(GameType.SPECTATOR.getShortDisplayName(), GameType.CREATIVE.getShortDisplayName()));
    }

    // 将玩家位置保存到文件
    private void savePlayerPos(MinecraftServer server, ServerPlayer player) {
        WorldFormat worldFormat = new WorldFormat(server, SPECTATOR);
        JsonObject json = new JsonObject();
        json.addProperty(DataUpdater.DATA_VERSION, CURRENT_VERSION);
        JsonObject pos = new JsonObject();
        pos.addProperty("x", player.getX());
        pos.addProperty("y", player.getY());
        pos.addProperty("z", player.getZ());
        json.add("pos", pos);
        JsonObject direction = new JsonObject();
        direction.addProperty("yaw", player.getYRot());
        direction.addProperty("pitch", player.getXRot());
        json.add("direction", direction);
        json.addProperty("dimension", ServerUtils.getIdAsString(ServerUtils.getWorld(player)));
        File file = worldFormat.file(player.getStringUUID() + IOUtils.JSON_EXTENSION);
        try {
            IOUtils.write(file, json);
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.warn("Unable to write the location information of {} to the file normally", PlayerUtils.getName(player), e);
        }
    }

    // 从文件加载位置并传送玩家
    public void loadAndTeleportPlayer(MinecraftServer server, ServerPlayer player) {
        WorldFormat worldFormat = new WorldFormat(server, SPECTATOR);
        File file = worldFormat.file(player.getStringUUID() + IOUtils.JSON_EXTENSION);
        if (file.isFile()) {
            try {
                JsonObject oldJson = IOUtils.readJson(file);
                int version = DataUpdater.getVersion(oldJson);
                SpectatorDataUpdater dataUpdater = SpectatorDataUpdater.getInstance();
                JsonObject json = dataUpdater.update(oldJson, version, CURRENT_VERSION);
                JsonObject pos = json.getAsJsonObject("pos");
                double x = pos.get("x").getAsDouble();
                double y = pos.get("y").getAsDouble();
                double z = pos.get("z").getAsDouble();
                JsonObject direction = json.getAsJsonObject("direction");
                float yaw = direction.get("yaw").getAsFloat();
                float pitch = direction.get("pitch").getAsFloat();
                String dimension = json.get("dimension").getAsString();
                ServerLevel world = ServerUtils.getWorld(server, dimension);
                ServerUtils.teleport(player, world, x, y, z, yaw, pitch);
                Files.deleteIfExists(file.toPath());
            } catch (IOException | NullPointerException e) {
                CarpetOrgAddition.LOGGER.warn("Unable to read the location information of {} normally", PlayerUtils.getName(player));
            }
        }
    }

    // 格式化坐标文本
    private String formatFloat(double value) {
        return MathUtils.formatToMaxTwoDecimals(value);
    }

    @Override
    public String getDefaultName() {
        return "spectator";
    }
}
