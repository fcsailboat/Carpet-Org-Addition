package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandManagerMixin {
    // TODO 添加新选项控制非玩家命令是否显示
    @Inject(method = "performCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/Commands;executeCommandInContext(Lnet/minecraft/commands/CommandSourceStack;Ljava/util/function/Consumer;)V"))
    private void recordCommand(ParseResults<CommandSourceStack> command, String commandString, CallbackInfo ci) {
        switch (CarpetOrgAdditionSettings.RECORD_SERVER_COMMAND.value()) {
            case ALL -> {
                CommandSourceStack source = command.getContext().getSource();
                record(commandString, source);
            }
            case ONLY_PLAYER -> {
                CommandSourceStack source = command.getContext().getSource();
                if (source.isPlayer()) {
                    record(commandString, source);
                }
            }
            case FALSE -> {
            }
        }
    }

    @Unique
    private static void record(String command, CommandSourceStack source) {
        CarpetOrgAddition.LOGGER.info("<{}> [Command: /{}]", source.getTextName(), command);
    }
}
