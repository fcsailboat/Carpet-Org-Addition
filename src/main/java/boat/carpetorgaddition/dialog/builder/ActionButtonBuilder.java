package boat.carpetorgaddition.dialog.builder;

import boat.carpetorgaddition.network.event.ActionSource;
import boat.carpetorgaddition.network.event.CustomClickAction;
import boat.carpetorgaddition.wheel.nbt.NbtWriter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.action.CustomAll;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("unused")
public class ActionButtonBuilder {
    /**
     * 按钮的渲染文本
     */
    private final Component label;
    /**
     * 按钮的工具提示
     */
    @Nullable
    private Component tooltip = null;
    /**
     * 按钮的宽度
     */
    private int width = CommonButtonData.DEFAULT_WIDTH;
    /**
     * 单击按钮后执行的操作
     */
    @Nullable
    private Action action = null;

    private ActionButtonBuilder(Component label) {
        this.label = label;
    }

    public static ActionButtonBuilder of(Component label) {
        return new ActionButtonBuilder(label);
    }

    public ActionButtonBuilder setTooltip(@Nullable Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public ActionButtonBuilder setWidth(int width) {
        this.width = width;
        return this;
    }

    public ActionButtonBuilder setAction(@Nullable Action action) {
        this.action = action;
        return this;
    }

    public ActionButtonBuilder setCustomClickAction(MinecraftServer server, Identifier id) {
        NbtWriter writer = new NbtWriter(server, CustomClickAction.CURRENT_VERSION);
        writer.putActionSource(ActionSource.DIALOG);
        this.action = new CustomAll(id, Optional.of(writer.toNbt()));
        return this;
    }

    public ActionButton build() {
        CommonButtonData data = new CommonButtonData(this.label, Optional.ofNullable(this.tooltip), this.width);
        return new ActionButton(data, Optional.ofNullable(this.action));
    }
}
