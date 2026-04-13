package boat.carpetorgaddition.dialog;

import boat.carpetorgaddition.network.event.ActionSource;
import boat.carpetorgaddition.network.event.CustomClickAction;
import boat.carpetorgaddition.network.event.CustomClickEvents;
import boat.carpetorgaddition.wheel.nbt.NbtWriter;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.action.StaticAction;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class DialogUtils {
    public static final Component UNDEFINED = LocalizationKeys.Misc.UNDEFINED.builder()
            .setColor(ChatFormatting.RED)
            .setBold()
            .build();

    private DialogUtils() {
    }

    public static String toValidDialogKey(String key) {
        int length = key.length();
        if (length > 32) {
            throw new IllegalArgumentException("The key is too long: " + key);
        }
        for (int i = 0; i < length; i++) {
            char c = key.charAt(i);
            if ((c >= 'a' && c <= 'z') || c == '_') {
                continue;
            }
            throw new IllegalArgumentException("Invalid key: %s, can only contain lowercase letters and underscores".formatted(key));
        }
        return key;
    }

    public static ActionButton createBackButton(MinecraftServer server, @Nullable Identifier parent) {
        if (parent == null) {
            CommonButtonData data = new CommonButtonData(LocalizationKeys.Button.CLOSE.translate(), CommonButtonData.DEFAULT_WIDTH);
            return new ActionButton(data, Optional.empty());
        } else {
            CommonButtonData data = new CommonButtonData(LocalizationKeys.Button.BACK.translate(), CommonButtonData.DEFAULT_WIDTH);
            NbtWriter writer = new NbtWriter(server, CustomClickAction.CURRENT_VERSION);
            writer.putIdentifier("id", parent);
            writer.putActionSource(ActionSource.DIALOG);
            Action action = new StaticAction(new ClickEvent.Custom(CustomClickEvents.OPEN_DIALOG, Optional.of(writer.toNbt())));
            return new ActionButton(data, Optional.of(action));
        }
    }
}
