package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.RuleUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.commands.GameRuleCommand;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionProviderCheck;
import net.minecraft.server.permissions.PermissionSetSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRuleCommand.class)
public class GameRuleCommandMixin {
    //开放/gamerule命令权限
    @WrapOperation(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/Commands;hasPermission(Lnet/minecraft/server/permissions/PermissionCheck;)Lnet/minecraft/server/permissions/PermissionProviderCheck;"))
    private static <T extends PermissionSetSupplier> PermissionProviderCheck<T> privilege(PermissionCheck permissionCheck, Operation<PermissionProviderCheck<T>> original) {
        return RuleUtils.requireOrOpenPermissionLevel(CarpetOrgAdditionSettings.OPEN_GAME_RULE_PERMISSION, original.call(permissionCheck));
    }
}
