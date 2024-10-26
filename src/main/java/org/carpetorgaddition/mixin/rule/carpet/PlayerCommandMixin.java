package org.carpetorgaddition.mixin.rule.carpet;

import carpet.commands.PlayerCommand;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerCommand.class)
public class PlayerCommandMixin {
    // 显示假玩家召唤者
    @WrapOperation(method = "spawn", at = @At(value = "INVOKE", target = "Lcarpet/patches/EntityPlayerMPFake;createFake(Ljava/lang/String;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/util/math/Vec3d;DDLnet/minecraft/registry/RegistryKey;Lnet/minecraft/world/GameMode;Z)Z"))
    private static boolean spawn(
            String username,
            MinecraftServer server,
            Vec3d pos,
            double yaw,
            double pitch,
            RegistryKey<World> dimensionId,
            GameMode gamemode,
            boolean flying,
            Operation<Boolean> original,
            @Local(argsOnly = true) CommandContext<ServerCommandSource> context
    ) {
        // 检查玩家是否成功召唤
        boolean success = original.call(username, server, pos, yaw, pitch, dimensionId, gamemode, flying);
        if (success && CarpetOrgAdditionSettings.displayFakePlayerSummoner) {
            ServerPlayerEntity player = context.getSource().getPlayer();
            if (player == null) {
                return true;
            }
            MutableText message = TextUtils.translate(
                    "carpet.rule.message.displayFakePlayerSummoner",
                    player.getDisplayName(), username
            );
            // 将消息设置为灰色斜体
            message = TextUtils.toGrayItalic(message);
            // 对所有玩家发送命令反馈
            MessageUtils.broadcastMessage(context.getSource(), message);
            CarpetOrgAddition.LOGGER.info("{}召唤了{}于{}[{}]",
                    player.getName().getString(), username,
                    dimensionId.getValue().toString(),
                    WorldUtils.toPosString(BlockPos.ofFloored(pos))
            );
        }
        return success;
    }
}
