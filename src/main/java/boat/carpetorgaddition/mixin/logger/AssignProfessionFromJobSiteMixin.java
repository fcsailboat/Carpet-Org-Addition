package boat.carpetorgaddition.mixin.logger;

import boat.carpetorgaddition.logger.Loggers;
import boat.carpetorgaddition.network.handler.LibrarianCommodityPacketHandler;
import boat.carpetorgaddition.network.s2c.LibrarianCommodityCacheInvalidationS2CPacket;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AssignProfessionFromJobSite.class)
public class AssignProfessionFromJobSiteMixin {
    @WrapOperation(method = "lambda$create$6", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;setVillagerData(Lnet/minecraft/world/entity/npc/villager/VillagerData;)V"))
    private static void onUpdateJobSite(Villager instance, VillagerData data, Operation<Void> original) {
        notifyTheClientCacheInvalidation(instance, data);
        original.call(instance, data);
    }

    @Inject(method = "lambda$create$2", at = @At(value = "RETURN"),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/VillagerData;profession()Lnet/minecraft/core/Holder;"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getServer()Lnet/minecraft/server/MinecraftServer;")
            ))
    private static void onUpdateJobSite(CallbackInfoReturnable<BehaviorControl<Villager>> cir, @Local(argsOnly = true, name = "body") Villager body) {
        notifyTheClientCacheInvalidation(body, body.getVillagerData());
    }

    @Unique
    private static void notifyTheClientCacheInvalidation(Villager instance, VillagerData data) {
        if (Loggers.LIBRARIAN.isEnable()) {
            if (data.profession().is(VillagerProfession.LIBRARIAN)) {
                MinecraftServer server = ServerUtils.getServer(instance);
                if (server == null) {
                    return;
                }
                ServerUtils.forEachPlayer(server, player -> {
                    if (Loggers.LIBRARIAN.isSubscribed(player)) {
                        instance.getBrain()
                                .getMemory(MemoryModuleType.JOB_SITE)
                                .filter(globalPos -> globalPos.equals(LibrarianCommodityPacketHandler.PREVIOUS_QUERY_BLOCK_POS.get(player)))
                                .ifPresent(_ -> PlayerUtils.sendNetworkPacket(player, new LibrarianCommodityCacheInvalidationS2CPacket()));
                    }
                });
            }
        }
    }
}
