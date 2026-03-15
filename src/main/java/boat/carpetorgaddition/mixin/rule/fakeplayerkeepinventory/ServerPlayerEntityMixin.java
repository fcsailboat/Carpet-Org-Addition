package boat.carpetorgaddition.mixin.rule.fakeplayerkeepinventory;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.RuleUtils;
import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
public class ServerPlayerEntityMixin {
    @Unique
    private final ServerPlayer thisPlayer = (ServerPlayer) (Object) this;

    @SuppressWarnings("unchecked")
    @WrapOperation(method = "restoreFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
    private <T> T keepItem(GameRules instance, GameRule<T> rule, Operation<T> original) {
        if (this.shouldKeepInventory() && rule.gameRuleType() == GameRuleType.BOOL) {
            return (T) Boolean.TRUE;
        }
        return original.call(instance, rule);
    }

    @Unique
    private boolean shouldKeepInventory() {
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_KEEP_INVENTORY.value() && thisPlayer instanceof EntityPlayerMPFake fakePlayer) {
            return RuleUtils.shouldKeepInventory(fakePlayer);
        }
        return false;
    }
}
