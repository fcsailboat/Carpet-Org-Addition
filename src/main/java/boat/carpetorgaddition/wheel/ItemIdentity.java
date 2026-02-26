package boat.carpetorgaddition.wheel;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class ItemIdentity {
    private final Item item;
    private final DataComponentPatch components;
    @Nullable
    private ItemStack itemStack;

    public ItemIdentity(Item item, DataComponentPatch components) {
        this.item = item;
        this.components = components;
    }

    public ItemIdentity(ItemStack itemStack) {
        this(itemStack.getItem(), itemStack.getComponentsPatch());
        this.itemStack = itemStack;
    }

    @SuppressWarnings("unused")
    public ItemIdentity(ItemStackTemplate template) {
        this(template.item().value(), template.components());
    }

    @NonNull
    public ItemStack asItemStack() {
        if (this.itemStack == null) {
            this.itemStack = new ItemStackTemplate(this.item, this.getComponents()).create();
        }
        return this.itemStack;
    }

    public Item getItem() {
        return item;
    }

    public DataComponentPatch getComponents() {
        return components;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        ItemIdentity that = (ItemIdentity) obj;
        return Objects.equals(this.item, that.item) && Objects.equals(this.components, that.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, components);
    }
}
