package boat.carpetorgaddition.dialog.builder;

import boat.carpetorgaddition.dialog.DialogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.input.SingleOptionInput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SingleOptionInputBuilder {
    /**
     * 从Nbt复合标签中读取数据时的键
     */
    private final String key;
    /**
     * 按钮的宽度
     */
    private int width = 200;
    /**
     * 选项列表
     */
    private final List<SingleOptionInput.Entry> options = new ArrayList<>();
    /**
     * 按钮的标签文本
     */
    private Component label = DialogUtils.UNDEFINED;
    /**
     * 是否渲染标签文本<br>
     * 当标签文本为{@code 玩家}，选项文本为{@code Steve}时<br>
     * 如果为{@code true}，则按钮文本将被渲染为{@code 玩家：Steve}，否则按钮文本将被渲染为{@code Steve}
     */
    private boolean labelVisible = true;

    private SingleOptionInputBuilder(String key) {
        this.key = DialogUtils.toValidDialogKey(key);
    }

    public static SingleOptionInputBuilder of(String key) {
        return new SingleOptionInputBuilder(key);
    }

    @SuppressWarnings("unused")
    public SingleOptionInputBuilder setWidth(int width) {
        this.width = width;
        return this;
    }

    @SuppressWarnings("unused")
    public SingleOptionInputBuilder setLabel(Component label) {
        this.label = label;
        return this;
    }

    public SingleOptionInputBuilder setLabelVisible(boolean labelVisible) {
        this.labelVisible = labelVisible;
        return this;
    }

    @SuppressWarnings("unused")
    public SingleOptionInputBuilder addEntry(String id) {
        return this.addEntry(id, null);
    }

    public SingleOptionInputBuilder addEntry(String id, @Nullable Component display) {
        return this.addEntry(id, display, this.options.isEmpty());
    }

    public SingleOptionInputBuilder addEntry(String id, @Nullable Component display, boolean initial) {
        this.options.add(new SingleOptionInput.Entry(id, Optional.ofNullable(display), initial));
        return this;
    }

    public Input build() {
        return new Input(this.key, new SingleOptionInput(this.width, this.options, this.label, this.labelVisible));
    }
}
