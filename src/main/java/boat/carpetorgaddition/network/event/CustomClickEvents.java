package boat.carpetorgaddition.network.event;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.PlayerCommandExtension;
import boat.carpetorgaddition.dialog.DialogProvider;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.rule.RuleAccessor;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.IdentifierUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.wheel.GameProfileCache;
import boat.carpetorgaddition.wheel.inventory.PlayerInventoryType;
import boat.carpetorgaddition.wheel.nbt.NbtReader;
import boat.carpetorgaddition.wheel.page.PageManager;
import boat.carpetorgaddition.wheel.page.PagedCollection;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

public class CustomClickEvents {
    /**
     * 打开对话框
     */
    public static final Identifier OPEN_DIALOG = register("open_dialog", context -> {
        NbtReader reader = context.getReader();
        Identifier id = reader.getIdentifierOrThrow("id");
        MinecraftServer server = context.getServer();
        DialogProvider provider = ServerComponentCoordinator.getCoordinator(server).getDialogProvider();
        Dialog dialog = provider.getDialog(id);
        PlayerUtils.openDialog(context.getPlayer(), dialog);
    });
    /**
     * 打开玩家物品栏
     */
    public static final Identifier OPEN_INVENTORY = register("open_inventory", context -> {
        NbtReader reader = context.getReader();
        UUID uuid = reader.getUuidNullable(CustomClickKeys.UUID).orElseThrow(() -> unableToResolveUuid(reader));
        PlayerInventoryType type = reader.getPlayerInventoryTypeOrThrow(CustomClickKeys.INVENTORY_TYPE);
        PlayerCommandExtension.openInventory(context.getPlayer(), type, new PlayerCommandExtension.WithCheckPlayerInventoryAccessor(context.getServer(), uuid, context.getPlayer()));
    });
    /**
     * 查询玩家名称
     */
    public static final Identifier QUERY_PLAYER_NAME = register("query_player_name", context -> {
        try {
            NbtReader reader = context.getReader();
            UUID uuid = reader.getUuidNullable(CustomClickKeys.UUID).orElseThrow(() -> unableToResolveUuid(reader));
            ServerPlayer player = context.getPlayer();
            if (context.getActionSource() == ActionSource.DIALOG) {
                // 关闭当前对话框（等待服务器响应屏幕）
                player.closeContainer();
            }
            GameProfileCache cache = GameProfileCache.getInstance();
            Optional<String> optional = cache.get(uuid);
            if (optional.isPresent()) {
                // 如果本地存在，就不再从Mojang API获取
                String playerName = optional.get();
                TextBuilder builder = TextBuilder.of("UUID");
                builder.setHover(uuid.toString());
                sendQueryPlayerNameFeekback(player, builder.build(), playerName);
            } else {
                // 本地不存在，从Mojang API获取
                GameProfileCache.QUERY_PLAYER_NAME_THREAD_POOL.submit(() -> queryPlayerName(context.getServer(), player, uuid));
                MessageUtils.sendMessage(player, LocalizationKeys.Operation.QueryPlayerName.START.translate());
            }
        } catch (RejectedExecutionException e) {
            // 只允许同时存在一个线程执行查询任务
            throw CommandUtils.createException(LocalizationKeys.Operation.WAIT_LAST.translate());
        }
    });
    /**
     * 翻页
     */
    public static final Identifier TURN_THE_PAGE = register("turn_the_page", context -> {
        NbtReader reader = context.getReader();
        int id = reader.getIntOrThrow(CustomClickKeys.ID);
        int page = reader.getIntOrThrow(CustomClickKeys.PAGE_NUMBER);
        PageManager manager = ServerComponentCoordinator.getCoordinator(context.getServer()).getPageManager();
        Optional<PagedCollection> optional = manager.get(id);
        if (optional.isPresent()) {
            PagedCollection collection = optional.get();
            collection.print(page, true);
        } else {
            throw CommandUtils.createException(LocalizationKeys.Operation.Page.NON_EXISTENT.translate());
        }
    });
    /**
     * 启用潜影盒堆叠
     */
    public static final Identifier ENABLE_SHULKER_BOX_STACKABLE = register("enable_shulker_box_stackable", context -> {
        RuleAccessor<Boolean> accessor = CarpetOrgAdditionSettings.shulkerBoxStackable;
        CommandSourceStack source = context.getSource();
        ScopedValue.where(CarpetOrgAdditionSettings.CONFIRM_ENABLE, true).run(() -> accessor.setRuleValue(source, true));
    });

    /**
     * 在独立线程查询玩家名称
     */
    private static void queryPlayerName(MinecraftServer server, ServerPlayer player, UUID uuid) {
        TextBuilder builder = TextBuilder.of("UUID");
        builder.setHover(uuid.toString());
        Component displayUuid = builder.build();
        String name;
        try {
            name = GameProfileCache.queryPlayerNameFromMojangApi(uuid);
        } catch (IOException e) {
            CommandSourceStack source = player.createCommandSourceStack();
            MessageUtils.sendErrorMessage(source, LocalizationKeys.Operation.QueryPlayerName.FAIL.translate(displayUuid), e);
            return;
        }
        GameProfileCache cache = GameProfileCache.getInstance();
        cache.put(uuid, name);
        // 在服务器线程发送命令反馈
        server.execute(() -> sendQueryPlayerNameFeekback(player, displayUuid, name));
    }

    private static void sendQueryPlayerNameFeekback(ServerPlayer player, Component uuid, String playerName) {
        Component name = TextBuilder.of(playerName).setCopyToClipboard(playerName).setColor(ChatFormatting.GRAY).build();
        MessageUtils.sendMessage(player, LocalizationKeys.Operation.QueryPlayerName.SUCCESS.translate(uuid, name));
    }

    /**
     * 字符串无法解析为UUID
     */
    private static CommandSyntaxException unableToResolveUuid(NbtReader reader) {
        String uuid = reader.getString(CustomClickKeys.UUID);
        Component component = LocalizationKeys.Operation.UNABLE_TO_PARSE_STRING_TO_UUID.translate(uuid);
        return CommandUtils.createException(component);
    }

    private static Identifier register(String id, CustomClickAction.CustomClickActionProcessor processor) {
        Identifier identifier = IdentifierUtils.ofIdentifier(id);
        CustomClickAction.register(identifier, processor);
        return identifier;
    }

    /**
     * 空方法，用于触发类加载
     */
    public static void init() {
    }
}
