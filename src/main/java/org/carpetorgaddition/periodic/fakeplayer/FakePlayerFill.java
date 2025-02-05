package org.carpetorgaddition.periodic.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import org.carpetorgaddition.periodic.fakeplayer.actioncontext.FillContext;

import java.util.stream.IntStream;

public class FakePlayerFill {
    private FakePlayerFill() {
    }

    public static void fill(FillContext context, EntityPlayerMPFake fakePlayer) {
        ScreenHandler screenHandler = fakePlayer.currentScreenHandler;
        if (screenHandler == null || screenHandler instanceof PlayerScreenHandler) {
            return;
        }
        boolean allItem = context.isAllItem();
        // 获取要装在潜影盒的物品
        Item item = allItem ? null : context.getItem();
        for (int index : getRange(screenHandler)) {
            Slot slot = screenHandler.getSlot(index);
            ItemStack itemStack = slot.getStack();
            if (itemStack.isEmpty()) {
                continue;
            }
            if ((allItem || itemStack.isOf(item))) {
                if (screenHandler instanceof ShulkerBoxScreenHandler && !itemStack.getItem().canBeNested()) {
                    continue;
                }
                // 模拟按住Shift键移动物品
                if (FakePlayerUtils.quickMove(screenHandler, index, fakePlayer).isEmpty()) {
                    fakePlayer.onHandledScreenClosed();
                    return;
                }
            }
        }
    }

    private static Integer[] getRange(ScreenHandler screenHandler) {
        IntStream intStream = switch (screenHandler) {
            case ShulkerBoxScreenHandler ignored -> IntStream.rangeClosed(27, 62);
            // 箱子，末影箱，木桶等容器
            case GenericContainerScreenHandler handler
                    when handler.getType() == ScreenHandlerType.GENERIC_9X3 -> IntStream.rangeClosed(27, 62);
            // 大箱子，GCA假人背包
            case GenericContainerScreenHandler handler
                    when handler.getType() == ScreenHandlerType.GENERIC_9X6 -> IntStream.rangeClosed(54, 89);
            // 漏斗
            case HopperScreenHandler ignored -> IntStream.rangeClosed(5, 40);
            // 发射器，投掷器
            case Generic3x3ContainerScreenHandler ignored -> IntStream.rangeClosed(9, 44);
            // 合成器
            case CrafterScreenHandler ignored -> IntStream.rangeClosed(9, 44);
            case null, default -> IntStream.of();
        };
        return intStream.boxed().toArray(Integer[]::new);
    }
}
