package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.exception.CommandExecuteIOException;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.parcel.Parcel;
import boat.carpetorgaddition.periodic.parcel.ParcelManager;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.page.PageManager;
import boat.carpetorgaddition.wheel.page.PagedCollection;
import boat.carpetorgaddition.wheel.permission.PermissionLevel;
import boat.carpetorgaddition.wheel.permission.PermissionManager;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.screen.SendParcelScreenHandler;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MailCommand extends AbstractServerCommand {
    private final Predicate<CommandSourceStack> intercept = PermissionManager.register("mail.intercept", PermissionLevel.OPS);
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("mail");
    public static final LocalizationKey SEND = KEY.then("send");
    public static final LocalizationKey COLLECT = KEY.then("collect");
    public static final LocalizationKey RECALL = KEY.then("recall");
    public static final LocalizationKey INTERCEPT = KEY.then("intercept");
    public static final LocalizationKey NOTICE = KEY.then("notice");
    public static final LocalizationKey LIST = KEY.then("list");

    public MailCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.COMMAND_MAIL))
                .then(Commands.literal("send")
                        .then(Commands.argument(CommandUtils.PLAYER, GameProfileArgument.gameProfile())
                                .executes(this::send)))
                .then(Commands.literal("collect")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .suggests(collectSuggests(true))
                                .executes(this::collect)))
                .then(Commands.literal("recall")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .suggests(collectSuggests(false))
                                .executes(this::recall)))
                .then(Commands.literal("intercept")
                        .requires(intercept)
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .suggests(interceptSuggests())
                                .executes(this::intercept)))
                .then(Commands.literal("list")
                        .executes(this::list))
                .then(Commands.literal("multiple")
                        .then(Commands.argument(CommandUtils.PLAYER, GameProfileArgument.gameProfile())
                                .executes(this::sendMultipleParcel)))
                .then(Commands.literal("override")
                        .requires(_ -> CarpetOrgAddition.isDebugMode())
                        .executes(this::override)));
    }

    // 自动补全快递单号
    private SuggestionProvider<CommandSourceStack> collectSuggests(boolean recipient) {
        return (context, builder) -> {
            CommandSourceStack source = context.getSource();
            MinecraftServer server = ServerUtils.getServer(source);
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                return SharedSuggestionProvider.suggest(List.of(), builder);
            }
            ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
            ParcelManager manager = coordinator.getParcelManager();
            // 获取所有发送给自己的快递（或所有自己发送的快递）
            List<String> list = manager.stream()
                    .filter(parcel -> recipient ? parcel.isRecipient(player) : parcel.isSender(player))
                    .map(parcel -> Integer.toString(parcel.getId()))
                    .toList();
            return SharedSuggestionProvider.suggest(list, builder);
        };
    }

    // 自动补全快递单号
    private SuggestionProvider<CommandSourceStack> interceptSuggests() {
        return (context, builder) -> {
            CommandSourceStack source = context.getSource();
            MinecraftServer server = ServerUtils.getServer(source);
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                return SharedSuggestionProvider.suggest(List.of(), builder);
            }
            ParcelManager manager = ServerComponentCoordinator.getCoordinator(server).getParcelManager();
            // 获取所有发送的快递
            List<String> list = manager.stream()
                    .map(parcel -> Integer.toString(parcel.getId()))
                    .toList();
            return SharedSuggestionProvider.suggest(list, builder);
        };
    }

    // 发送快递
    private int send(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ServerPlayer sourcePlayer = CommandUtils.getSourcePlayer(context);
        GameProfile gameProfile = CommandUtils.getGameProfile(context, "player");
        Optional<ServerPlayer> optional = ServerUtils.getPlayer(server, gameProfile);
        ParcelManager manager = ServerComponentCoordinator.getCoordinator(server).getParcelManager();
        Parcel parcel;
        if (optional.isEmpty()) {
            parcel = new Parcel(server, sourcePlayer, gameProfile, manager.generateNumber());
        } else {
            ServerPlayer targetPlayer = optional.get();
            // 限制只允许发送给其他真玩家
            checkPlayer(sourcePlayer, targetPlayer);
            parcel = new Parcel(server, sourcePlayer, targetPlayer, manager.generateNumber());
        }
        try {
            // 将快递信息添加到快递管理器
            manager.put(parcel);
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
        return 1;
    }

    // 发送多个快递
    private int sendMultipleParcel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        GameProfile gameProfile = CommandUtils.getGameProfile(context, "player");
        Optional<ServerPlayer> optional = ServerUtils.getPlayer(server, gameProfile);
        if (optional.isPresent()) {
            checkPlayer(player, optional.get());
        }
        SimpleContainer inventory = new SimpleContainer(27);
        PlayerUtils.openScreenHandler(
                player,
                (i, inv, _) -> new SendParcelScreenHandler(i, inv, player, gameProfile, inventory),
                SEND.then("multiple").then("gui").translate()
        );
        return 1;
    }

    // 接收快递
    private int collect(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        // 获取快递
        Parcel parcel = getParcel(context);
        // 只能接收发送给自己的快递
        if (parcel.isRecipient(player)) {
            try {
                parcel.collect();
            } catch (IOException e) {
                throw CommandExecuteIOException.of(e);
            }
            return 1;
        }
        throw CommandUtils.createException(COLLECT.then("not_myself").translate());
    }

    // 撤回快递
    private int recall(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        Parcel parcel = getParcel(context);
        if (parcel.isSender(player)) {
            try {
                parcel.recall();
            } catch (IOException e) {
                throw CommandExecuteIOException.of(e);
            }
            return 1;
        }
        throw CommandUtils.createException(RECALL.then("not_myself").translate());
    }

    /**
     * 拦截一件快递
     */
    private int intercept(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        Parcel parcel = getParcel(context);
        try {
            // 快递被部分撤回后，如果发送者从此不再登录游戏，则物品将永远无法取出
            // 当管理员玩家无法以接收的方式取出物品时，则可以以拦截的方式取出
            parcel.intercept(player);
        } catch (IOException e) {
            IOUtils.loggerError(e);
        }
        return parcel.getId();
    }

    // 调试：将快递数据写入文件
    private int override(CommandContext<CommandSourceStack> context) {
        if (CarpetOrgAddition.isDebugMode()) {
            MinecraftServer server = ServerUtils.getServer(context.getSource());
            ParcelManager manager = ServerComponentCoordinator.getCoordinator(server).getParcelManager();
            manager.stream().forEach(parcel -> {
                try {
                    parcel.save();
                } catch (IOException e) {
                    IOUtils.loggerError(e);
                }
            });
            return (int) manager.stream().count();
        }
        return 0;
    }

    // 列出快递
    private int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ServerPlayer player = CommandUtils.getSourcePlayer(context);
        CommandSourceStack source = context.getSource();
        MinecraftServer server = ServerUtils.getServer(source);
        ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
        ParcelManager manager = coordinator.getParcelManager();
        List<Parcel> list = manager.stream().toList();
        if (list.isEmpty()) {
            // 没有快递被列出
            MessageUtils.sendMessage(context, LIST.then("empty").translate());
            return 0;
        }
        PageManager pageManager = coordinator.getPageManager();
        PagedCollection collection = pageManager.newPagedCollection(source);
        ArrayList<Supplier<Component>> messages = new ArrayList<>();
        for (Parcel parcel : list) {
            messages.add(() -> line(player, parcel));
        }
        collection.addContent(messages);
        MessageUtils.sendEmptyMessage(source);
        MessageUtils.sendMessage(source, LIST.then("head").translate(list.size()));
        collection.print();
        return list.size();
    }

    private Component line(ServerPlayer player, Parcel parcel) {
        Parcel.Operation operation = parcel.getPlayerOperation(player);
        TextBuilder builder = switch (operation) {
            case COLLECT -> TextBuilder.of(LIST.then("collect").translate())
                    .setCommand(CommandProvider.collectParcel(parcel.getId(), false))
                    .setColor(ChatFormatting.AQUA);
            case RECALL -> TextBuilder.of(LIST.then("recall").translate())
                    .setCommand(CommandProvider.recallParcel(parcel.getId(), false))
                    .setColor(ChatFormatting.AQUA);
            case INTERCEPT -> TextBuilder.of(LIST.then("intercept").translate())
                    .setCommand(CommandProvider.interceptParcel(parcel.getId(), false))
                    .setColor(ChatFormatting.AQUA);
            case VIEW -> TextBuilder.of(LIST.then("view").translate())
                    .setColor(ChatFormatting.GRAY);
        };
        TextJoiner joiner = new TextJoiner();
        joiner.newline(LIST.then("id").translate(parcel.getId()));
        joiner.newline(LIST.then("sender").translate(parcel.getSender()));
        joiner.newline(LIST.then("recipient").translate(parcel.getRecipient()));
        joiner.newline(LIST.then("item").translate(parcel.getDisplayName(), parcel.getCount()));
        joiner.newline(LIST.then("time").translate(parcel.getTime()));
        builder.setHover(joiner.join());
        return LIST.then("each").translate(
                parcel.getId(),
                parcel.getDisplayName(),
                parcel.getCount(),
                builder.build()
        );
    }

    // 获取快递
    private Parcel getParcel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = ServerUtils.getServer(context.getSource());
        ParcelManager manager = ServerComponentCoordinator.getCoordinator(server).getParcelManager();
        // 获取快递单号
        int id = IntegerArgumentType.getInteger(context, "id");
        // 查找指定单号的快递
        Optional<Parcel> optional = manager.binarySearch(id);
        if (optional.isEmpty()) {
            throw CommandUtils.createException(KEY.then("invalid_parcel").translate(id));
        }
        return optional.get();
    }

    // 检查玩家是否是自己或假玩家
    private void checkPlayer(ServerPlayer sourcePlayer, ServerPlayer targetPlayer) throws CommandSyntaxException {
        // 允许在开发环境下发送给自己
        if (CarpetOrgAddition.isDebugMode()) {
            return;
        }
        if (sourcePlayer == targetPlayer || targetPlayer instanceof EntityPlayerMPFake) {
            throw CommandUtils.createException(SEND.then("check_player").translate());
        }
    }

    public Predicate<CommandSourceStack> getInterceptPredicate() {
        return this.intercept;
    }

    @Override
    public String getDefaultName() {
        return "mail";
    }
}
