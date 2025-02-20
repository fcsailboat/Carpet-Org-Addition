package org.carpetorgaddition.mixin.rule;

import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ContainerComponent.class)
public interface ContainerComponentAccessor {
    @Invoker(value = "<init>", remap = false)
    static ContainerComponent constructor(DefaultedList<ItemStack> stacks) {
        throw new AssertionError();
    }
}
