package org.carpetorgaddition.mixin.util;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import org.carpetorgaddition.util.wheel.CommandRegistryAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(CommandManager.class)
public class CommandRegistryAccessMixin implements CommandRegistryAccessor {
    @Unique
    private CommandRegistryAccess commandRegistryAccess;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CommandManager.RegistrationEnvironment environment, CommandRegistryAccess commandRegistryAccess, CallbackInfo ci) {
        this.commandRegistryAccess = commandRegistryAccess;
    }


    @Override
    public CommandRegistryAccess getAccess() {
        return this.commandRegistryAccess;
    }
}
