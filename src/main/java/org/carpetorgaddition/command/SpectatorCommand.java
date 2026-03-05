package org.carpetorgaddition.command;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.*;
import org.carpetorgaddition.wheel.WorldFormat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

// 在生存模式和旁观模式间切换
public class SpectatorCommand extends AbstractServerCommand {
    private static final String SPECTATOR = "spectator";

    public SpectatorCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(CommandManager.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandSpectator))
                .executes(context -> setGameMode(context, false))
                .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                        .executes(context -> setGameMode(context, true)))
                .then(CommandManager.literal("teleport")
                        .then(CommandManager.literal("dimension")
                                .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                        .executes(this::tpToDimension)
                                        .then(CommandManager.argument("location", Vec3ArgumentType.vec3())
                                                .executes(this::tpToDimensionLocation))))
                        .then(CommandManager.literal("entity")
                                .then(CommandManager.argument("entity", EntityArgumentType.entity())
                                        .executes(this::tpToEntity)))));
    }

    // 更改游戏模式
    private int setGameMode(CommandContext<ServerCommandSource> context, boolean isFakePlayer) throws CommandSyntaxException {
        ServerPlayerEntity player = isFakePlayer
                ? CommandUtils.getArgumentFakePlayer(context)
                : CommandUtils.getSourcePlayer(context);
        // 如果玩家当前是旁观模式，就切换到生存模式，否则切换到旁观模式
        GameMode gameMode;
        if (player.isSpectator()) {
            gameMode = GameMode.SURVIVAL;
            if (!isFakePlayer) {
                // 假玩家切换游戏模式不需要回到原位置
                loadPlayerPos(FetcherUtils.getServer(player), player);
            }
        } else {
            gameMode = GameMode.SPECTATOR;
            if (isFakePlayer) {
                // 让假玩家切换旁观模式时向上移动0.2格
                // Mojang真的修复MC-146582了吗？（https://bugs.mojang.com/browse/MC-146582）
                player.requestTeleportOffset(0.0, 0.2, 0.0);
            } else {
                savePlayerPos(FetcherUtils.getServer(player), player);
            }
        }
        player.changeGameMode(gameMode);
        // 发送命令反馈
        Text text = gameMode.getTranslatableName();
        player.sendMessage(Text.translatable("commands.gamemode.success.self", text), true);
        return gameMode == GameMode.SURVIVAL ? 1 : 0;
    }

    // 传送到维度
    private int tpToDimension(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        this.requireSpectator(player);
        ServerWorld dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
        if (player.getEntityWorld().getRegistryKey() == World.OVERWORLD && dimension.getRegistryKey() == World.NETHER) {
            WorldUtils.teleport(player, dimension, player.getX() / 8, player.getY(), player.getZ() / 8, player.getYaw(), player.getPitch());
        } else if (player.getEntityWorld().getRegistryKey() == World.NETHER && dimension.getRegistryKey() == World.OVERWORLD) {
            WorldUtils.teleport(player, dimension, player.getX() * 8, player.getY(), player.getZ() * 8, player.getYaw(), player.getPitch());
        } else {
            WorldUtils.teleport(player, dimension, player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
        }
        // 发送命令反馈
        MessageUtils.sendMessage(context, "carpet.commands.spectator.teleport.success.dimension",
                player.getDisplayName(), WorldUtils.getDimensionId(dimension));
        return 1;
    }

    // 传送到维度的指定坐标
    private int tpToDimensionLocation(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 检查玩家是不是旁观模式
        requireSpectator(player);
        ServerWorld dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
        Vec3d location = Vec3ArgumentType.getVec3(context, "location");
        WorldUtils.teleport(player, dimension, location.getX(), location.getY(), location.getZ(), player.getYaw(), player.getPitch());
        // 发送命令反馈
        MessageUtils.sendMessage(context, "commands.teleport.success.location.single",
                player.getDisplayName(), formatFloat(location.getX()), formatFloat(location.getY()), formatFloat(location.getZ()));
        return 1;
    }

    // 传送到实体
    private int tpToEntity(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 检查玩家是不是旁观模式
        requireSpectator(player);
        Entity entity = EntityArgumentType.getEntity(context, "entity");
        WorldUtils.teleport(player, (ServerWorld) FetcherUtils.getWorld(entity), entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), entity.getPitch());
        // 发送命令反馈
        MessageUtils.sendMessage(context, "commands.teleport.success.entity.single",
                player.getDisplayName(), entity.getDisplayName());
        return 1;
    }

    // 检查玩家当前是不是旁观模式
    private void requireSpectator(ServerPlayerEntity player) throws CommandSyntaxException {
        if (player.isSpectator()) {
            return;
        }
        throw CommandUtils.createException("carpet.commands.spectator.teleport.fail", GameMode.SPECTATOR.getTranslatableName());
    }

    // 将玩家位置保存到文件
    private void savePlayerPos(MinecraftServer server, ServerPlayerEntity player) {
        WorldFormat worldFormat = new WorldFormat(server, SPECTATOR);
        JsonObject json = new JsonObject();
        json.addProperty("x", MathUtils.numberToTwoDecimalString(player.getX()));
        json.addProperty("y", MathUtils.numberToTwoDecimalString(player.getY()));
        json.addProperty("z", MathUtils.numberToTwoDecimalString(player.getZ()));
        json.addProperty("yaw", MathUtils.numberToTwoDecimalString(player.getYaw()));
        json.addProperty("pitch", MathUtils.numberToTwoDecimalString(player.getPitch()));
        json.addProperty("dimension", WorldUtils.getDimensionId(FetcherUtils.getWorld(player)));
        File file = worldFormat.file(player.getUuidAsString() + IOUtils.JSON_EXTENSION);
        try {
            IOUtils.write(file, json);
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.warn("Unable to write the location information of {} to the file normally", FetcherUtils.getPlayerName(player), e);
        }
    }

    // 从文件加载位置并传送玩家
    public void loadPlayerPos(MinecraftServer server, ServerPlayerEntity player) {
        WorldFormat worldFormat = new WorldFormat(server, SPECTATOR);
        File file = worldFormat.file(player.getUuidAsString() + IOUtils.JSON_EXTENSION);
        if (file.isFile()) {
            try {
                BufferedReader reader = IOUtils.toReader(file);
                try (reader) {
                    Gson gson = new Gson();
                    JsonObject json = gson.fromJson(reader, JsonObject.class);
                    double x = json.get("x").getAsDouble();
                    double y = json.get("y").getAsDouble();
                    double z = json.get("z").getAsDouble();
                    float yaw = json.get("yaw").getAsFloat();
                    float pitch = json.get("pitch").getAsFloat();
                    String dimension = json.get("dimension").getAsString();
                    ServerWorld world = WorldUtils.getWorld(server, dimension);
                    WorldUtils.teleport(player, world, x, y, z, yaw, pitch);
                }
                Files.deleteIfExists(file.toPath());
            } catch (IOException | NullPointerException e) {
                CarpetOrgAddition.LOGGER.warn("Unable to read the location information of {} normally", FetcherUtils.getPlayerName(player));
            }
        }
    }

    // 格式化坐标文本
    private String formatFloat(double d) {
        return String.format(Locale.ROOT, "%f", d);
    }

    @Override
    public String getDefaultName() {
        return "spectator";
    }
}
