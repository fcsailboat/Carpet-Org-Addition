package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.wheel.BeaconRangeBox;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
    // 大范围信标
    @WrapOperation(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private static List<Player> box(Level world, Class<Player> aClass, AABB box, Operation<List<Player>> original) {
        BeaconRangeBox beaconRangeBox = new BeaconRangeBox(box);
        // 调整信标范围
        int range = CarpetOrgAdditionSettings.BEACON_RANGE_EXPAND.value();
        if (range != 0 && range <= 1024) {
            beaconRangeBox = beaconRangeBox.modify(range);
        }
        // 调整信标高度
        if (CarpetOrgAdditionSettings.BEACON_WORLD_HEIGHT.value()) {
            beaconRangeBox = beaconRangeBox.worldHeight(world);
        }
        return original.call(world, aClass, beaconRangeBox);
    }
}
