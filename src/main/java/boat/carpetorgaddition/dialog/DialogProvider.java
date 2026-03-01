package boat.carpetorgaddition.dialog;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.dialog.builder.*;
import boat.carpetorgaddition.network.event.CustomClickEvents;
import boat.carpetorgaddition.network.event.CustomClickKeys;
import boat.carpetorgaddition.util.IdentifierUtils;
import boat.carpetorgaddition.wheel.inventory.PlayerInventoryType;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@NullMarked
public class DialogProvider {
    private final Map<Identifier, Supplier<Dialog>> dialogs = new HashMap<>();
    public static final Identifier START = getDialogIdentifier("start");
    public static final Identifier FUNCTION = getDialogIdentifier("function");
    public static final Identifier OPEN_INVENTORY = getDialogIdentifier("open_inventory");
    public static final Identifier QUERY_PLAYER_NAME = getDialogIdentifier("query_player_name");
    private final MinecraftServer server;

    public DialogProvider(MinecraftServer server) {
        this.server = server;
        this.init();
    }

    private void init() {
        // 对话框主屏幕
        dialogs.put(START, () -> {
            Component version = TextBuilder.of(CarpetOrgAdditionConstants.VERSION)
                    .setHover(CarpetOrgAdditionConstants.BUILD_TIMESTAMP)
                    .build();
            Component component = TextBuilder.create(CarpetOrgAdditionConstants.MOD_NAME);
            Component translate = LocalizationKeys.Dialog.Text.VERSION.translate(version);
            return DialogListDialogBuilder.of(component)
                    .addDialogBody(translate)
                    .addDialog(getDialog(FUNCTION))
                    .setParent(this.server, null)
                    .build();
        });
        // 功能列表对话框
        dialogs.put(FUNCTION, () -> DialogListDialogBuilder.of(LocalizationKeys.Dialog.Title.FUNCTION.translate())
                .addDialog(getDialog(OPEN_INVENTORY))
                .addDialog(getDialog(QUERY_PLAYER_NAME))
                .setAfterAction(DialogAction.WAIT_FOR_RESPONSE)
                .setParent(this.server, START)
                .build()
        );
        // 打开玩家物品栏对话框
        dialogs.put(OPEN_INVENTORY, () -> MultiActionDialogBuilder.of(LocalizationKeys.Dialog.Title.OPEN_INVENTORY.translate())
                .addActionButton(
                        ActionButtonBuilder.of(LocalizationKeys.Button.CONFIRM.translate())
                                .setCustomClickAction(this.server, CustomClickEvents.OPEN_INVENTORY)
                                .build()
                )
                .setAfterAction(DialogAction.WAIT_FOR_RESPONSE)
                .addInput(
                        TextInputBuilder.of(CustomClickKeys.UUID)
                                .setLabel(LocalizationKeys.Dialog.Textbox.UUID.translate())
                                .setMaxLength(36)
                                .build()
                )
                .addInput(
                        SingleOptionInputBuilder.of(CustomClickKeys.INVENTORY_TYPE)
                                .setLabelVisible(false)
                                .addEntry(PlayerInventoryType.INVENTORY.toString(), PlayerInventoryType.INVENTORY.getDisplayName())
                                .addEntry(PlayerInventoryType.ENDER_CHEST.toString(), PlayerInventoryType.ENDER_CHEST.getDisplayName())
                                .build()
                )
                .setParent(this.server, FUNCTION)
                .build()
        );
        // 查询玩家名称对话框
        dialogs.put(QUERY_PLAYER_NAME, () -> MultiActionDialogBuilder.of(LocalizationKeys.Dialog.Title.QUERY_PLAYER_NAME.translate())
                .addInput(
                        TextInputBuilder.of(CustomClickKeys.UUID)
                                .setLabel(LocalizationKeys.Dialog.Textbox.UUID.translate())
                                .setMaxLength(36)
                                .build()
                )
                .addActionButton(
                        ActionButtonBuilder.of(LocalizationKeys.Button.CONFIRM.translate())
                                .setCustomClickAction(this.server, CustomClickEvents.QUERY_PLAYER_NAME)
                                .build()
                )
                .setAfterAction(DialogAction.WAIT_FOR_RESPONSE)
                .setParent(this.server, FUNCTION)
                .build()
        );
    }

    public Dialog getDialog(Identifier key) {
        return Objects.requireNonNull(dialogs.get(key).get(), () -> "Unknown dialog: " + key);
    }

    public Dialog createErrorNoticeDialog(CommandSyntaxException exception) {
        return NoticeDialogBuilder.of(
                        LocalizationKeys.Dialog.Title.ERROR.builder()
                                .setColor(ChatFormatting.RED)
                                .setBold()
                                .build()
                )
                .addDialogBody(exception)
                .build();
    }

    public Set<Map.Entry<Identifier, Dialog>> entrySet() {
        return this.dialogs.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toSet());
    }

    private static Identifier getDialogIdentifier(String id) {
        return IdentifierUtils.ofIdentifier("dialog/" + id);
    }
}
