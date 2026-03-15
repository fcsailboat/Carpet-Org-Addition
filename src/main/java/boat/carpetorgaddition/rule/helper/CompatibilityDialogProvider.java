package boat.carpetorgaddition.rule.helper;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.dialog.builder.ActionButtonBuilder;
import boat.carpetorgaddition.dialog.builder.ConfirmationDialogBuilder;
import boat.carpetorgaddition.network.event.CustomClickEvents;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.Dialog;

public class CompatibilityDialogProvider {
    public static Dialog getShulkerBoxStackableDialog(MinecraftServer server) {
        Component component = LocalizationKeys.Rule.Compatibility.WARNING.builder()
                .setColor(ChatFormatting.RED)
                .setBold()
                .build();
        LocalizationKey key = LocalizationKeys.Rule.COMPATIBILITY.then("shulkerBoxStackable");
        final int width = 400;
        ChatFormatting color = ChatFormatting.GRAY;
        Component tis = key.then("tis").translate();
        return ConfirmationDialogBuilder.of(component)
                .addDialogBody(key.then("line").then("0").translate(CarpetOrgAdditionSettings.SHULKER_BOX_STACKABLE.getDisplayName()), width)
                .addDialogBody(key.then("line").then("1").builder().setColor(color).build(), width)
                .addDialogBody(key.then("line").then("2").builder().setHover(tis).setColor(color).setStrikethrough().build(), width)
                .addDialogBody(key.then("line").then("3").builder().setHover(tis).setColor(color).setStrikethrough().build(), width)
                .addDialogBody(key.then("line").then("4").builder().setHover(tis).setColor(color).build(), width)
                .setYesButton(
                        ActionButtonBuilder.of(LocalizationKeys.Rule.Compatibility.STILL_OPEN.translate())
                                .setTooltip(key.then("open").translate())
                                .setCustomClickAction(server, CustomClickEvents.ENABLE_SHULKER_BOX_STACKABLE)
                                .build()
                )
                .setNoButton(
                        ActionButtonBuilder.of(LocalizationKeys.Rule.Compatibility.KEEP_CLOSED.translate())
                                .setTooltip(key.then("close").translate())
                                .build()
                )
                .build();
    }
}
