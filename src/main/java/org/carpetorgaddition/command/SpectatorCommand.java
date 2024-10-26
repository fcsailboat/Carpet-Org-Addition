package org.carpetorgaddition.command;

import carpet.utils.CommandHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.*;
import org.carpetorgaddition.util.wheel.WorldFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

// 在生存模式和旁观模式间切换
public class SpectatorCommand {
    private static final String SPECTATOR = "spectator";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("spectator")
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandSpectator))
                .executes(context -> setGameMode(context, false))
                .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                        .executes(context -> setGameMode(context, true)))
                .then(CommandManager.literal("teleport")
                        .then(CommandManager.literal("dimension")
                                .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                        .executes(SpectatorCommand::tpToDimension)
                                        .then(CommandManager.argument("location", Vec3ArgumentType.vec3())
                                                .executes(SpectatorCommand::tpToDimensionLocation))))
                        .then(CommandManager.literal("entity")
                                .then(CommandManager.argument("entity", EntityArgumentType.entity())
                                        .executes(SpectatorCommand::tpToEntity)))));
    }

    // 更改游戏模式
    private static int setGameMode(CommandContext<ServerCommandSource> context, boolean isFakePlayer) throws CommandSyntaxException {
        ServerPlayerEntity player = isFakePlayer
                ? CommandUtils.getArgumentFakePlayer(context)
                : CommandUtils.getSourcePlayer(context);
        // 如果玩家当前是旁观模式，就切换到生存模式，否则切换到旁观模式
        GameMode gameMode;
        if (player.isSpectator()) {
            gameMode = GameMode.SURVIVAL;
            if (!isFakePlayer) {
                // 假玩家切换游戏模式不需要回到原位置
                loadPlayerPos(player.getServer(), player);
            }
        } else {
            gameMode = GameMode.SPECTATOR;
            if (isFakePlayer) {
                // 让假玩家切换旁观模式时向上移动0.2格
                // Mojang真的修复MC-146582了吗？（https://bugs.mojang.com/browse/MC-146582）
                player.requestTeleportOffset(0.0, 0.2, 0.0);
            } else {
                savePlayerPos(player.getServer(), player);
            }
        }
        player.changeGameMode(gameMode);
        // 发送命令反馈
        MutableText text = Text.translatable("gameMode." + gameMode.getName());
        player.sendMessage(Text.translatable("commands.gamemode.success.self", text), true);
        return gameMode == GameMode.SURVIVAL ? 1 : 0;
    }

    // 传送到维度
    private static int tpToDimension(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        requireSpectator(player);
        ServerWorld dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
        if (player.getWorld().getRegistryKey() == World.OVERWORLD && dimension.getRegistryKey() == World.NETHER) {
            player.teleport(dimension, player.getX() / 8, player.getY(), player.getZ() / 8, player.getYaw(), player.getPitch());
        } else if (player.getWorld().getRegistryKey() == World.NETHER && dimension.getRegistryKey() == World.OVERWORLD) {
            player.teleport(dimension, player.getX() * 8, player.getY(), player.getZ() * 8, player.getYaw(), player.getPitch());
        } else {
            player.teleport(dimension, player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
        }
        // 发送命令反馈
        MessageUtils.sendMessage(context, "carpet.commands.spectator.teleport.success.dimension",
                player.getDisplayName(), WorldUtils.getDimensionId(dimension));
        return 1;
    }

    // 传送到维度的指定坐标
    private static int tpToDimensionLocation(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 检查玩家是不是旁观模式
        requireSpectator(player);
        ServerWorld dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
        Vec3d location = Vec3ArgumentType.getVec3(context, "location");
        player.teleport(dimension, location.getX(), location.getY(), location.getZ(), player.getYaw(), player.getPitch());
        // 发送命令反馈
        MessageUtils.sendMessage(context, "commands.teleport.success.location.single",
                player.getDisplayName(), formatFloat(location.getX()), formatFloat(location.getY()), formatFloat(location.getZ()));
        return 1;
    }

    // 传送到实体
    private static int tpToEntity(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 检查玩家是不是旁观模式
        requireSpectator(player);
        Entity entity = EntityArgumentType.getEntity(context, "entity");
        player.teleport((ServerWorld) entity.getWorld(), entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), entity.getPitch());
        // 发送命令反馈
        MessageUtils.sendMessage(context, "commands.teleport.success.entity.single",
                player.getDisplayName(), entity.getDisplayName());
        return 1;
    }

    // 检查玩家当前是不是旁观模式
    private static void requireSpectator(ServerPlayerEntity player) throws CommandSyntaxException {
        if (player.isSpectator()) {
            return;
        }
        throw CommandUtils.createException("carpet.commands.spectator.teleport.fail",
                Text.translatable("gameMode." + GameMode.SPECTATOR.getName()));
    }

    // 将玩家位置保存到文件
    private static void savePlayerPos(MinecraftServer server, ServerPlayerEntity player) {
        WorldFormat worldFormat = new WorldFormat(server, SPECTATOR);
        JsonObject json = new JsonObject();
        json.addProperty("x", MathUtils.keepTwoDecimalPlaces(player.getX()));
        json.addProperty("y", MathUtils.keepTwoDecimalPlaces(player.getY()));
        json.addProperty("z", MathUtils.keepTwoDecimalPlaces(player.getZ()));
        json.addProperty("yaw", MathUtils.keepTwoDecimalPlaces(player.getYaw()));
        json.addProperty("pitch", MathUtils.keepTwoDecimalPlaces(player.getPitch()));
        json.addProperty("dimension", WorldUtils.getDimensionId(player.getWorld()));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(json, JsonObject.class);
        File file = worldFormat.file(player.getUuidAsString() + IOUtils.JSON_EXTENSION);
        try {
            try (BufferedWriter writer = IOUtils.toWriter(file)) {
                writer.write(jsonString);
            }
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.warn("无法正常将{}的位置信息写入文件", GameUtils.getPlayerName(player), e);
        }
    }

    // 从文件加载位置并传送玩家
    private static void loadPlayerPos(MinecraftServer server, ServerPlayerEntity player) {
        WorldFormat worldFormat = new WorldFormat(server, SPECTATOR);
        File file = worldFormat.file(player.getUuidAsString() + IOUtils.JSON_EXTENSION);
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
                player.teleport(world, x, y, z, yaw, pitch);
            }
        } catch (IOException | NullPointerException e) {
            CarpetOrgAddition.LOGGER.warn("无法正常读取{}的位置信息", GameUtils.getPlayerName(player));
        }
    }

    // 格式化坐标文本
    private static String formatFloat(double d) {
        return String.format(Locale.ROOT, "%f", d);
    }
}
