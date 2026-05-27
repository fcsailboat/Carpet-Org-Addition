package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.periodic.task.search.OfflinePlayerSearchTask;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    // 通过阻止Fabric假玩家存入this.advancements集合以修复/finder命令查询离线玩家物品栏时，升级旧玩家数据产生的内存泄漏问题
    // 但是，this.stats看起来也存储了玩家数据，为什么并没有产生明显内存泄漏？（在26.2-snapshot-8中测试）
    @WrapOperation(method = "getPlayerAdvancements", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private <K, V> V put(Map<K, V> instance, K k, V v, Operation<V> original, @Local(argsOnly = true, name = "player") ServerPlayer player) {
        if (OfflinePlayerSearchTask.UPGRADING.orElse(false) && player instanceof FakePlayer) {
            return null;
        } else {
            return original.call(instance, k, v);
        }
    }
}
