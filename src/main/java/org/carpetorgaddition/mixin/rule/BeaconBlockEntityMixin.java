package org.carpetorgaddition.mixin.rule;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.client.renderer.waypoint.WaypointRenderManager;
import org.carpetorgaddition.util.wheel.BeaconRangeBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(BeaconBlockEntity.class)
//大范围信标
public class BeaconBlockEntityMixin {
    // TODO 移除测试代码；信标范围向上扩展一格
    @WrapOperation(method = "applyPlayerEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getNonSpectatingEntities(Ljava/lang/Class;Lnet/minecraft/util/math/Box;)Ljava/util/List;"))
    private static List<PlayerEntity> box(World instance, Class<PlayerEntity> aClass, Box box, Operation<List<PlayerEntity>> original) {
        BeaconRangeBox beaconRangeBox = new BeaconRangeBox(box);
        // 调整信标范围
        if (CarpetOrgAdditionSettings.beaconRangeExpand != 0 && CarpetOrgAdditionSettings.beaconRangeExpand <= 1024) {
            beaconRangeBox = beaconRangeBox.modify(CarpetOrgAdditionSettings.beaconRangeExpand);
        }
        // 调整信标高度
        if (CarpetOrgAdditionSettings.beaconWorldHeight) {
            beaconRangeBox = beaconRangeBox.worldHeight(instance);
        }
        WaypointRenderManager.beaconBox = beaconRangeBox;
        return original.call(instance, aClass, beaconRangeBox);
    }
}
