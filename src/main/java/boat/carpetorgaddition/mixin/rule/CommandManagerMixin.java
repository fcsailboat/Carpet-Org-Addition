package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ParseBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandManagerMixin {
    @Inject(method = "performCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/Commands;executeCommandInContext(Lnet/minecraft/commands/CommandSourceStack;Ljava/util/function/Consumer;)V"))
    private static void recordCommand(CommandSourceStack executionContext, ParseBox.Result command, String commandString, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.RECORD_PLAYER_COMMAND.value()) {
            CarpetOrgAddition.LOGGER.info("<{}> [Command: /{}]", executionContext.getTextName(), commandString);
        }
    }
}
