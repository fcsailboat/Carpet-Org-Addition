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
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandSpectator))
                .executes(context -> setGameMode(context, false))
                .then(Commands.argument(CommandUtils.PLAYER, EntityArgument.player())
                        .executes(context -> setGameMode(context, true)))
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
                                        .executes(this::tpToLocation)))));
    }

    // 更改游戏模式
    private int setGameMode(CommandContext<CommandSourceStack> context, boolean isFakePlayer) throws CommandSyntaxException {
        ServerPlayer player = isFakePlayer
                ? CommandUtils.getArgumentFakePlayer(context)
                : CommandUtils.getSourcePlayer(context);
        // 如果玩家当前是旁观模式，就切换到生存模式，否则切换到旁观模式
        GameType gameMode;
        if (player.isSpectator()) {
            gameMode = GameType.SURVIVAL;
            if (!isFakePlayer) {
                // 假玩家切换游戏模式不需要回到原位置
                this.loadAndTeleportPlayer(ServerUtils.getServer(player), player);
            }
        } else {
            gameMode = GameType.SPECTATOR;
            if (isFakePlayer) {
                // 让假玩家切换旁观模式时向上移动0.2格
                // Mojang真的修复MC-146582了吗？（https://bugs.mojang.com/browse/MC-146582）
                player.teleportRelative(0.0, 0.2, 0.0);
            } else {
                savePlayerPos(ServerUtils.getServer(player), player);
            }
        }
        player.setGameMode(gameMode);
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
        MessageUtils.sendMessage(context, TELEPORT.then("success").translate(player.getDisplayName(), ServerUtils.getWorldIdAsString(dimension)));
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
        json.addProperty("dimension", ServerUtils.getWorldIdAsString(ServerUtils.getWorld(player)));
        File file = worldFormat.file(player.getStringUUID() + IOUtils.JSON_EXTENSION);
        try {
            IOUtils.write(file, json);
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.warn("Unable to write the location information of {} to the file normally", ServerUtils.getPlayerName(player), e);
        }
    }

    // 从文件加载位置并传送玩家
    public void loadAndTeleportPlayer(MinecraftServer server, ServerPlayer player) {
        WorldFormat worldFormat = new WorldFormat(server, SPECTATOR);
        File file = worldFormat.file(player.getStringUUID() + IOUtils.JSON_EXTENSION);
        if (file.isFile()) {
            try {
                JsonObject oldJson = IOUtils.loadJson(file);
                int version = DataUpdater.getVersion(oldJson);
                SpectatorDataUpdater dataUpdater = SpectatorDataUpdater.getInstance();
                JsonObject json = dataUpdater.update(oldJson, version);
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
                CarpetOrgAddition.LOGGER.warn("Unable to read the location information of {} normally", ServerUtils.getPlayerName(player));
            }
        }
    }

    // 格式化坐标文本
    private String formatFloat(double value) {
        return MathUtils.formatToMaxTwoDecimals(value);
    }

    @Override
    public String getDefaultName() {
        return "";
    }
}
