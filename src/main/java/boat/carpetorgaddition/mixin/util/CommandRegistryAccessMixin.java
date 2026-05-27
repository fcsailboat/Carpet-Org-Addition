package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.wheel.CommandRegistryAccessor;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandRegistryAccessMixin implements CommandRegistryAccessor {
    @Unique
    private CommandBuildContext commandRegistryAccess;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Commands.CommandSelection commandSelection, CommandBuildContext context, CallbackInfo ci) {
        this.commandRegistryAccess = context;
    }


    @Override
    public CommandBuildContext carpet_Org_Addition$getAccess() {
        return this.commandRegistryAccess;
    }
}
