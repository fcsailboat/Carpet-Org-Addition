package org.carpetorgaddition.mixin.rule.blockdropsdirectlyenterinventory;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @Shadow
    @Final
    protected ServerPlayerEntity player;

    @WrapMethod(method = "tryBreakBlock")
    private boolean tryBreakBlock(BlockPos pos, Operation<Boolean> original) {
        if (CarpetOrgAdditionSettings.blockDropsDirectlyEnterInventory) {
            try {
                CarpetOrgAdditionSettings.blockBreaking.set(this.player);
                return original.call(pos);
            } finally {
                CarpetOrgAdditionSettings.blockBreaking.remove();
            }
        }
        return original.call(pos);
    }
}
