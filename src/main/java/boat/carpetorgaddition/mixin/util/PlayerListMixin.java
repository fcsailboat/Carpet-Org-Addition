package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.periodic.task.search.AbstractOfflinePlayerSearchTask;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.UUID;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Shadow
    @Final
    private Map<UUID, ServerStatsCounter> stats;

    // 通过阻止Fabric假玩家存入this.advancements集合以修复/finder命令查询离线玩家物品栏时，升级旧玩家数据产生的内存泄漏问题
    @WrapOperation(method = "getPlayerAdvancements", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private <K, V> V put(Map<K, V> instance, K k, V v, Operation<V> original, @Local(argsOnly = true, name = "player") ServerPlayer player) {
        if (AbstractOfflinePlayerSearchTask.UPGRADING.orElse(false) && player instanceof FakePlayer) {
            return null;
        } else {
            return original.call(instance, k, v);
        }
    }

    @WrapMethod(method = "getPlayerStats")
    private ServerStatsCounter getPlayerStats(Player player, Operation<ServerStatsCounter> original) {
        ServerStatsCounter serverStatsCounter = original.call(player);
        if (AbstractOfflinePlayerSearchTask.UPGRADING.orElse(false) && player instanceof FakePlayer) {
            this.stats.remove(player.getUUID(), serverStatsCounter);
        }
        return serverStatsCounter;
    }
}
