package boat.carpetorgaddition.mixin.rule.displayplayersummoner;

import boat.carpetorgaddition.wheel.FakePlayerSpawner;
import carpet.commands.PlayerCommand;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

@Mixin(value = PlayerCommand.class, remap = false)
public class PlayerCommandMixin {
    @WrapMethod(method = "spawn")
    private static int spawn(CommandContext<CommandSourceStack> context, Operation<Integer> original) {
        ServerPlayer player = context.getSource().getPlayer();
        return ScopedValue.where(FakePlayerSpawner.SUMMONER, Optional.ofNullable(player)).call(() -> original.call(context));
    }
}
