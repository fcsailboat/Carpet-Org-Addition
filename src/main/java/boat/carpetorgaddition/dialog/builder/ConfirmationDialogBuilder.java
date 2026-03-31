package boat.carpetorgaddition.dialog.builder;

import boat.carpetorgaddition.wheel.text.LocalizationKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.ConfirmationDialog;

@SuppressWarnings("unused")
public class ConfirmationDialogBuilder extends DialogBuilder<ConfirmationDialogBuilder, ConfirmationDialog> {
    private ActionButton yesButton = ActionButtonBuilder.of(LocalizationKey.literal("gui.yes").translate()).build();
    private ActionButton noButton = ActionButtonBuilder.of(LocalizationKey.literal("gui.cancel").translate()).build();

    private ConfirmationDialogBuilder(Component title) {
        super(title);
    }

    public static ConfirmationDialogBuilder of(Component title) {
        return new ConfirmationDialogBuilder(title);
    }

    public ConfirmationDialogBuilder setYesButton(ActionButton yesButton) {
        this.yesButton = yesButton;
        return this;
    }

    public ConfirmationDialogBuilder setNoButton(ActionButton noButton) {
        this.noButton = noButton;
        return this;
    }

    @Override
    protected ConfirmationDialogBuilder self() {
        return this;
    }

    @Override
    public ConfirmationDialog build() {
        return new ConfirmationDialog(this.createDialogData(), this.yesButton, this.noButton);
    }
}
