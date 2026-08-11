package boat.carpetorgaddition.mixin.rule.notoolbreak;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.RuleUtils;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public class PlayerMixin {
    @Unique
    private final Player self = (Player) (Object) this;

    @WrapMethod(method = "attack")
    private void attack(Entity entity, Operation<Void> original) {
        boolean noBreak = RuleUtils.isToolNoBreak(this.self.getMainHandItem(), this.self);
        ScopedValue.where(CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK, noBreak).call(() -> original.call(entity));
    }
}
