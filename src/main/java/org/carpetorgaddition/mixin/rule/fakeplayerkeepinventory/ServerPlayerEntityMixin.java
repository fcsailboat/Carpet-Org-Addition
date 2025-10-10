package org.carpetorgaddition.mixin.rule.fakeplayerkeepinventory;

import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.RuleUtils;
import org.carpetorgaddition.rule.value.FakePlayerKeepInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Unique
    private final ServerPlayerEntity thisPlayer = (ServerPlayerEntity) (Object) this;

    @WrapOperation(method = "copyFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameRules;getBoolean(Lnet/minecraft/world/GameRules$Key;)Z"))
    private boolean keepItem(GameRules instance, GameRules.Key<GameRules.BooleanRule> rule, Operation<Boolean> original) {
        if(CarpetOrgAdditionSettings.fakePlayerKeepInventory.get() == FakePlayerKeepInventory.TRUE && thisPlayer instanceof EntityPlayerMPFake fakePlayer) {
            if(RuleUtils.shouldKeepInventory(fakePlayer)){
                return true;
            }else{
                original.call(instance, rule);
            }
        }else if (CarpetOrgAdditionSettings.fakePlayerKeepInventory.get() == FakePlayerKeepInventory.FALSE && thisPlayer instanceof EntityPlayerMPFake fakePlayer){
            if(RuleUtils.shouldKeepInventory(fakePlayer)){
                return true;
            }else{
                original.call(instance, rule);
            }
        }
        return false;
    }


}
