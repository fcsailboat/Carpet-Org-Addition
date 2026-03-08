package boat.carpetorgaddition.client.render;

import boat.carpetorgaddition.client.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;

import java.util.List;

public class Tooltip {
    private Tooltip() {
    }

    /**
     * 在屏幕中心偏右下的位置渲染一个提示框
     *
     * @param context 绘制上下文
     * @param list    提示框的内容，一个元素表示提示内的一行文本
     */
    public static void drawTooltip(GuiGraphics context, List<Component> list) {
        Minecraft client = ClientUtils.getClient();
        int height = client.getWindow().getGuiScaledHeight();
        int width = client.getWindow().getGuiScaledWidth();
        Font font = ClientUtils.getTextRenderer();
        List<ClientTooltipComponent> components = list.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).toList();
        context.renderTooltip(font, components, width / 2 + 7, height / 2 + 27, DefaultTooltipPositioner.INSTANCE, null);
    }

    public static void drawTooltip(GuiGraphics context, Component text) {
        drawTooltip(context, List.of(text));
    }
}
