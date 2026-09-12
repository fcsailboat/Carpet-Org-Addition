package boat.carpetorgaddition.util;

import boat.carpetorgaddition.wheel.inventory.ContainerComponentInventory;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;

import java.util.List;

@Disabled
public class InventoryUtilsTest {
    @BeforeAll
    public static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @RepeatedTest(10)
    public void testAddItemToShulkerBox() {
        ItemStack itemStack = new ItemStack(Items.SHULKER_BOX);
        ItemContainerContents component = itemStack.get(DataComponents.CONTAINER);
        Assertions.assertNotNull(component);
        List<ItemStack> list = component.nonEmptyItemCopyStream().toList();
        Assertions.assertTrue(list.isEmpty());
        ContainerComponentInventory inventory = new ContainerComponentInventory(itemStack);
        int count = 0;
        while (true) {
            Item item = MathUtils.getRandomElement(BuiltInRegistries.ITEM.stream().toList());
            ItemStack stack = new ItemStack(item);
            int i = MathUtils.randomInt(1, stack.getMaxStackSize());
            stack.setCount(i);
            if (stack.isEmpty()) {
                continue;
            }
            count += i;
            System.out.println(stack);
            ItemStack remaining = inventory.addItem(stack);
            if (remaining.isEmpty()) {
                continue;
            }
            count -= remaining.getCount();
            break;
        }
        ItemContainerContents newComponent = itemStack.get(DataComponents.CONTAINER);
        System.out.println(inventory);
        Assertions.assertNotNull(newComponent);
        Assertions.assertEquals(27, newComponent.nonEmptyItemCopyStream().toList().size());
        Assertions.assertEquals(count, inventory.count(_ -> true));
    }
}
