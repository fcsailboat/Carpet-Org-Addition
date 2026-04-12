package boat.carpetorgaddition.dialog.builder;

import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.NoticeDialog;

@SuppressWarnings("unused")
public final class NoticeDialogBuilder extends DialogBuilder<NoticeDialogBuilder, NoticeDialog> {
    /**
     * 确认对话框的按钮
     */
    private ActionButton action = ActionButtonBuilder.of(LocalizationKeys.Button.CONFIRM.translate()).build();

    private NoticeDialogBuilder(Component title) {
        super(title);
    }

    public static NoticeDialogBuilder of(Component title) {
        return new NoticeDialogBuilder(title);
    }

    public NoticeDialogBuilder setAction(ActionButton action) {
        this.action = action;
        return this;
    }

    @Override
    protected NoticeDialogBuilder self() {
        return this;
    }

    @Override
    public NoticeDialog build() {
        CommonDialogData data = this.createDialogData();
        return new NoticeDialog(data, this.action);
    }
}
