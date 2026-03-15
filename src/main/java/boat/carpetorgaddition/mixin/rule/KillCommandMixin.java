package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.CommandUtils;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.commands.KillCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;

@Mixin(KillCommand.class)
public class KillCommandMixin {
    // 创造玩家免疫kill
    @Inject(method = "kill", at = @At(value = "HEAD"))
    private static void execute(CallbackInfoReturnable<Integer> cir, @Local(argsOnly = true) LocalRef<Collection<? extends Entity>> reference) throws CommandSyntaxException {
        if (CarpetOrgAdditionSettings.CREATIVE_IMMUNE_KILL.value()) {
            Collection<? extends Entity> entities = reference.get();
            List<? extends Entity> list = entities.stream().filter(entity -> !isCreativePlayer(entity)).toList();
            if (list.isEmpty()) {
                throw CommandUtils.createEntityNotFoundException();
            }
            reference.set(list);
        }
    }

    // 指定玩家是否是创造或旁观模式的玩家
    @Unique
    private static boolean isCreativePlayer(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return player.isCreative() || player.isSpectator();
        }
        return false;
    }
}
