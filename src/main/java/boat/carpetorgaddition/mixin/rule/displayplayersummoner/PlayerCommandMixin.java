package boat.carpetorgaddition.mixin.rule.displayplayersummoner;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import carpet.commands.PlayerCommand;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = PlayerCommand.class, remap = false)
public class PlayerCommandMixin {
    @WrapMethod(method = "spawn")
    private static int spawn(CommandContext<CommandSourceStack> context, Operation<Integer> original) {
        try {
            CarpetOrgAdditionSettings.PLAYER_SUMMONER.set(context.getSource().getPlayer());
            return original.call(context);
        } finally {
            CarpetOrgAdditionSettings.PLAYER_SUMMONER.remove();
        }
    }
}
