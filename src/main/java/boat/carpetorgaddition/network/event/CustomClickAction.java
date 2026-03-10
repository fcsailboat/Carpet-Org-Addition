package boat.carpetorgaddition.network.event;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.dialog.DialogProvider;
import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.wheel.nbt.NbtReader;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.Map;

@NullMarked
public class CustomClickAction {
    private static final Map<Identifier, CustomClickActionProcessor> ACTIONS = new HashMap<>();
    public static final int CURRENT_VERSION = 1;

    static {
        CustomClickEvents.init();
    }

    public static void register(Identifier identifier, CustomClickActionProcessor consumer) {
        CustomClickActionProcessor value = ACTIONS.put(identifier, consumer);
        if (value != null) {
            throw new IllegalStateException("Duplicate registration: " + identifier);
        }
    }

    public static void accept(Identifier identifier, CustomClickActionContext context) {
        CustomClickActionProcessor processor = ACTIONS.get(identifier);
        if (processor == null) {
            return;
        }
        try {
            NbtReader reader = context.getReader();
            if (reader.getVersion() > CURRENT_VERSION) {
                ServerPlayer player = context.getPlayer();
                if (context.getActionSource() == ActionSource.DIALOG) {
                    // 关闭当前对话框（等待服务器响应屏幕）
                    player.closeContainer();
                }
                PlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(player);
                if (coordinator.isVersionMismatchNotified()) {
                    return;
                }
                Component message = LocalizationKeys.CustomClickAction.EXPIRED.builder().setColor(ChatFormatting.RED).build();
                MessageUtils.sendMessage(context.getPlayer(), message);
                coordinator.markVersionMismatchNotified();
                return;
            }
        } catch (NullPointerException e) {
            CarpetOrgAddition.LOGGER.warn("{} contains no nbt data", identifier, e);
        }
        try {
            processor.accept(context);
        } catch (CommandSyntaxException e) {
            switch (context.getActionSource()) {
                case CHAT -> MessageUtils.sendVanillaErrorMessage(context.getSource(), e);
                case DIALOG -> {
                    DialogProvider provider = ServerComponentCoordinator.getCoordinator(context.getServer()).getDialogProvider();
                    Dialog dialog = provider.createErrorNoticeDialog(e);
                    PlayerUtils.openDialog(context.getPlayer(), dialog);
                }
                case SIGN -> MessageUtils.sendErrorMessageToHud(context.getSource(), e);
                case UNKNOWN -> {
                    if (CarpetOrgAddition.isDebugMode()) {
                        CarpetOrgAddition.LOGGER.warn("Unexpected problem while executing custom action from unknown source", e);
                    }
                }
            }
        } catch (RuntimeException e) {
            CarpetOrgAddition.LOGGER.error("Unexpected error while processing custom click action for identifier [{}]: ", identifier, e);
        }
    }

    @FunctionalInterface
    public interface CustomClickActionProcessor {
        void accept(CustomClickActionContext context) throws CommandSyntaxException;
    }
}
