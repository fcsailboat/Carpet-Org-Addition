package org.carpetorgaddition.command;

import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.CommandExecuteIOException;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.carpetorgaddition.util.fakeplayer.FakePlayerSafeAfkInterface;
import org.carpetorgaddition.util.fakeplayer.FakePlayerSerial;
import org.carpetorgaddition.util.task.ServerTask;
import org.carpetorgaddition.util.task.ServerTaskManagerInterface;
import org.carpetorgaddition.util.task.playerscheduletask.DelayedLoginTask;
import org.carpetorgaddition.util.task.playerscheduletask.DelayedLogoutTask;
import org.carpetorgaddition.util.task.playerscheduletask.PlayerScheduleTask;
import org.carpetorgaddition.util.task.playerscheduletask.ReLoginTask;
import org.carpetorgaddition.util.wheel.WorldFormat;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;

public class PlayerManagerCommand {

    private static final String SAFEAFK_PROPERTIES = "safeafk.properties";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // 延迟登录节点
        RequiredArgumentBuilder<ServerCommandSource, Integer> loginNode = CommandManager.argument("delayed", IntegerArgumentType.integer(1));
        for (TimeUnit unit : TimeUnit.values()) {
            // 添加时间单位
            loginNode.then(CommandManager.literal(unit.getName())
                    .executes(context -> addDelayedLoginTask(context, unit)));
        }
        // 延迟登出节点
        RequiredArgumentBuilder<ServerCommandSource, Integer> logoutNode = CommandManager.argument("delayed", IntegerArgumentType.integer(1));
        for (TimeUnit unit : TimeUnit.values()) {
            logoutNode.then(CommandManager.literal(unit.getName())
                    .executes(context -> addDelayedLogoutTask(context, unit)));
        }
        dispatcher.register(CommandManager.literal("playerManager")
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandPlayerManager))
                .then(CommandManager.literal("save")
                        .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                .executes(context -> savePlayer(context, false))
                                .then(CommandManager.argument("annotation", StringArgumentType.string())
                                        .executes(context -> withAnnotationSavePlayer(context, false)))))
                .then(CommandManager.literal("spawn")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests(defaultSuggests())
                                .executes(PlayerManagerCommand::spawnPlayer)))
                .then(CommandManager.literal("annotation")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests(defaultSuggests())
                                .executes(context -> setAnnotation(context, true))
                                .then(CommandManager.argument("annotation", StringArgumentType.string())
                                        .executes(context -> setAnnotation(context, false)))))
                .then(CommandManager.literal("autologin")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests(defaultSuggests())
                                .then(CommandManager.argument("autologin", BoolArgumentType.bool())
                                        .executes(PlayerManagerCommand::setAutoLogin))))
                .then(CommandManager.literal("resave")
                        .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                .executes(context -> savePlayer(context, true))
                                .then(CommandManager.argument("annotation", StringArgumentType.string())
                                        .executes(context -> withAnnotationSavePlayer(context, true)))))
                .then(CommandManager.literal("list")
                        .executes(context -> list(context, s -> true))
                        .then(CommandManager.argument("filter", StringArgumentType.string())
                                .executes(context -> list(context, s -> s.contains(StringArgumentType.getString(context, "filter"))))))
                .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests(defaultSuggests())
                                .executes(PlayerManagerCommand::delete)))
                .then(CommandManager.literal("schedule")
                        .then(CommandManager.literal("relogin")
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .suggests(reLoginTaskSuggests())
                                        .then(CommandManager.argument("interval", IntegerArgumentType.integer(1))
                                                .executes(PlayerManagerCommand::setReLogin))
                                        .then(CommandManager.literal("stop")
                                                .executes(PlayerManagerCommand::stopReLogin))))
                        .then(CommandManager.literal("login")
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .suggests(defaultSuggests())
                                        .then(loginNode)))
                        .then(CommandManager.literal("logout")
                                .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                        .then(logoutNode)))
                        .then(CommandManager.literal("cancel")
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .suggests(cancelSuggests())
                                        .executes(PlayerManagerCommand::cancelScheduleTask)))
                        .then(CommandManager.literal("list")
                                .executes(PlayerManagerCommand::listScheduleTask)))
                .then(CommandManager.literal("safeafk")
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                        .executes(context -> safeAfk(context, 5F, false))
                                        .then(CommandManager.argument("threshold", FloatArgumentType.floatArg())
                                                .executes(context -> safeAfk(context, FloatArgumentType.getFloat(context, "threshold"), false))
                                                .then(CommandManager.argument("save", BoolArgumentType.bool())
                                                        .executes(context -> safeAfk(context, FloatArgumentType.getFloat(context, "threshold"), BoolArgumentType.getBool(context, "save")))))))
                        .then(CommandManager.literal("list")
                                .executes(PlayerManagerCommand::listSafeAfk))
                        .then(CommandManager.literal("cancel")
                                .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                        .executes(context -> cancelSafeAfk(context, false))
                                        .then(CommandManager.argument("save", BoolArgumentType.bool())
                                                .executes(context -> cancelSafeAfk(context, true)))))
                        .then(CommandManager.literal("query")
                                .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                        .executes(PlayerManagerCommand::querySafeAfk)))));
    }

    // cancel子命令自动补全
    private static @NotNull SuggestionProvider<ServerCommandSource> cancelSuggests() {
        return (context, builder) -> {
            MinecraftServer server = context.getSource().getServer();
            ServerTaskManagerInterface instance = ServerTaskManagerInterface.getInstance(server);
            ArrayList<String> list = new ArrayList<>();
            // 将任务的玩家名添加到集合
            instance.findTask(PlayerScheduleTask.class, task -> true).forEach(task -> list.add(task.getPlayerName()));
            return CommandSource.suggestMatching(list, builder);
        };
    }

    // 自动补全玩家名
    private static SuggestionProvider<ServerCommandSource> defaultSuggests() {
        return (context, builder) -> CommandSource.suggestMatching(new WorldFormat(context.getSource().getServer(),
                FakePlayerSerial.PLAYER_DATA).toImmutableFileList().stream()
                .filter(file -> file.getName().endsWith(IOUtils.JSON_EXTENSION))
                .map(file -> IOUtils.removeExtension(file.getName()))
                .map(StringArgumentType::escapeIfRequired), builder);
    }

    // relogin子命令自动补全
    public static @NotNull SuggestionProvider<ServerCommandSource> reLoginTaskSuggests() {
        return (context, builder) -> {
            MinecraftServer server = context.getSource().getServer();
            ServerTaskManagerInterface instance = ServerTaskManagerInterface.getInstance(server);
            // 所有正在周期性上下线的玩家
            List<String> taskList = instance.getTaskList().stream()
                    .filter(task -> task instanceof ReLoginTask)
                    .map(task -> ((ReLoginTask) task).getPlayerName()).toList();
            // 所有在线玩家
            List<String> onlineList = server.getPlayerManager().getPlayerList().stream()
                    .map(player -> player.getName().getString()).toList();
            HashSet<String> players = new HashSet<>();
            players.addAll(taskList);
            players.addAll(onlineList);
            return CommandSource.suggestMatching(players.stream(), builder);
        };
    }

    // 安全挂机
    private static int safeAfk(CommandContext<ServerCommandSource> context, float threshold, boolean save) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 假玩家安全挂机阈值必须小于玩家最大生命值
        if (threshold >= fakePlayer.getMaxHealth()) {
            throw CommandUtils.createException("carpet.commands.playerManager.safeafk.threshold_too_high");
        }
        // 低于或等于0的值没有实际意义，统一设置为-1
        if (threshold <= 0F) {
            threshold = -1F;
        }
        // 设置安全挂机阈值
        FakePlayerSafeAfkInterface safeAfk = (FakePlayerSafeAfkInterface) fakePlayer;
        safeAfk.setHealthThreshold(threshold);
        if (save) {
            try {
                saveSafeAfkThreshold(context, threshold, fakePlayer);
            } catch (IOException e) {
                throw CommandExecuteIOException.of(e);
            }
        } else {
            String command = "/playerManager safeafk set " + fakePlayer.getName().getString() + " " + threshold + " true";
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.safeafk.successfully_set_up",
                    fakePlayer.getDisplayName(), threshold, TextConstants.clickRun(command));
        }
        return (int) threshold;
    }

    // 列出所有设置了安全挂机的在线假玩家
    private static int listSafeAfk(CommandContext<ServerCommandSource> context) {
        List<ServerPlayerEntity> list = context.getSource().getServer().getPlayerManager().getPlayerList()
                .stream().filter(player -> player instanceof EntityPlayerMPFake).toList();
        int count = 0;
        // 遍历所有在线并且设置了安全挂机的假玩家
        for (ServerPlayerEntity player : list) {
            float threshold = ((FakePlayerSafeAfkInterface) player).getHealthThreshold();
            if (threshold < 0) {
                continue;
            }
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.safeafk.list.each",
                    player.getDisplayName(), threshold);
            count++;
        }
        // 没有玩家被列出
        if (count == 0) {
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.safeafk.list.empty");
        }
        return count;
    }

    // 取消假玩家的安全挂机
    private static int cancelSafeAfk(CommandContext<ServerCommandSource> context, boolean remove) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 设置安全挂机阈值
        FakePlayerSafeAfkInterface safeAfk = (FakePlayerSafeAfkInterface) fakePlayer;
        safeAfk.setHealthThreshold(-1);
        if (remove) {
            try {
                saveSafeAfkThreshold(context, -1, fakePlayer);
            } catch (IOException e) {
                throw CommandExecuteIOException.of(e);
            }
        } else {
            String key = "carpet.commands.playerManager.safeafk.successfully_set_up.cancel";
            MutableText command = TextConstants.clickRun("/playerManager safeafk set " + fakePlayer.getName().getString() + " -1 true");
            MessageUtils.sendMessage(context, key, fakePlayer.getDisplayName(), command);
        }
        return 1;
    }

    // 查询指定玩家的安全挂机阈值
    private static int querySafeAfk(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        float threshold = ((FakePlayerSafeAfkInterface) fakePlayer).getHealthThreshold();
        String key = "carpet.commands.playerManager.safeafk.list.each";
        MessageUtils.sendMessage(context, key, fakePlayer.getDisplayName(), threshold);
        return (int) threshold;
    }

    // 保存或删除安全挂机阈值
    private static void saveSafeAfkThreshold(CommandContext<ServerCommandSource> context, float threshold,
                                             EntityPlayerMPFake fakePlayer) throws IOException {
        String playerName = fakePlayer.getName().getString();
        WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), null);
        File file = worldFormat.file(SAFEAFK_PROPERTIES);
        // 文件存在或者文件成功创建
        if (file.isFile() || file.createNewFile()) {
            Properties properties = new Properties();
            BufferedReader reader = IOUtils.toReader(file);
            try (reader) {
                properties.load(reader);
            }
            if (threshold > 0) {
                // 将玩家安全挂机阈值保存到配置文件
                properties.setProperty(playerName, String.valueOf(threshold));
                MessageUtils.sendMessage(context, "carpet.commands.playerManager.safeafk.successfully_set_up.save", fakePlayer.getDisplayName(), threshold);
            } else {
                // 将玩家安全挂机阈值从配置文件中删除
                properties.remove(playerName);
                MessageUtils.sendMessage(context, "carpet.commands.playerManager.safeafk.successfully_set_up.remove", fakePlayer.getDisplayName());
            }
            BufferedWriter writer = IOUtils.toWriter(file);
            try (writer) {
                properties.store(writer, null);
            }
        }
    }

    /**
     * 加载安全挂机阈值
     */
    public static void loadSafeAfk(ServerPlayerEntity player) {
        if (player instanceof EntityPlayerMPFake) {
            WorldFormat worldFormat = new WorldFormat(player.server, null);
            File file = worldFormat.file(SAFEAFK_PROPERTIES);
            // 文件必须存在
            if (file.isFile()) {
                Properties properties = new Properties();
                try {
                    BufferedReader reader = IOUtils.toReader(file);
                    try (reader) {
                        properties.load(reader);
                    }
                } catch (IOException e) {
                    CarpetOrgAddition.LOGGER.error("假玩家安全挂机阈值加载时出错", e);
                    return;
                }
                try {
                    // 设置安全挂机阈值
                    FakePlayerSafeAfkInterface safeAfk = (FakePlayerSafeAfkInterface) player;
                    String value = properties.getProperty(player.getName().getString());
                    if (value == null) {
                        return;
                    }
                    float threshold = Float.parseFloat(value);
                    safeAfk.setHealthThreshold(threshold);
                    // 广播阈值设置的消息
                    String key = "carpet.commands.playerManager.safeafk.successfully_set_up.auto";
                    MutableText message = TextUtils.translate(key, player.getDisplayName(), threshold);
                    MessageUtils.broadcastMessage(player, TextUtils.toGrayItalic(message));
                } catch (NumberFormatException e) {
                    CarpetOrgAddition.LOGGER.error("{}安全挂机阈值设置失败", player.getName().getString(), e);
                }
            }
        }
    }

    // 列出每一个玩家
    private static int list(CommandContext<ServerCommandSource> context, Predicate<String> filter) {
        WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), FakePlayerSerial.PLAYER_DATA);
        int count = FakePlayerSerial.list(context, worldFormat, filter);
        if (count == 0) {
            // 没有玩家被列出
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.list.no_player");
            return 0;
        }
        return count;
    }

    // 保存假玩家数据
    private static int savePlayer(CommandContext<ServerCommandSource> context, boolean resave) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerSerial fakePlayerSerial = new FakePlayerSerial(fakePlayer);
        savePlayer(context, fakePlayerSerial, fakePlayer, resave);
        return 1;
    }

    // 保存玩家带注释
    private static int withAnnotationSavePlayer(CommandContext<ServerCommandSource> context, boolean resave) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        String annotation = StringArgumentType.getString(context, "annotation");
        FakePlayerSerial fakePlayerSerial = new FakePlayerSerial(fakePlayer, annotation);
        return savePlayer(context, fakePlayerSerial, fakePlayer, resave);
    }

    // 设置注释
    private static int setAnnotation(CommandContext<ServerCommandSource> context, boolean remove) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), FakePlayerSerial.PLAYER_DATA);
        // 修改注释
        String annotation = remove ? null : StringArgumentType.getString(context, "annotation");
        FakePlayerSerial serial;
        try {
            serial = new FakePlayerSerial(worldFormat, name);
            serial.setAnnotation(annotation);
            // 将玩家信息重新保存的本地文件
            serial.save(context, true);
        } catch (FileNotFoundException e) {
            throw CommandUtils.createException("carpet.commands.playerManager.cannot_find_file", name);
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
        // 发送命令反馈
        if (remove) {
            // 移除注释
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.annotation.remove", serial.getDisplayName());
        } else {
            // 修改注释
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.annotation.modify", serial.getDisplayName(), annotation);
        }
        return 1;
    }

    // 设置自动登录
    private static int setAutoLogin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), FakePlayerSerial.PLAYER_DATA);
        String name = StringArgumentType.getString(context, "name");
        boolean autologin = BoolArgumentType.getBool(context, "autologin");
        FakePlayerSerial serial;
        try {
            serial = new FakePlayerSerial(worldFormat, name);
            // 设置自动登录
            serial.setAutologin(autologin);
            serial.save(context, true);
        } catch (FileNotFoundException e) {
            throw CommandUtils.createException("carpet.commands.playerManager.cannot_find_file", name);
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
        // 发送命令反馈
        if (autologin) {
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.autologin.setup", serial.getDisplayName());
        } else {
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.autologin.cancel", serial.getDisplayName());
        }
        return 1;
    }

    // 保存玩家
    private static int savePlayer(CommandContext<ServerCommandSource> context, FakePlayerSerial fakePlayerSerial, EntityPlayerMPFake fakePlayer, boolean resave) throws CommandSyntaxException {
        try {
            int result = fakePlayerSerial.save(context, resave);
            if (result == 0) {
                // 首次保存
                MessageUtils.sendMessage(context.getSource(),
                        "carpet.commands.playerManager.save.success",
                        fakePlayer.getDisplayName());
            } else if (result == 1) {
                // 重新保存
                MessageUtils.sendMessage(context.getSource(),
                        "carpet.commands.playerManager.save.resave",
                        fakePlayer.getDisplayName());
            }
            // 成功保存玩家时，命令返回1，未能保存玩家时，命令返回0
            return result < 0 ? 0 : 1;
        } catch (IOException e) {
            throw CommandUtils.createException("carpet.commands.playerManager.save.fail", fakePlayer.getDisplayName());
        }
    }

    // 生成假玩家
    private static int spawnPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), FakePlayerSerial.PLAYER_DATA);
        try {
            FakePlayerSerial serial = new FakePlayerSerial(worldFormat, name);
            // 生成假玩家
            serial.spawn(context.getSource().getServer());
        } catch (FileNotFoundException e) {
            throw CommandUtils.createException("carpet.commands.playerManager.cannot_find_file", name);
        } catch (RuntimeException | IOException e) {
            // 尝试生成假玩家时出现意外问题
            throw CommandUtils.createException(e, "carpet.commands.playerManager.spawn.fail");
        }
        return 1;
    }

    // 删除玩家信息
    private static int delete(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), FakePlayerSerial.PLAYER_DATA);
        String name = StringArgumentType.getString(context, "name");
        File file = worldFormat.getFile(name);
        // 文件存在且文件删除成功
        if (file.isFile() && file.delete()) {
            MessageUtils.sendMessage(context.getSource(), "carpet.commands.playerManager.delete.success");
        } else {
            throw CommandUtils.createException("carpet.commands.playerManager.delete.fail");
        }
        return 1;
    }

    // 设置不断重新上线下线
    private static int setReLogin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // 强制启用内存泄漏修复
        if (fixMemoryLeak(context)) {
            // 获取目标假玩家名
            String name = StringArgumentType.getString(context, "name");
            int interval = IntegerArgumentType.getInteger(context, "interval");
            MinecraftServer server = context.getSource().getServer();
            ServerTaskManagerInterface instance = ServerTaskManagerInterface.getInstance(server);
            // 如果任务存在，修改任务，否则添加任务
            ReLoginTask task = getReLoginTask(instance, name);
            if (task == null) {
                // 添加任务
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
                if (player == null) {
                    // 玩家不存在
                    throw CommandUtils.createException("argument.entity.notfound.player");
                } else {
                    // 目标玩家不是假玩家
                    CommandUtils.checkFakePlayer(player);
                }
                instance.addTask(new ReLoginTask(name, interval, server, player.getServerWorld().getRegistryKey(), context));
            } else {
                // 修改周期时间
                task.setInterval(interval);
                MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.relogin.set_interval", name, interval);
            }
            return interval;
        }
        return 0;
    }

    // 启用内存泄漏修复
    private static boolean fixMemoryLeak(CommandContext<ServerCommandSource> context) {
        if (CarpetOrgAdditionSettings.fakePlayerSpawnMemoryLeakFix) {
            return true;
        }
        // 文本内容：[这里]
        MutableText here = TextUtils.translate("carpet.command.text.click.here");
        // 单击后输入的命令
        String command = "/carpet fakePlayerSpawnMemoryLeakFix true";
        // [这里]的悬停提示
        MutableText input = TextUtils.translate("carpet.command.text.click.input", command);
        here = TextUtils.suggest(here, command, input, Formatting.AQUA);
        MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.relogin.condition", here);
        return false;
    }

    // 获取假玩家周期上下线任务
    private static ReLoginTask getReLoginTask(ServerTaskManagerInterface instance, String name) {
        List<ReLoginTask> list = instance.getTaskList().stream()
                .filter(task -> task instanceof ReLoginTask)
                .map(task -> (ReLoginTask) task).toList();
        for (ReLoginTask task : list) {
            if (Objects.equals(task.getPlayerName(), name)) {
                return task;
            }
        }
        return null;
    }

    // 停止重新上线下线
    private static int stopReLogin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // 获取目标假玩家名
        String name = StringArgumentType.getString(context, "name");
        ServerTaskManagerInterface instance = ServerTaskManagerInterface.getInstance(context.getSource().getServer());
        List<ReLoginTask> list = instance.findTask(ReLoginTask.class, task -> Objects.equals(task.getPlayerName(), name));
        if (list.isEmpty()) {
            throw CommandUtils.createException("carpet.commands.playerManager.schedule.cancel.fail");
        }
        list.forEach(task -> {
            instance.getTaskList().remove(task);
            task.onCancel(context);
        });
        return 1;
    }

    // 延时上线
    private static int addDelayedLoginTask(CommandContext<ServerCommandSource> context, TimeUnit unit) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ServerTaskManagerInterface instance = ServerTaskManagerInterface.getInstance(server);
        String name = StringArgumentType.getString(context, "name");
        // 等待时间
        long tick = unit.getDelayed(context);
        List<DelayedLoginTask> list = instance.findTask(DelayedLoginTask.class, loginTask -> Objects.equals(name, loginTask.getPlayerName()));
        MutableText time = TextUtils.hoverText(TextConstants.tickToTime(tick), TextConstants.tickToRealTime(tick));
        if (list.isEmpty()) {
            // 添加上线任务
            WorldFormat worldFormat = new WorldFormat(server, FakePlayerSerial.PLAYER_DATA);
            FakePlayerSerial serial;
            try {
                serial = new FakePlayerSerial(worldFormat, name);
            } catch (IOException e) {
                throw CommandUtils.createException("carpet.commands.playerManager.schedule.read_file");
            }
            instance.addTask(new DelayedLoginTask(server, serial, tick));
            String key = server.getPlayerManager().getPlayer(name) == null
                    // <玩家>将于<时间>后上线
                    ? "carpet.commands.playerManager.schedule.login"
                    // <玩家>将于<时间>后再次尝试上线
                    : "carpet.commands.playerManager.schedule.login.try";
            // 发送命令反馈
            MessageUtils.sendMessage(context, key, serial.getDisplayName(), time);
        } else {
            // 修改上线时间
            DelayedLoginTask task = list.getFirst();
            // 为名称添加悬停文本
            MutableText info = TextUtils.hoverText(name, task.getInfo());
            task.setDelayed(tick);
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.login.modify", info, time);
        }
        return (int) tick;
    }

    // 延迟下线
    private static int addDelayedLogoutTask(CommandContext<ServerCommandSource> context, TimeUnit unit) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 获取假玩家延时下线游戏刻数
        long tick = unit.getDelayed(context);
        MutableText time = TextUtils.hoverText(TextConstants.tickToTime(tick), TextConstants.tickToRealTime(tick));
        ServerTaskManagerInterface instance = ServerTaskManagerInterface.getInstance(server);
        List<DelayedLogoutTask> list = instance.findTask(DelayedLogoutTask.class, logoutTask -> fakePlayer.equals(logoutTask.getFakePlayer()));
        // 添加新任务
        if (list.isEmpty()) {
            // 添加延时下线任务
            instance.addTask(new DelayedLogoutTask(server, fakePlayer, tick));
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.logout", fakePlayer.getDisplayName(), time);
        } else {
            // 修改退出时间
            DelayedLogoutTask task = list.getFirst();
            task.setDelayed(tick);
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.logout.modify", fakePlayer.getDisplayName(), time);
        }
        return (int) tick;
    }

    // 取消任务
    private static int cancelScheduleTask(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ServerTaskManagerInterface instance = ServerTaskManagerInterface.getInstance(server);
        String name = StringArgumentType.getString(context, "name");
        // 获取符合条件的任务列表
        List<PlayerScheduleTask> list = instance.findTask(PlayerScheduleTask.class, task -> Objects.equals(task.getPlayerName(), name));
        if (list.isEmpty()) {
            throw CommandUtils.createException("carpet.commands.playerManager.schedule.cancel.fail");
        }
        ArrayList<ServerTask> tasks = instance.getTaskList();
        list.forEach(task -> {
            // 删除任务，发送命令反馈
            tasks.remove(task);
            task.onCancel(context);
        });
        return list.size();
    }

    // 列出所有任务
    private static int listScheduleTask(CommandContext<ServerCommandSource> context) {
        MinecraftServer server = context.getSource().getServer();
        ServerTaskManagerInterface instance = ServerTaskManagerInterface.getInstance(server);
        List<PlayerScheduleTask> list = instance.findTask(PlayerScheduleTask.class, take -> true);
        if (list.isEmpty()) {
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.list.empty");
        } else {
            list.forEach(task -> task.sendEachMessage(context.getSource()));
        }
        return list.size();
    }

    /**
     * 时间单位
     */
    private enum TimeUnit {
        /**
         * tick
         */
        TICK,
        /**
         * 秒
         */
        SECOND,
        /**
         * 分钟
         */
        MINUTE,
        /**
         * 小时
         */
        HOUR;

        // 获取单位名称
        private String getName() {
            return switch (this) {
                case TICK -> "t";
                case SECOND -> "s";
                case MINUTE -> "min";
                case HOUR -> "h";
            };
        }

        // 将游戏刻转化为对应单位
        private long getDelayed(CommandContext<ServerCommandSource> context) {
            int delayed = IntegerArgumentType.getInteger(context, "delayed");
            return switch (this) {
                case TICK -> delayed;
                case SECOND -> delayed * 20L;
                case MINUTE -> delayed * 1200L;
                case HOUR -> delayed * 72000L;
            };
        }
    }
}
