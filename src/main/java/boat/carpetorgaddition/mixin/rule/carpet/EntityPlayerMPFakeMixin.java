package boat.carpetorgaddition.mixin.rule.carpet;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.ServerUtils;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerMPFake.class)
public class EntityPlayerMPFakeMixin extends ServerPlayer {
    @Unique
    private final EntityPlayerMPFake thisPlayer = (EntityPlayerMPFake) (Object) this;

    @SuppressWarnings("unused")
    private EntityPlayerMPFakeMixin(MinecraftServer server, ServerLevel world, GameProfile profile, ClientInformation clientOptions) {
        super(server, world, profile, clientOptions);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void fakePlayerTick(CallbackInfo ci) {
        // 假玩家回血
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_HEAL.value()) {
            long time = ServerUtils.getWorld(thisPlayer).getGameTime();
            if (time % 40 == 0) {
                // 回复血量
                thisPlayer.heal(1);
                // 回复饥饿值
                thisPlayer.getFoodData().eat(1, 0);
            }
        }
    }
}
