package boat.carpetorgaddition.dialog.builder;

import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @see <a href="https://zh.minecraft.wiki/w/%E5%AF%B9%E8%AF%9D%E6%A1%86%E5%AE%9A%E4%B9%89%E6%A0%BC%E5%BC%8F">对话框定义格式</a>
 */
@SuppressWarnings("unused")
public abstract class DialogBuilder<C extends DialogBuilder<C, D>, D extends Dialog> {
    /**
     * 对话框的标题
     */
    private final Component title;
    /**
     * 在暂停屏幕或其他对话框内用于打开此对话框按钮的文本
     */
    @Nullable
    private Component externalTitle = null;
    /**
     * 是否可以通过{@code Ecs}键关闭对话框
     */
    private boolean canCloseWithEscape = true;
    /**
     * 打开对话框是是否暂停单人游戏
     */
    private boolean pause = false;
    /**
     * 对话框的操作后行为
     */
    private DialogAction afterAction = DialogAction.CLOSE;
    /**
     * 对话框的主体元素
     */
    private final List<DialogBody> bodies = new ArrayList<>();
    /**
     * 对话框的输入空间
     */
    private final List<Input> inputs = new ArrayList<>();

    protected DialogBuilder(Component title) {
        this.title = title;
    }

    public C setExternalTitle(Component externalTitle) {
        this.externalTitle = externalTitle;
        return this.self();
    }

    public C setCanCloseWithEscape(boolean canCloseWithEscape) {
        this.canCloseWithEscape = canCloseWithEscape;
        return this.self();
    }

    public C setPause(boolean pause) {
        this.pause = pause;
        return this.self();
    }

    public C setAfterAction(DialogAction afterAction) {
        this.afterAction = afterAction;
        return this.self();
    }

    public C addDialogBody(DialogBody body) {
        this.bodies.add(body);
        return this.self();
    }

    public C addDialogBody(Component component) {
        PlainMessage message = new PlainMessage(component, PlainMessage.DEFAULT_WIDTH);
        return this.addDialogBody(message);
    }

    public C addDialogBody(Component component, int width) {
        PlainMessage message = new PlainMessage(component, width);
        return this.addDialogBody(message);
    }

    public C addDialogBody(CommandSyntaxException exception) {
        return this.addDialogBody(
                TextBuilder.of(exception.getRawMessage())
                        .setColor(ChatFormatting.RED)
                        .build()
        );
    }

    public C addInput(Input input) {
        this.inputs.add(input);
        return this.self();
    }

    /**
     * @return 获取泛型，直接使用{@code this}会导致无法推导泛型
     */
    protected abstract C self();

    protected CommonDialogData createDialogData() {
        return new CommonDialogData(
                this.title,
                Optional.ofNullable(this.externalTitle),
                this.canCloseWithEscape,
                this.pause,
                this.afterAction,
                this.bodies,
                this.inputs
        );
    }

    public abstract D build();
}
