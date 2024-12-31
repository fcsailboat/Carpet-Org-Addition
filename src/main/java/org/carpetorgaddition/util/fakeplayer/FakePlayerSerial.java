package org.carpetorgaddition.util.fakeplayer;

import carpet.fakes.ServerPlayerInterface;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.*;
import org.carpetorgaddition.util.constant.TextConstants;
import org.carpetorgaddition.util.fakeplayer.actiondata.FakePlayerActionSerial;
import org.carpetorgaddition.util.task.ServerTaskManagerInterface;
import org.carpetorgaddition.util.task.playerscheduletask.DelayedLoginTask;
import org.carpetorgaddition.util.wheel.Annotation;
import org.carpetorgaddition.util.wheel.TextBuilder;
import org.carpetorgaddition.util.wheel.WorldFormat;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public class FakePlayerSerial {
    public static final String PLAYER_DATA = "player_data";
    /**
     * 玩家名称
     */
    private final String fakePlayerName;
    /**
     * 注释
     */
    private final Annotation annotation = new Annotation();
    /**
     * 位置
     */
    private final Vec3d playerPos;
    /**
     * 偏航角
     */
    private final float yaw;
    /**
     * 俯仰角
     */
    private final float pitch;
    private final String dimension;
    /**
     * 游戏模式
     */
    private final GameMode gameMode;
    /**
     * 是否飞行
     */
    private final boolean flying;
    /**
     * 是否潜行
     */
    private final boolean sneaking;
    /**
     * 是否自动登录
     */
    private boolean autologin = false;
    /**
     * 假玩家手部动作
     */
    private final EntityPlayerActionPackSerial interactiveAction;
    /**
     * 假玩家自动动作
     */
    private final FakePlayerActionSerial autoAction;

    public FakePlayerSerial(EntityPlayerMPFake fakePlayer) {
        this.fakePlayerName = fakePlayer.getName().getString();
        this.playerPos = fakePlayer.getPos();
        this.yaw = fakePlayer.getYaw();
        this.pitch = fakePlayer.getPitch();
        this.dimension = WorldUtils.getDimensionId(fakePlayer.getWorld());
        this.gameMode = fakePlayer.interactionManager.getGameMode();
        this.flying = fakePlayer.getAbilities().flying;
        this.sneaking = fakePlayer.isSneaking();
        this.interactiveAction = new EntityPlayerActionPackSerial(((ServerPlayerInterface) fakePlayer).getActionPack());
        this.autoAction = new FakePlayerActionSerial(fakePlayer);
    }

    public FakePlayerSerial(EntityPlayerMPFake fakePlayer, String annotation) {
        this(fakePlayer);
        this.annotation.setAnnotation(annotation);
    }

    public FakePlayerSerial(WorldFormat worldFormat, String name) throws IOException {
        JsonObject json = IOUtils.loadJson(worldFormat.getFile(name));
        // 玩家名
        this.fakePlayerName = IOUtils.removeExtension(name);
        // 玩家位置
        JsonObject pos = json.get("pos").getAsJsonObject();
        this.playerPos = new Vec3d(pos.get("x").getAsDouble(), pos.get("y").getAsDouble(), pos.get("z").getAsDouble());
        // 获取朝向
        JsonObject direction = json.get("direction").getAsJsonObject();
        this.yaw = direction.get("yaw").getAsFloat();
        this.pitch = direction.get("pitch").getAsFloat();
        // 维度
        this.dimension = json.get("dimension").getAsString();
        // 游戏模式
        this.gameMode = GameMode.byName(json.get("gamemode").getAsString());
        // 是否飞行
        this.flying = json.get("flying").getAsBoolean();
        // 是否潜行
        this.sneaking = json.get("sneaking").getAsBoolean();
        // 是否自动登录
        this.autologin = IOUtils.getJsonElement(json, "autologin", false, Boolean.class);
        // 注释
        this.annotation.setAnnotation(json);
        // 假玩家左右手动作
        if (json.has("hand_action")) {
            this.interactiveAction = new EntityPlayerActionPackSerial(json.get("hand_action").getAsJsonObject());
        } else {
            this.interactiveAction = EntityPlayerActionPackSerial.NO_ACTION;
        }
        // 假玩家动作，自动合成自动交易等
        if (json.has("script_action")) {
            this.autoAction = new FakePlayerActionSerial(json.get("script_action").getAsJsonObject());
        } else {
            this.autoAction = FakePlayerActionSerial.NO_ACTION;
        }
    }

    /**
     * 将当前对象保存到本地文件
     *
     * @return 如果是首次保存，返回0，如果是重新保存，返回1，如果未能保存，返回-1
     */
    public int save(CommandContext<ServerCommandSource> context, boolean resave) throws IOException {
        MinecraftServer server = context.getSource().getServer();
        WorldFormat worldFormat = new WorldFormat(server, PLAYER_DATA);
        String name = fakePlayerName;
        File file = worldFormat.file(name);
        // 玩家数据是否已存在
        boolean exists = file.exists();
        if (exists && !resave) {
            String command = "/playerManager resave " + name;
            // 在命令参数后面追加注释
            if (this.annotation.hasContent()) {
                command = command + " \"" + this.annotation + "\"";
            }
            // 单击执行命令
            MutableText clickResave = TextConstants.clickRun(command);
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.save.file_already_exist", clickResave);
            return -1;
        }
        IOUtils.saveJson(file, this.toJson());
        return exists ? 1 : 0;
    }

    // 从json加载并生成假玩家
    public void spawn(MinecraftServer server) throws CommandSyntaxException {
        if (server.getPlayerManager().getPlayer(this.fakePlayerName) != null) {
            throw CommandUtils.createException("carpet.commands.playerManager.spawn.player_exist");
        }
        // 生成假玩家
        EntityPlayerMPFake fakePlayer = GameUtils.createFakePlayer(this.fakePlayerName, server, this.playerPos, yaw, pitch,
                WorldUtils.getWorld(dimension), this.gameMode, flying);
        fakePlayer.setSneaking(sneaking);
        // 设置玩家动作
        this.interactiveAction.startAction(fakePlayer);
        this.autoAction.startAction(fakePlayer);
    }

    // 显示文本信息
    public Text info() {
        TextBuilder build = new TextBuilder();
        // 玩家位置
        String pos = MathUtils.numberToTwoDecimalString(this.playerPos.getX()) + " "
                + MathUtils.numberToTwoDecimalString(this.playerPos.getY()) + " "
                + MathUtils.numberToTwoDecimalString(this.playerPos.getZ());
        build.appendLine("carpet.commands.playerManager.info.pos",
                pos);
        // 获取朝向
        build.appendLine("carpet.commands.playerManager.info.direction",
                MathUtils.numberToTwoDecimalString(this.yaw),
                MathUtils.numberToTwoDecimalString(this.pitch));
        // 维度
        build.appendLine("carpet.commands.playerManager.info.dimension", switch (this.dimension) {
            case "minecraft:overworld", "overworld" -> TextConstants.OVERWORLD;
            case "minecraft:the_nether", "the_nether" -> TextConstants.THE_NETHER;
            case "minecraft:the_end", "the_end" -> TextConstants.THE_END;
            default -> TextUtils.createText(dimension);
        });
        // 游戏模式
        build.appendLine("carpet.commands.playerManager.info.gamemode", this.gameMode.getTranslatableName());
        // 是否飞行
        build.appendLine("carpet.commands.playerManager.info.flying", TextConstants.getBoolean(this.flying));
        // 是否潜行
        build.appendLine("carpet.commands.playerManager.info.sneaking", TextConstants.getBoolean(this.sneaking));
        // 是否自动登录
        build.append("carpet.commands.playerManager.info.autologin", TextConstants.getBoolean(this.autologin));
        if (this.interactiveAction.hasAction()) {
            build.newLine().append(this.interactiveAction.toText());
        }
        if (autoAction.hasAction()) {
            build.newLine().append(this.autoAction.toText());
        }
        if (this.annotation.hasContent()) {
            // 添加注释
            build.newLine().append("carpet.commands.playerManager.info.annotation", this.annotation.getText());
        }
        return build.toLine();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        // 玩家位置
        JsonObject pos = new JsonObject();
        pos.addProperty("x", this.playerPos.x);
        pos.addProperty("y", this.playerPos.y);
        pos.addProperty("z", this.playerPos.z);
        json.add("pos", pos);
        // 玩家朝向
        JsonObject direction = new JsonObject();
        direction.addProperty("yaw", this.yaw);
        direction.addProperty("pitch", this.pitch);
        json.add("direction", direction);
        // 维度
        json.addProperty("dimension", this.dimension);
        // 游戏模式
        json.addProperty("gamemode", this.gameMode.getName());
        // 是否飞行
        json.addProperty("flying", this.flying);
        // 是否潜行
        json.addProperty("sneaking", this.sneaking);
        // 自动登录
        json.addProperty("autologin", this.autologin);
        // 注释
        json.addProperty("annotation", this.annotation.getAnnotation());
        // 添加左键右键动作
        json.add("hand_action", interactiveAction.toJson());
        // 添加玩家动作
        json.add("script_action", this.autoAction.toJson());
        return json;
    }

    // 修改注释
    public void setAnnotation(@Nullable String annotation) {
        this.annotation.setAnnotation(annotation);
    }

    // 设置自动登录
    public void setAutologin(boolean autologin) {
        this.autologin = autologin;
    }

    // 获取玩家名
    public String getFakePlayerName() {
        return this.fakePlayerName;
    }

    // 获取显示名称
    public Text getDisplayName() {
        return TextUtils.hoverText(this.fakePlayerName, this.info());
    }

    // 列出每一条玩家信息
    public static int list(CommandContext<ServerCommandSource> context, WorldFormat worldFormat, Predicate<String> filter) {
        MutableText online = TextUtils.translate("carpet.commands.playerManager.click.online");
        MutableText offline = TextUtils.translate("carpet.commands.playerManager.click.offline");
        // 使用变量记录列出的数量，而不是直接使用集合的长度，因为集合中可能存在一些非json的文件，或者被损坏的json文件
        int count = 0;
        // 所有json文件
        List<File> jsonFileList = worldFormat.toImmutableFileList(WorldFormat.JSON_EXTENSIONS);
        for (File file : jsonFileList) {
            try {
                FakePlayerSerial serial = new FakePlayerSerial(worldFormat, file.getName());
                if (filter.test(serial.annotation.getAnnotation()) || filter.test(serial.fakePlayerName.toLowerCase(Locale.ROOT))) {
                    eachPlayer(context, file, online, offline, serial);
                    count++;
                }
            } catch (IOException | RuntimeException e) {
                CarpetOrgAddition.LOGGER.warn("无法从文件{}加载玩家信息", file.getName(), e);
            }
        }
        return count;
    }

    private static void eachPlayer(CommandContext<ServerCommandSource> context, File file, MutableText online, MutableText offline, FakePlayerSerial serial) {
        // 添加快捷命令
        String playerName = IOUtils.removeExtension(file.getName());
        String onlineCommand = "/playerManager spawn " + playerName;
        String offlineCommand = "/player " + playerName + " kill";
        MutableText mutableText = TextUtils.appendAll(
                TextUtils.command(TextUtils.createText("[↑]"), onlineCommand, online, Formatting.GREEN, false), " ",
                TextUtils.command(TextUtils.createText("[↓]"), offlineCommand, offline, Formatting.RED, false), " ",
                TextUtils.hoverText(TextUtils.createText("[?]"), serial.info(), Formatting.GRAY), " ",
                // 如果有注释，在列出的玩家的名字上也添加注释
                serial.annotation.hasContent() ? TextUtils.hoverText(playerName, serial.annotation.getText()) : playerName);
        // 发送消息
        MessageUtils.sendMessage(context.getSource(), mutableText);
    }

    /**
     * 假玩家自动登录
     */
    public static void autoLogin(MinecraftServer server) {
        ServerTaskManagerInterface instance = ServerTaskManagerInterface.getInstance(server);
        try {
            tryAutoLogin(server, instance);
        } catch (RuntimeException e) {
            CarpetOrgAddition.LOGGER.error("玩家自动登录出现意外错误", e);
        }
    }

    private static void tryAutoLogin(MinecraftServer server, ServerTaskManagerInterface instance) {
        WorldFormat worldFormat = new WorldFormat(server, FakePlayerSerial.PLAYER_DATA);
        List<File> files = worldFormat.toImmutableFileList(WorldFormat.JSON_EXTENSIONS);
        int count = server.getCurrentPlayerCount();
        for (File file : files) {
            FakePlayerSerial fakePlayerSerial;
            try {
                fakePlayerSerial = new FakePlayerSerial(worldFormat, file.getName());
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.error("无法读取{}玩家数据", IOUtils.removeExtension(file.getName()), e);
                continue;
            }
            if (fakePlayerSerial.autologin) {
                instance.addTask(new DelayedLoginTask(server, fakePlayerSerial, 1));
                count++;
                // 阻止假玩家把玩家上线占满，至少为一名真玩家保留一个名额
                if (count >= server.getMaxPlayerCount() - 1) {
                    CarpetOrgAddition.LOGGER.warn("服务器玩家即将达到上限");
                    return;
                }
            }
        }
    }
}
