package org.carpetorgaddition.mixin.rule;

import com.mojang.brigadier.ParseResults;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
    @Inject(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/command/CommandManager;callWithContext(Lnet/minecraft/server/command/ServerCommandSource;Ljava/util/function/Consumer;)V"))
    private void recordCommand(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.recordPlayerCommand) {
            ServerCommandSource source = parseResults.getContext().getSource();
            CarpetOrgAddition.LOGGER.info("<%s> [Command: /%s]".formatted(source.getName(), command));
        }
    }
}
