package boat.carpetorgaddition.mixin.debug;

import boat.carpetorgaddition.debug.DebugSettings;
import boat.carpetorgaddition.debug.OnlyDeveloped;
import boat.carpetorgaddition.wheel.ItemStackCounter;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@OnlyDeveloped
@Mixin(ItemContainerContents.class)
public class ItemContainerContentsMixin {
    @Shadow
    @Final
    private List<Optional<ItemStackTemplate>> items;

    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components, CallbackInfo ci) {
        if (DebugSettings.mergeShulkerTooltip.get()) {
            ItemStackCounter counter = new ItemStackCounter();
            this.items.forEach(optional -> optional.ifPresent(counter::add));
            LocalizationKey key = LocalizationKey.literal("item.container.item_count");
            for (Object2IntMap.Entry<ItemStack> entry : counter.entrySet()) {
                consumer.accept(key.translate(entry.getKey().getHoverName(), entry.getIntValue()));
            }
            ci.cancel();
        }
    }
}
