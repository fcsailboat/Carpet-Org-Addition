package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {
    // 将镐作为基岩的有效采集工具
    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void miningSpeed(ItemStack itemStack, BlockState state, CallbackInfoReturnable<Float> cir) {
        if (CarpetOrgAdditionSettings.PICKAXE_MINED_BEDROCK.value() && state.getBlock() == Blocks.BEDROCK) {
            Tool tool = itemStack.get(DataComponents.TOOL);
            if (tool == null) {
                return;
            }
            cir.setReturnValue(tool.getMiningSpeed(Blocks.STONE.defaultBlockState()));
        }
    }
}
