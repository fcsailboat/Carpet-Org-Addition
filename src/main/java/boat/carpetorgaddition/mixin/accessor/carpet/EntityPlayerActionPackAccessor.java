package boat.carpetorgaddition.mixin.accessor.carpet;

import carpet.helpers.EntityPlayerActionPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(EntityPlayerActionPack.class)
public interface EntityPlayerActionPackAccessor {
    @Accessor(remap = false)
    Map<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action> getActions();

    @Accessor(value = "forward", remap = false)
    float getForward();

    @Mixin(EntityPlayerActionPack.Action.class)
    interface ActionAccessor {
        @Accessor(value = "isContinuous", remap = false)
        boolean isContinuous();
    }
}
