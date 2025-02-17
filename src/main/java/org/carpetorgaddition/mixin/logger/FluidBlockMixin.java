package org.carpetorgaddition.mixin.logger;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.logger.FunctionLogger;
import org.carpetorgaddition.logger.LoggerRegister;
import org.carpetorgaddition.logger.Loggers;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FluidBlock.class)
public class FluidBlockMixin {
    @WrapOperation(method = "receiveNeighborFluids", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z", ordinal = 0))
    private boolean receiveNeighborFluids(World world, BlockPos pos, BlockState state, Operation<Boolean> original) {
        if (LoggerRegister.obsidian && state.isOf(Blocks.OBSIDIAN)) {
            MinecraftServer server = world.getServer();
            if (server != null) {
                FunctionLogger logger = Loggers.getObsidianLogger();
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (logger.isSubscribed(player)) {
                        MutableText text = TextConstants.blockPos(pos, WorldUtils.getColor(world));
                        MessageUtils.sendMessage(player, "carpet.logger.obsidian.generate", text);
                    }
                }
            }
        }
        return original.call(world, pos, state);
    }
}
