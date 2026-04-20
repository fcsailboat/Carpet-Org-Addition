package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.config.GlobalConfigs;
import boat.carpetorgaddition.exception.CommandExecuteIOException;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.fakeplayer.*;
import boat.carpetorgaddition.periodic.task.DelayedTask;
import boat.carpetorgaddition.periodic.task.IterativeTask;
import boat.carpetorgaddition.periodic.task.ServerTaskManager;
import boat.carpetorgaddition.periodic.task.batch.BatchSpawnFakePlayerTask;
import boat.carpetorgaddition.periodic.task.schedule.DelayedLoginTask;
import boat.carpetorgaddition.periodic.task.schedule.DelayedLogoutTask;
import boat.carpetorgaddition.periodic.task.schedule.PlayerScheduleTask;
import boat.carpetorgaddition.periodic.task.schedule.ReLoginTask;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.FakePlayerSpawner;
import boat.carpetorgaddition.wheel.WorldFormat;
import boat.carpetorgaddition.wheel.page.PageManager;
import boat.carpetorgaddition.wheel.page.PagedCollection;
import boat.carpetorgaddition.wheel.permission.PermissionLevel;
import boat.carpetorgaddition.wheel.permission.PermissionManager;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PlayerManagerCommand extends AbstractServerCommand {
    private static final String SAFEAFK_PROPERTIES = "safeafk.properties";
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("playerManager");
    public static final LocalizationKey GROUP = KEY.then("group");
    public static final LocalizationKey GROUP_LIST = GROUP.then("list");
    public static final LocalizationKey SAFE_AFK = KEY.then("safeafk");
    public static final LocalizationKey SCHEDULE = KEY.then("schedule");
    public static final LocalizationKey BATCH = KEY.then("batch");
    private static final LocalizationKey SUMMON = KEY.then("summon");
    private static final LocalizationKey STARTUP = KEY.then("info").then("startup");

    public PlayerManagerCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        // 延迟登录节点
        RequiredArgumentBuilder<CommandSourceStack, Integer> loginNode = Commands.argument("delayed", IntegerArgumentType.integer(1));
        for (TimeUnit unit : TimeUnit.values()) {
            // 添加时间单位
            loginNode.then(Commands.literal(unit.getName())
                    .executes(context -> addDelayedLoginTask(context, unit)));
        }
        // 延迟登出节点
        RequiredArgumentBuilder<CommandSourceStack, Integer> logoutNode = Commands.argument("delayed", IntegerArgumentType.integer(1));
        for (TimeUnit unit : TimeUnit.values()) {
            logoutNode.then(Commands.literal(unit.getName())
                    .executes(context -> addDelayedLogoutTask(context, unit)));
        }
        // 玩家启动任务节点
        LiteralArgumentBuilder<CommandSourceStack> startupNode = Commands.literal("startup");
        String[] times = {"1t", "3t", "5t"};
        for (FakePlayerStartupAction action : FakePlayerStartupAction.SimpleAction.values()) {
            startupNode.then(Commands.literal(action.toString())
                    .executes(context -> this.addStartupFunction(context, action, 1))
                    .then(Commands.argument("delay", TimeArgument.time(1))
                            .suggests((_, builder) -> SharedSuggestionProvider.suggest(times, builder))
                            .executes(context -> this.addStartupFunction(context, action, IntegerArgumentType.getInteger(context, "delay"))))
                    .then(Commands.literal("clear")
                            .executes(context -> this.addStartupFunction(context, action, -1))));
        }
        startupNode.then(Commands.literal("run")
                .then(Commands.argument("command", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            String playerName = StringArgumentType.getString(context, "name");
                            FakePlayerSerializer serializer = getFakePlayerSerializer(context, playerName);
                            List<String> list = serializer.listCommandStartupFunction().stream()
                                    .map(StringArgumentType::escapeIfRequired)
                                    .toList();
                            return SharedSuggestionProvider.suggest(list, builder);
                        })
                        .executes(context -> this.addStartupRunCommandFunction(context, 1))
                        .then(Commands.argument("delay", TimeArgument.time(1))
                                .suggests((_, builder) -> SharedSuggestionProvider.suggest(times, builder))
                                .executes(context -> this.addStartupRunCommandFunction(context, IntegerArgumentType.getInteger(context, "delay"))))
                        .then(Commands.literal("clear")
                                .executes(context -> this.addStartupRunCommandFunction(context, -1)))));
        this.dispatcher.register(Commands.literal(name)
                .requires(source -> CarpetOrgAdditionSettings.COMMAND_PLAYER_MANAGER.value().hasPermission(source))
                .then(Commands.literal("save")
                        .then(Commands.argument(CommandUtils.PLAYER, EntityArgument.player())
                                .executes(context -> savePlayerData(context, false))
                                .then(Commands.argument("comment", StringArgumentType.string())
                                        .executes(context -> savePlayerData(context, true)))))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(playerSuggests())
                                .executes(this::spawnPlayer)))
                .then(Commands.literal("kill")
                        .then(Commands.argument(CommandUtils.PLAYER, EntityArgument.player())
                                .executes(this::killPlayer)))
                .then(Commands.literal("modify")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(playerSuggests())
                                .then(Commands.literal("comment")
                                        .then(Commands.argument("comment", StringArgumentType.string())
                                                .executes(this::setComment)))
                                .then(Commands.literal("resave")
                                        .executes(this::modifyPlayer))
                                .then(startupNode)
                                .then(Commands.literal("autologin")
                                        .requires(PermissionManager.register("playerManager.autologin", PermissionLevel.PASS))
                                        .then(Commands.argument("autologin", BoolArgumentType.bool())
                                                .executes(context -> this.setAutoLogin(context, false))))))
                .then(Commands.literal("group")
                        .then(Commands.literal("add")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests(playerSuggests())
                                        .then(Commands.argument("group", StringArgumentType.string())
                                                .suggests(groupSuggests(true))
                                                .executes(this::addToGroup))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests(playerSuggests())
                                        .then(Commands.argument("group", StringArgumentType.string())
                                                .suggests(groupSuggests(false))
                                                .executes(this::removeFromGroup))))
                        .then(Commands.literal("list")
                                .then(Commands.literal("group")
                                        .then(Commands.argument("group", StringArgumentType.string())
                                                .suggests(allGroupSuggests())
                                                .executes(this::listGroup)))
                                .then(Commands.literal("ungrouped")
                                        .executes(this::listUngrouped))
                                .then(Commands.literal("all")
                                        .executes(this::listAll)))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("group", StringArgumentType.string())
                                        .suggests(allGroupSuggests())
                                        .executes(this::spawnGroupPlayer)))
                        .then(Commands.literal("kill")
                                .then(Commands.argument("group", StringArgumentType.string())
                                        .suggests(allGroupSuggests())
                                        .executes(this::killGroupPlayer))))
                .then(Commands.literal("reload")
                        .executes(this::reload))
                .then(Commands.literal("autologin")
                        .requires(PermissionManager.register("playerManager.autologin", PermissionLevel.PASS))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(playerSuggests())
                                .then(Commands.argument("autologin", BoolArgumentType.bool())
                                        .executes(context -> this.setAutoLogin(context, true)))))
                .then(Commands.literal("list")
                        .executes(this::list)
                        .then(Commands.argument("filter", StringArgumentType.string())
                                .executes(this::listWithFilter)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(playerSuggests())
                                .executes(this::remove)))
                .then(Commands.literal("schedule")
                        .then(Commands.literal("relogin")
                                .requires(PermissionManager.register("playerManager.schedule.relogin", PermissionLevel.PASS))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests(reLoginTaskSuggests())
                                        .then(Commands.argument("interval", IntegerArgumentType.integer(1))
                                                .suggests((_, builder) -> SharedSuggestionProvider.suggest(new String[]{"1", "3", "5"}, builder))
                                                .executes(this::setReLogin))
                                        .then(Commands.literal("stop")
                                                .executes(this::stopReLogin))))
                        .then(Commands.literal("login")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests(playerSuggests())
                                        .then(loginNode)))
                        .then(Commands.literal("logout")
                                .then(Commands.argument(CommandUtils.PLAYER, EntityArgument.player())
                                        .then(logoutNode)))
                        .then(Commands.literal("cancel")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests(cancelSuggests())
                                        .executes(this::cancelScheduleTask)))
                        .then(Commands.literal("list")
                                .executes(this::listScheduleTask)))
                .then(Commands.literal("safeafk")
                        .then(Commands.literal("set")
                                .then(Commands.argument(CommandUtils.PLAYER, EntityArgument.player())
                                        .executes(context -> safeAfk(context, 5F, false))
                                        .then(Commands.argument("threshold", FloatArgumentType.floatArg())
                                                .executes(context -> safeAfk(context, FloatArgumentType.getFloat(context, "threshold"), false))
                                                .then(Commands.argument("save", BoolArgumentType.bool())
                                                        .executes(context -> safeAfk(context, FloatArgumentType.getFloat(context, "threshold"), BoolArgumentType.getBool(context, "save")))))))
                        .then(Commands.literal("list")
                                .executes(this::listSafeAfk))
                        .then(Commands.literal("cancel")
                                .then(Commands.argument(CommandUtils.PLAYER, EntityArgument.player())
                                        .executes(context -> cancelSafeAfk(context, false))
                                        .then(Commands.argument("save", BoolArgumentType.bool())
                                                .executes(context -> cancelSafeAfk(context, true)))))
                        .then(Commands.literal("query")
                                .then(Commands.argument(CommandUtils.PLAYER, EntityArgument.player())
                                        .executes(this::querySafeAfk))))
                .then(Commands.literal("batch")
                        .then(Commands.argument("prefix", StringArgumentType.word())
                                .then(Commands.argument("start", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("end", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                                .then(Commands.literal("spawn")
                                                        .executes(context -> batchSpawn(context, false))
                                                        .then(Commands.argument("at", Vec3Argument.vec3())
                                                                .executes(context -> batchSpawn(context, true))))
                                                .then(Commands.literal("kill")
                                                        .executes(this::batchKill))
                                                .then(Commands.literal("drop")
                                                        .executes(this::batchDrop))
                                                .then(Commands.literal("trial")
                                                        .executes(context -> batchTrial(context, false))
                                                        .then(Commands.argument("at", Vec3Argument.vec3())
                                                                .executes(context -> batchTrial(context, true))))))))
                .then(Commands.literal("respawn")
                        .executes(context -> respawnResident(context, null))
                        .then(Commands.argument("time", StringArgumentType.string())
                                .suggests(respawnPlayerSuggests())
                                .executes(context -> respawnResident(context, StringArgumentType.getString(context, "time"))))));
    }

    // cancel子命令自动补全
    @NonNull
    private SuggestionProvider<CommandSourceStack> cancelSuggests() {
        return (context, builder) -> {
            MinecraftServer server = context.getSource().getServer();
            ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
            Stream<String> stream = manager.stream(PlayerScheduleTask.class).map(PlayerScheduleTask::getPlayerName);
            return SharedSuggestionProvider.suggest(stream, builder);
        };
    }

    // 自动补全玩家名
    private SuggestionProvider<CommandSourceStack> playerSuggests() {
        return (context, builder) -> {
            MinecraftServer server = context.getSource().getServer();
            ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
            Stream<String> stream = coordinator.getPlayerSerializationManager()
                    .listAll()
                    .stream()
                    .map(FakePlayerSerializer::getName)
                    .map(StringArgumentType::escapeIfRequired);
            return SharedSuggestionProvider.suggest(stream, builder);
        };
    }

    private SuggestionProvider<CommandSourceStack> allGroupSuggests() {
        return (context, builder) -> {
            MinecraftServer server = context.getSource().getServer();
            ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
            PlayerSerializationManager manager = coordinator.getPlayerSerializationManager();
            Stream<String> stream = manager.listGrouped()
                    .keySet()
                    .stream()
                    .map(StringArgumentType::escapeIfRequired);
            return SharedSuggestionProvider.suggest(stream, builder);
        };
    }

    /**
     * @param add 如果为{@code true}，表示当前正在输入{@code add}子命令，否则当前正在输入{@code remove}子命令
     */
    private SuggestionProvider<CommandSourceStack> groupSuggests(boolean add) {
        return (context, builder) -> {
            MinecraftServer server = context.getSource().getServer();
            ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
            PlayerSerializationManager manager = coordinator.getPlayerSerializationManager();
            String name = StringArgumentType.getString(context, "name");
            Stream<String> stream = manager.listGrouped().keySet().stream();
            Optional<FakePlayerSerializer> optional = manager.get(name);
            if (optional.isPresent()) {
                // 输入... group add ...命令时，如果玩家本来就在某个组中，则命令建议中不显示该组，remove命令同理
                Predicate<String> predicate = group -> optional.get().getGroups().contains(group);
                stream = stream.filter(add ? predicate.negate() : predicate);
            }
            return SharedSuggestionProvider.suggest(stream.map(StringArgumentType::escapeIfRequired), builder);
        };
    }

    // relogin子命令自动补全
    @NonNull
    private SuggestionProvider<CommandSourceStack> reLoginTaskSuggests() {
        return (context, builder) -> {
            MinecraftServer server = context.getSource().getServer();
            ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
            // 所有正在周期性上下线的玩家
            List<String> taskList = manager.stream(ReLoginTask.class).map(ReLoginTask::getPlayerName).toList();
            // 所有在线玩家
            List<String> onlineList = server.getPlayerList()
                    .getPlayers()
                    .stream()
                    .map(PlayerUtils::getName)
                    .toList();
            HashSet<String> players = new HashSet<>();
            players.addAll(taskList);
            players.addAll(onlineList);
            return SharedSuggestionProvider.suggest(players.stream(), builder);
        };
    }

    private SuggestionProvider<CommandSourceStack> respawnPlayerSuggests() {
        return (context, builder) -> {
            CommandSourceStack source = context.getSource();
            MinecraftServer server = ServerUtils.getServer(source);
            ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
            FakePlayerResidents players = coordinator.getSavedFakePlayer();
            Stream<String> stream = players.listFileTime().stream().map(StringArgumentType::escapeIfRequired);
            return SharedSuggestionProvider.suggest(stream, builder);
        };
    }

    private int listGroup(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String group = StringArgumentType.getString(context, "group");
        MinecraftServer server = context.getSource().getServer();
        ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
        PlayerSerializationManager manager = coordinator.getPlayerSerializationManager();
        List<FakePlayerSerializer> serializers = manager.listGroup(group);
        // 不存在的组
        if (serializers.isEmpty()) {
            throw CommandUtils.createException(GROUP.then("non_existent").translate(group));
        }
        Component login = TextBuilder.of("[↑]")
                .setCommand(CommandProvider.playerManagerSpawnGroup(group))
                .setHover(LocalizationKeys.Button.LOGIN.translate())
                .setColor(ChatFormatting.GREEN)
                .build();
        Component logout = TextBuilder.of("[↓]")
                .setCommand(CommandProvider.playerManagerKillGroup(group))
                .setHover(LocalizationKeys.Button.LOGOUT.translate())
                .setColor(ChatFormatting.RED)
                .build();
        PagedCollection collection = coordinator.getPageManager().newPagedCollection(context.getSource());
        List<Supplier<Component>> messages = serializers.stream().map(FakePlayerSerializer::line).toList();
        collection.addContent(messages);
        MessageUtils.sendEmptyMessage(context);
        MessageUtils.sendMessage(context, GROUP_LIST.translate(group, login, logout));
        collection.print();
        return collection.length();
    }

    private int listUngrouped(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
        PlayerSerializationManager manager = coordinator.getPlayerSerializationManager();
        List<FakePlayerSerializer> ungrouped = manager.listUngrouped();
        LocalizationKey key = GROUP.then("name");
        if (ungrouped.isEmpty()) {
            Component component = key.then("ungrouped").translate();
            throw CommandUtils.createException(GROUP.then("non_existent").translate(component));
        }
        List<Supplier<Component>> messages = ungrouped.stream().map(FakePlayerSerializer::line).toList();
        PagedCollection collection = coordinator.getPageManager().newPagedCollection(context.getSource());
        collection.addContent(messages);
        MessageUtils.sendEmptyMessage(context);
        MessageUtils.sendMessage(context, GROUP_LIST.then("ungrouped").translate());
        collection.print();
        return collection.length();
    }

    private int listAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
        PlayerSerializationManager manager = coordinator.getPlayerSerializationManager();
        List<Supplier<Component>> list = manager.listAll().stream().map(FakePlayerSerializer::line).toList();
        PagedCollection collection = coordinator.getPageManager().newPagedCollection(context.getSource());
        collection.addContent(list);
        MessageUtils.sendEmptyMessage(context);
        MessageUtils.sendMessage(context, GROUP_LIST.then("all").translate());
        collection.print();
        return collection.length();
    }

    private int addToGroup(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        String group = StringArgumentType.getString(context, "group");
        LocalizationKey key = GROUP.then("add");
        if (group.isEmpty()) {
            throw CommandUtils.createException(key.then("empty").translate());
        }
        FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
        if (serializer.addToGroup(group)) {
            MessageUtils.sendMessage(context, key.translate(serializer.getDisplayName(), group));
        } else {
            throw CommandUtils.createException(key.then("fail").translate());
        }
        return 1;
    }

    private int removeFromGroup(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        String group = StringArgumentType.getString(context, "group");
        FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
        LocalizationKey key = GROUP.then("remove");
        if (serializer.removeFromGroup(group)) {
            MessageUtils.sendMessage(context, key.translate(serializer.getDisplayName(), group));
        } else {
            throw CommandUtils.createException(key.then("fail").translate());
        }
        return 1;
    }

    private int spawnGroupPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String group = StringArgumentType.getString(context, "group");
        MinecraftServer server = context.getSource().getServer();
        PlayerSerializationManager manager = ServerComponentCoordinator.getCoordinator(server).getPlayerSerializationManager();
        List<FakePlayerSerializer> list = manager.listGroup(group);
        // 不存在的组
        if (list.isEmpty()) {
            throw CommandUtils.createException(GROUP.then("non_existent").translate(group));
        }
        int count = (int) list.stream()
                // 如果玩家不存在则生成
                .filter(serializer -> serializer.spawn(server, false))
                .count();
        sendPlayerJoinMessage(server, count);
        return count;
    }

    private int killGroupPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String group = StringArgumentType.getString(context, "group");
        MinecraftServer server = context.getSource().getServer();
        PlayerSerializationManager manager = ServerComponentCoordinator.getCoordinator(server).getPlayerSerializationManager();
        List<FakePlayerSerializer> list = manager.listGroup(group);
        // 不存在的组
        if (list.isEmpty()) {
            throw CommandUtils.createException(GROUP.then("non_existent").translate(group));
        }
        List<EntityPlayerMPFake> players = list.stream()
                .map(FakePlayerSerializer::getName)
                .map(name -> ServerUtils.getPlayer(server, name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(player -> player instanceof EntityPlayerMPFake)
                .map(player -> (EntityPlayerMPFake) player).toList();
        players.forEach(PlayerUtils::silenceLogout);
        sendPlayerLeaveMessage(server, players.size());
        return players.size();
    }

    /**
     * 重新加载玩家数据
     */
    private int reload(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
        PlayerSerializationManager manager = coordinator.getPlayerSerializationManager();
        manager.init();
        MessageUtils.sendMessage(context, KEY.then("reload").translate());
        return 1;
    }

    // 安全挂机
    private int safeAfk(CommandContext<CommandSourceStack> context, float threshold, boolean save) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 假玩家安全挂机阈值必须小于玩家最大生命值
        if (threshold >= fakePlayer.getMaxHealth()) {
            throw CommandUtils.createException(SAFE_AFK.then("threshold_too_high").translate());
        }
        // 低于或等于0的值没有实际意义，统一设置为-1
        if (threshold <= 0F) {
            threshold = -1F;
        }
        // 设置安全挂机阈值
        FakePlayerSafeAfkInterface safeAfk = (FakePlayerSafeAfkInterface) fakePlayer;
        safeAfk.carpet_Org_Addition$setHealthThreshold(threshold);
        if (save) {
            try {
                saveSafeAfkThreshold(context, threshold, fakePlayer);
            } catch (IOException e) {
                throw CommandExecuteIOException.of(e);
            }
        } else {
            String command = CommandProvider.setupSafeAfkPermanentlyChange(fakePlayer, threshold);
            MessageUtils.sendMessage(
                    context,
                    SAFE_AFK.then("set").translate(fakePlayer.getDisplayName(), threshold, TextProvider.clickRun(command))
            );
        }
        return (int) threshold;
    }

    // 列出所有设置了安全挂机的在线假玩家
    private int listSafeAfk(CommandContext<CommandSourceStack> context) {
        List<ServerPlayer> list = context.getSource().getServer()
                .getPlayerList()
                .getPlayers()
                .stream()
                .filter(player -> player instanceof EntityPlayerMPFake)
                .toList();
        LocalizationKey key = SAFE_AFK.then("list");
        int count = 0;
        // 遍历所有在线并且设置了安全挂机的假玩家
        for (ServerPlayer player : list) {
            float threshold = ((FakePlayerSafeAfkInterface) player).carpet_Org_Addition$getHealthThreshold();
            if (threshold < 0) {
                continue;
            }
            MessageUtils.sendMessage(context, key.then("each").translate(player.getDisplayName(), threshold));
            count++;
        }
        // 没有玩家被列出
        if (count == 0) {
            MessageUtils.sendMessage(context, key.then("empty").translate());
        }
        return count;
    }

    // 取消假玩家的安全挂机
    private int cancelSafeAfk(CommandContext<CommandSourceStack> context, boolean remove) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 设置安全挂机阈值
        FakePlayerSafeAfkInterface safeAfk = (FakePlayerSafeAfkInterface) fakePlayer;
        safeAfk.carpet_Org_Addition$setHealthThreshold(-1);
        if (remove) {
            try {
                saveSafeAfkThreshold(context, -1, fakePlayer);
            } catch (IOException e) {
                throw CommandExecuteIOException.of(e);
            }
        } else {
            Component command = TextProvider.clickRun(CommandProvider.cancelSafeAfkPermanentlyChange(fakePlayer));
            MessageUtils.sendMessage(context, SAFE_AFK.then("remove").translate(fakePlayer.getDisplayName(), command));
        }
        return 1;
    }

    // 查询指定玩家的安全挂机阈值
    private int querySafeAfk(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        float threshold = ((FakePlayerSafeAfkInterface) fakePlayer).carpet_Org_Addition$getHealthThreshold();
        LocalizationKey key = SAFE_AFK.then("list").then("each");
        MessageUtils.sendMessage(context, key.translate(fakePlayer.getDisplayName(), threshold));
        return (int) threshold;
    }

    // 保存或删除安全挂机阈值
    private void saveSafeAfkThreshold(CommandContext<CommandSourceStack> context, float threshold, EntityPlayerMPFake fakePlayer) throws IOException {
        String playerName = PlayerUtils.getName(fakePlayer);
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
                MessageUtils.sendMessage(context, SAFE_AFK.then("set").then("persistence").translate(fakePlayer.getDisplayName(), threshold));
            } else {
                // 将玩家安全挂机阈值从配置文件中删除
                properties.remove(playerName);
                MessageUtils.sendMessage(context, SAFE_AFK.then("remove").then("persistence").translate(fakePlayer.getDisplayName()));
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
    public static void loadSafeAfk(ServerPlayer player) {
        if (player instanceof EntityPlayerMPFake) {
            WorldFormat worldFormat = new WorldFormat(ServerUtils.getServer(player), null);
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
                    CarpetOrgAddition.LOGGER.error("Error loading safe AFK threshold for fake players: ", e);
                    return;
                }
                try {
                    // 设置安全挂机阈值
                    FakePlayerSafeAfkInterface safeAfk = (FakePlayerSafeAfkInterface) player;
                    String value = properties.getProperty(PlayerUtils.getName(player));
                    if (value == null) {
                        return;
                    }
                    float threshold = Float.parseFloat(value);
                    safeAfk.carpet_Org_Addition$setHealthThreshold(threshold);
                    // 广播阈值设置的消息
                    TextBuilder builder = TextBuilder.of(SAFE_AFK.then("set").then("on_login").translate(player.getDisplayName(), threshold));
                    builder.setGrayItalic();
                    MessageUtils.sendMessage(ServerUtils.getServer(player), builder.build());
                } catch (NumberFormatException e) {
                    CarpetOrgAddition.LOGGER.error("Failed to set the AFK safety threshold for {}", PlayerUtils.getName(player), e);
                }
            }
        }
    }

    // 列出每一个玩家
    private int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        PlayerSerializationManager manager = getSerializationManager(server);
        Map<@Nullable String, List<FakePlayerSerializer>> map = manager.listAllGroups();
        LocalizationKey listKey = KEY.then("list");
        if (map.isEmpty()) {
            // 没有玩家被列出
            MessageUtils.sendMessage(context, listKey.then("no_player").translate());
            return 0;
        }
        if (map.size() == 1) {
            // 只有一个组，直接展示玩家
            Map.Entry<String, List<FakePlayerSerializer>> entry = map.entrySet().iterator().next();
            List<Supplier<Component>> list = entry.getValue().stream().map(FakePlayerSerializer::line).toList();
            PageManager pageManager = ServerComponentCoordinator.getCoordinator(server).getPageManager();
            PagedCollection collection = pageManager.newPagedCollection(context.getSource());
            collection.addContent(list);
            MessageUtils.sendEmptyMessage(context);
            MessageUtils.sendMessage(context, GROUP_LIST.then("all").translate());
            collection.print();
            return list.size();
        } else {
            ArrayList<Component> list = new ArrayList<>();
            MessageUtils.sendMessage(context, listKey.then("expand").translate());
            LocalizationKey groupNameKey = GROUP.then("name");
            // 未分组，在倒数第二个展示
            TextBuilder ungrouped = null;
            for (Map.Entry<@Nullable String, List<FakePlayerSerializer>> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    ungrouped = TextBuilder.of(groupNameKey.then("ungrouped").translate());
                    setStyle(ungrouped, entry.getValue().size(), CommandProvider.listUngroupedPlayer());
                    continue;
                }
                TextBuilder builder = TextBuilder.of("[" + entry.getKey() + "]");
                setStyle(builder, entry.getValue().size(), CommandProvider.listGroupPlayer(entry.getKey()));
                list.add(builder.build());
            }
            if (ungrouped != null) {
                list.add(ungrouped.build());
            }
            // 包含所有玩家的组，在最后一个展示
            TextBuilder builder = TextBuilder.of(groupNameKey.then("all").translate());
            setStyle(builder, manager.size(), CommandProvider.listAllPlayer());
            list.add(builder.build());
            Component message = TextBuilder.joinList(list, TextBuilder.create(" "));
            MessageUtils.sendMessage(context.getSource(), message);
            return list.size();
        }
    }

    private int listWithFilter(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        String filter = StringArgumentType.getString(context, "filter");
        PlayerSerializationManager manager = getSerializationManager(server);
        List<Supplier<Component>> list = manager.listAll().stream()
                .filter(serializer -> serializer.match(filter))
                .map(FakePlayerSerializer::line)
                .toList();
        LocalizationKey key = KEY.then("list");
        if (list.isEmpty()) {
            // 没有玩家被列出
            MessageUtils.sendMessage(context, key.then("no_player").translate());
            return 0;
        }
        PageManager pageManager = ServerComponentCoordinator.getCoordinator(server).getPageManager();
        PagedCollection collection = pageManager.newPagedCollection(context.getSource());
        collection.addContent(list);
        MessageUtils.sendEmptyMessage(context);
        MessageUtils.sendMessage(context, key.then("filter").translate(filter));
        collection.print();
        return list.size();
    }

    private void setStyle(TextBuilder builder, int size, String command) {
        builder.setColor(ChatFormatting.AQUA);
        builder.setHover(GROUP.then("player").translate(size));
        builder.setCommand(command);
    }

    // 设置注释
    private int setComment(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        // 修改注释
        String comment = StringArgumentType.getString(context, "comment");
        FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
        serializer.setComment(comment);
        LocalizationKey key = KEY.then("comment");
        // 发送命令反馈
        if (comment.isEmpty()) {
            // 移除注释
            MessageUtils.sendMessage(context, key.then("remove").translate(serializer.getDisplayName()));
        } else {
            // 修改注释
            MessageUtils.sendMessage(context, key.then("set").translate(serializer.getDisplayName(), comment));
        }
        return 1;
    }

    // 设置自动登录
    private int setAutoLogin(CommandContext<CommandSourceStack> context, boolean displayWarning) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        boolean autologin = BoolArgumentType.getBool(context, "autologin");
        LocalizationKey key = KEY.then("autologin");
        if (displayWarning) {
            String command = CommandProvider.playerManagerAutologin(name, autologin);
            Component component = key.then("warn").builder(command).setGrayItalic().build();
            MessageUtils.sendMessage(context, component);
        }
        FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
        // 设置自动登录
        serializer.setAutologin(autologin);
        // 发送命令反馈
        if (autologin) {
            MessageUtils.sendMessage(context, key.then("set").translate(serializer.getDisplayName()));
        } else {
            MessageUtils.sendMessage(context, key.then("remove").translate(serializer.getDisplayName()));
        }
        return 1;
    }

    // 保存玩家
    private int savePlayerData(CommandContext<CommandSourceStack> context, boolean hasComment) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        MinecraftServer server = context.getSource().getServer();
        PlayerSerializationManager manager = getSerializationManager(server);
        // 玩家数据是否已存在
        String name = PlayerUtils.getName(fakePlayer);
        if (IOUtils.isValidFileName(name)) {
            throw CommandUtils.createException(LocalizationKeys.File.INVALID_NAME.translate());
        }
        Optional<FakePlayerSerializer> optional = manager.get(name);
        LocalizationKey key = KEY.then("save");
        if (optional.isPresent()) {
            String command = CommandProvider.playerManagerResave(name);
            // 单击执行命令
            Component clickResave = TextProvider.clickRun(command);
            MessageUtils.sendMessage(context, key.then("file_already_exist").translate(clickResave));
            return 0;
        } else {
            String comment = hasComment ? StringArgumentType.getString(context, "comment") : "";
            if (CarpetOrgAdditionSettings.PLAYER_MANAGER_FORCE_COMMENT.value() && comment.isBlank()) {
                throw CommandUtils.createException(LocalizationKeys.Rule.Message.PLAYER_MANAGER_FORCE_COMMENT.translate());
            }
            FakePlayerSerializer serializer = new FakePlayerSerializer(fakePlayer);
            if (!comment.isBlank()) {
                serializer.setComment(comment);
            }
            manager.add(serializer);
            // 首次保存
            MessageUtils.sendMessage(context.getSource(), key.translate(fakePlayer.getDisplayName()));
            return 1;
        }
    }

    // 修改玩家数据
    private int modifyPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        EntityPlayerMPFake fakePlayer = CommandUtils.getFakePlayer(server, name);
        FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
        serializer.update(fakePlayer);
        MessageUtils.sendMessage(source, KEY.then("resave").translate(fakePlayer.getDisplayName()));
        return 1;
    }

    // 生成假玩家
    private int spawnPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        try {
            // 生成假玩家
            FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
            MinecraftServer server = context.getSource().getServer();
            if (ServerUtils.getPlayer(server, name).isPresent()) {
                throw CommandUtils.createException(PlayerManagerCommand.KEY.then("spawn").then("player_exist").translate());
            }
            serializer.getSpawner(server)
                    .setSummoner(context.getSource().getPlayer())
                    .spawn();
        } catch (RuntimeException e) {
            // 尝试生成假玩家时出现意外问题
            throw CommandUtils.createException(KEY.then("spawn").then("fail").translate(), e);
        }
        return 1;
    }

    private int killPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        PlayerUtils.kill(fakePlayer);
        return 1;
    }

    private FakePlayerSerializer getFakePlayerSerializer(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        PlayerSerializationManager manager = getSerializationManager(context);
        Optional<FakePlayerSerializer> optional = manager.get(name);
        if (optional.isEmpty()) {
            throw CommandUtils.createException(KEY.then("cannot_find_file").translate(name));
        }
        return optional.get();
    }

    // 删除玩家信息
    private int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        PlayerSerializationManager manager = getSerializationManager(context);
        Optional<FakePlayerSerializer> optional = manager.get(name);
        LocalizationKey key = KEY.then("delete");
        if (optional.isEmpty()) {
            throw CommandUtils.createException(key.then("non_existent").translate());
        }
        if (manager.remove(optional.get())) {
            // 文件存在且文件删除成功
            MessageUtils.sendMessage(context.getSource(), key.translate());
            return 1;
        }
        throw CommandUtils.createException(key.then("fail").translate());
    }

    private int respawnResident(CommandContext<CommandSourceStack> context, @Nullable String time) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = ServerUtils.getServer(source);
        ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
        FakePlayerResidents players = coordinator.getSavedFakePlayer();
        Set<FakePlayerSerializer> set = players.get(time);
        if (set.isEmpty()) {
            return 0;
        }
        ServerTaskManager taskManager = coordinator.getServerTaskManager();
        List<Runnable> list = set.stream()
                .filter(serializer -> ServerUtils.getPlayer(server, serializer.getName()).isEmpty())
                .map(serializer -> (Runnable) () -> serializer.spawn(server, true))
                .toList();
        taskManager.addTask(new IterativeTask(source, list, 30, 10000));
        return set.size();
    }

    private int addStartupFunction(CommandContext<CommandSourceStack> context, FakePlayerStartupAction action, int delay) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
        serializer.addStartupFunction(action, delay);
        return delay;
    }

    private int addStartupRunCommandFunction(CommandContext<CommandSourceStack> context, int delay) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        if (server.isDedicatedServer()) {
            this.validateMultiPlayerUsability(context, source);
        }
        String command = StringArgumentType.getString(context, "command");
        FakePlayerStartupAction action = FakePlayerStartupAction.CommandAction.of(command);
        return this.addStartupFunction(context, action, delay);
    }

    private void validateMultiPlayerUsability(CommandContext<CommandSourceStack> context, CommandSourceStack source) throws CommandSyntaxException {
        LocalizationKey key = STARTUP.then("run");
        if (GlobalConfigs.isAllowMpPlayerStartupCmd()) {
            if (Commands.LEVEL_OWNERS.check(source.permissions())) {
                return;
            }
            throw key.then("permission").toSyntaxException();
        }
        String name = StringArgumentType.getString(context, "name");
        FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
        File file = serializer.getFile();
        TextBuilder builder = key.then("multiplayer").builder();
        if (file != null) {
            builder.setHover(file.toPath().toAbsolutePath().normalize().toString());
        }
        throw CommandUtils.createException(builder.build());
    }

    // 设置不断重新上线下线
    private int setReLogin(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // 强制启用内存泄漏修复
        if (fixMemoryLeak(context)) {
            // 获取目标假玩家名
            String name = StringArgumentType.getString(context, "name");
            int interval = IntegerArgumentType.getInteger(context, "interval");
            MinecraftServer server = context.getSource().getServer();
            ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
            // 如果任务存在，修改任务，否则添加任务
            Optional<ReLoginTask> optional = manager.stream(ReLoginTask.class)
                    .filter(task -> Objects.equals(task.getPlayerName(), name))
                    .findFirst();
            if (optional.isEmpty()) {
                // 添加任务
                ServerPlayer player = server.getPlayerList().getPlayerByName(name);
                if (player == null) {
                    // 玩家不存在
                    throw CommandUtils.createPlayerNotFoundException();
                } else {
                    // 目标玩家不是假玩家
                    CommandUtils.requireFakePlayer(player);
                }
                manager.addTask(new ReLoginTask((EntityPlayerMPFake) player, interval, server, context.getSource()));
            } else {
                // 修改周期时间
                optional.get().setInterval(interval);
                MessageUtils.sendMessage(context, SCHEDULE.then("relogin").then("modify").translate(name, interval));
            }
            return interval;
        }
        return 0;
    }

    /**
     * 批量生成玩家
     */
    private int batchSpawn(CommandContext<CommandSourceStack> context, boolean at) throws CommandSyntaxException {
        return batchSpawn(context, at, CarpetOrgAddition::pass);
    }

    /**
     * 批量生成玩家并踢出玩家
     */
    private int batchTrial(CommandContext<CommandSourceStack> context, boolean at) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        // 异常由服务器命令源进行处理
        CommandSourceStack serverSource = server.createCommandSourceStack();
        ServerTaskManager taskManager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        return batchSpawn(context, at, fakePlayer -> {
            DelayedTask task = new DelayedTask(source, 30, () -> PlayerUtils.silenceLogout(fakePlayer));
            CommandUtils.handlingException(() -> taskManager.addTask(task), serverSource);
        });
    }

    private int batchSpawn(CommandContext<CommandSourceStack> context, boolean at, Consumer<EntityPlayerMPFake> callback) throws CommandSyntaxException {
        int start = IntegerArgumentType.getInteger(context, "start");
        int end = IntegerArgumentType.getInteger(context, "end");
        // 交换最大最小值，玩家可能将end和start参数反向输入
        int temp = Math.min(start, end);
        end = Math.max(start, end);
        start = temp;
        int count = end - start + 1;
        if (count > 256) {
            // 限制单次生成的最大玩家数量
            throw CommandUtils.createException(BATCH.then("exceeds_limit").translate(count, 256));
        }
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerTaskManager taskManager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        Vec3 vec3d = at ? Vec3Argument.getVec3(context, "at") : source.getPosition();
        Optional<ServerPlayer> optional = CommandUtils.getSourcePlayerNullable(source);
        Function<String, FakePlayerSpawner> function;
        if (optional.isEmpty()) {
            function = name -> FakePlayerSpawner.of(server, name)
                    .setPosition(vec3d)
                    .setWorld(ServerUtils.getWorld(source))
                    .setGameMode(GameType.SURVIVAL)
                    .setFlying(false)
                    .setSilence(true)
                    .setCallback(callback);

        } else {
            ServerPlayer player = optional.get();
            function = name -> FakePlayerSpawner.of(server, name)
                    .setPosition(vec3d)
                    .setYaw(player.getYRot())
                    .setPitch(player.getXRot())
                    .setWorld(ServerUtils.getWorld(player))
                    .setGameMode(player.gameMode())
                    .setFlying(player.getAbilities().flying)
                    .setSilence(true)
                    .setCallback(callback);
        }
        // 为假玩家名添加前缀，这不仅仅是为了让名称更统一，也是为了在一定程度上阻止玩家使用其他真玩家的名称召唤假玩家
        String prefix = StringArgumentType.getString(context, "prefix");
        List<String> list = batchPlayerList(prefix, start, end);
        LocalizationKey key = BATCH.then("name_too_long");
        if (PlayerUtils.playerNameTooLong(list.getFirst())) {
            throw CommandUtils.createException(key.then("all").translate());
        }
        if (PlayerUtils.playerNameTooLong(list.getLast())) {
            MessageUtils.sendMessage(source, key.then("partial").builder().setGrayItalic().build());
        }
        taskManager.addTask(new BatchSpawnFakePlayerTask(server, source, function, list));
        return list.size();
    }

    private List<String> batchPlayerList(String prefix, int start, int end) {
        return IntStream.rangeClosed(start, end)
                .mapToObj(i -> (getPrefix(prefix)) + i)
                .map(PlayerUtils::appendNamePrefix)
                .map(PlayerUtils::appendNameSuffix)
                .toList();
    }

    private static String getPrefix(String prefix) {
        return prefix.endsWith("_") ? prefix : prefix + "_";
    }

    /**
     * 批量下线玩家
     */
    private int batchKill(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int start = IntegerArgumentType.getInteger(context, "start");
        int end = IntegerArgumentType.getInteger(context, "end");
        int temp = Math.min(start, end);
        end = Math.max(start, end);
        start = temp;
        String prefix = StringArgumentType.getString(context, "prefix");
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerTaskManager taskManager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        List<Runnable> list = this.batchPlayerList(prefix, start, end).stream()
                .map(name -> ServerUtils.getPlayer(server, name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(player -> player instanceof EntityPlayerMPFake)
                .map(player -> (EntityPlayerMPFake) player)
                .map(fakePlayer -> (Runnable) () -> PlayerUtils.silenceLogout(fakePlayer))
                .toList();
        if (list.isEmpty()) {
            return 0;
        }
        taskManager.addTask(new IterativeTask(source, list, 30, 5000));
        sendPlayerLeaveMessage(server, list.size());
        return list.size();
    }

    public static void sendPlayerJoinMessage(MinecraftServer server, int count) {
        if (count > 0) {
            Component message = SUMMON.then("joined").builder(count).setColor(ChatFormatting.YELLOW).build();
            MessageUtils.sendMessage(server, message);
        }
    }

    private static void sendPlayerLeaveMessage(MinecraftServer server, int count) {
        if (count > 0) {
            Component message = SUMMON.then("left").builder(count).setColor(ChatFormatting.YELLOW).build();
            MessageUtils.sendMessage(server, message);
        }
    }

    /**
     * 批量控制玩家丢弃物品
     */
    private int batchDrop(CommandContext<CommandSourceStack> context) {
        int start = IntegerArgumentType.getInteger(context, "start");
        int end = IntegerArgumentType.getInteger(context, "end");
        int temp = Math.min(start, end);
        end = Math.max(start, end);
        start = temp;
        String prefix = StringArgumentType.getString(context, "prefix");
        prefix = getPrefix(prefix);
        MinecraftServer server = context.getSource().getServer();
        int count = 0;
        PlayerList playerManager = server.getPlayerList();
        for (int i = start; i <= end; i++) {
            ServerPlayer player = playerManager.getPlayerByName(prefix + i);
            if (player instanceof EntityPlayerMPFake fakePlayer) {
                EntityPlayerActionPack actionPack = ((ServerPlayerInterface) fakePlayer).getActionPack();
                actionPack.drop(-2, true);
                count++;
            }
        }
        return count;
    }

    // 启用内存泄漏修复
    private boolean fixMemoryLeak(CommandContext<CommandSourceStack> context) {
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_SPAWN_MEMORY_LEAK_FIX.value()) {
            return true;
        }
        // 单击后输入的命令
        String command = CommandProvider.setCarpetRule("fakePlayerSpawnMemoryLeakFix", "true");
        // 文本内容：[这里]
        Component here = TextBuilder.of(LocalizationKeys.Button.HERE.translate())
                .setSuggest(command)
                .setColor(ChatFormatting.AQUA)
                .setHover(LocalizationKeys.Button.INPUT.translate(command))
                .build();
        MessageUtils.sendMessage(context, SCHEDULE.then("relogin").then("prerequisite").translate(here));
        return false;
    }

    // 停止重新上线下线
    private int stopReLogin(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // 获取目标假玩家名
        String name = StringArgumentType.getString(context, "name");
        MinecraftServer server = ServerUtils.getServer(context.getSource());
        ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        Optional<ReLoginTask> optional = manager.stream(ReLoginTask.class)
                .filter(task -> Objects.equals(task.getPlayerName(), name))
                .findFirst();
        if (optional.isEmpty()) {
            throw CommandUtils.createException(SCHEDULE.then("cancel").then("fail").translate());
        }
        optional.ifPresent(task -> task.onCancel(context));
        return 1;
    }

    // 延时上线
    private int addDelayedLoginTask(CommandContext<CommandSourceStack> context, TimeUnit unit) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerTaskManager taskManager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        String name = StringArgumentType.getString(context, "name");
        Optional<DelayedLoginTask> optional = taskManager.stream(DelayedLoginTask.class)
                .filter(task -> Objects.equals(name, task.getPlayerName()))
                .findFirst();
        // 等待时间
        long tick = unit.getDelayed(context);
        Component time = TextBuilder.of(TextProvider.tickToTime(tick)).setHover(TextProvider.tickToRealTime(tick)).build();
        if (optional.isEmpty()) {
            // 添加上线任务
            FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
            taskManager.addTask(new DelayedLoginTask(server, source, serializer, tick));
            LocalizationKey key = SCHEDULE.then("login");
            TextBuilder builder = key.builder(serializer.getDisplayName(), time);
            if (ServerUtils.getPlayer(server, name).isPresent()) {
                builder.setStrikethrough();
                builder.setHover(key.then("online").translate());
            }
            // 发送命令反馈
            MessageUtils.sendMessage(context, builder.build());
        } else {
            // 修改上线时间
            DelayedLoginTask task = optional.get();
            // 为名称添加悬停文本
            TextBuilder builder = TextBuilder.of(name);
            builder.setHover(task.getInfo());
            task.setDelayed(tick);
            MessageUtils.sendMessage(context, SCHEDULE.then("login").then("modify").translate(builder.build(), time));
        }
        return (int) tick;
    }

    // 延迟下线
    private int addDelayedLogoutTask(CommandContext<CommandSourceStack> context, TimeUnit unit) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 获取假玩家延时下线游戏刻数
        long tick = unit.getDelayed(context);
        Component time = TextBuilder.of(TextProvider.tickToTime(tick)).setHover(TextProvider.tickToRealTime(tick)).build();
        ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        Optional<DelayedLogoutTask> optional = manager.stream(DelayedLogoutTask.class)
                .filter(task -> fakePlayer.equals(task.getFakePlayer()))
                .findFirst();
        LocalizationKey key = SCHEDULE.then("logout");
        // 添加新任务
        if (optional.isEmpty()) {
            // 添加延时下线任务
            manager.addTask(new DelayedLogoutTask(server, source, fakePlayer, tick));
            MessageUtils.sendMessage(context, key.translate(fakePlayer.getDisplayName(), time));
        } else {
            // 修改退出时间
            optional.get().setDelayed(tick);
            MessageUtils.sendMessage(context, key.then("modify").translate(fakePlayer.getDisplayName(), time));
        }
        return (int) tick;
    }

    // 取消任务
    private int cancelScheduleTask(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = ServerUtils.getServer(context.getSource());
        ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        String name = StringArgumentType.getString(context, "name");
        // 获取符合条件的任务列表
        List<PlayerScheduleTask> list = manager.stream(PlayerScheduleTask.class)
                .filter(task -> Objects.equals(task.getPlayerName(), name))
                .toList();
        if (list.isEmpty()) {
            throw CommandUtils.createException(SCHEDULE.then("cancel").then("fail").translate());
        }
        list.forEach(task -> task.onCancel(context));
        return list.size();
    }

    // 列出所有任务
    private int listScheduleTask(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = ServerUtils.getServer(context.getSource());
        ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        List<PlayerScheduleTask> list = manager.stream(PlayerScheduleTask.class).toList();
        if (list.isEmpty()) {
            MessageUtils.sendMessage(context, SCHEDULE.then("list").then("empty").translate());
        } else {
            list.forEach(task -> task.sendEachMessage(context.getSource()));
        }
        return list.size();
    }

    private PlayerSerializationManager getSerializationManager(MinecraftServer server) {
        ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
        return coordinator.getPlayerSerializationManager();
    }

    private PlayerSerializationManager getSerializationManager(CommandContext<CommandSourceStack> context) {
        return getSerializationManager(context.getSource().getServer());
    }

    @Override
    public String getDefaultName() {
        return "playerManager";
    }

    /**
     * 时间单位
     */
    public enum TimeUnit {
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
        private long getDelayed(CommandContext<CommandSourceStack> context) {
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
