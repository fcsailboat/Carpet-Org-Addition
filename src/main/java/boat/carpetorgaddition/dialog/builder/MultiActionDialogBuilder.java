package boat.carpetorgaddition.dialog.builder;

import boat.carpetorgaddition.dialog.DialogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.MultiActionDialog;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class MultiActionDialogBuilder extends DialogBuilder<MultiActionDialogBuilder, MultiActionDialog> {
    /**
     * 按钮列表
     */
    private final List<ActionButton> actions = new ArrayList<>();
    /**
     * 对话框的返回按钮
     */
    @Nullable
    private ActionButton exitAction = null;
    /**
     * 每一行显示的按钮数量
     */
    private int columns = 1;

    private MultiActionDialogBuilder(Component title) {
        super(title);
    }

    public static MultiActionDialogBuilder of(Component title) {
        return new MultiActionDialogBuilder(title);
    }

    public MultiActionDialogBuilder addActionButton(ActionButton button) {
        this.actions.add(button);
        return this;
    }

    public MultiActionDialogBuilder setExitAction(@Nullable ActionButton exitAction) {
        this.exitAction = exitAction;
        return this;
    }

    /**
     * @see DialogListDialogBuilder#setParent(MinecraftServer, Identifier)
     */
    public MultiActionDialogBuilder setParent(MinecraftServer server, Identifier parent) {
        return this.setExitAction(DialogUtils.createBackButton(server, parent));
    }

    public MultiActionDialogBuilder setColumns(int columns) {
        this.columns = columns;
        return this;
    }

    @Override
    protected MultiActionDialogBuilder self() {
        return this;
    }

    @Override
    public MultiActionDialog build() {
        if (this.actions.isEmpty()) {
            throw new IllegalStateException("At least one action must be added before building Multi Action Dialog");
        }
        CommonDialogData common = this.createDialogData();
        return new MultiActionDialog(common, this.actions, Optional.ofNullable(this.exitAction), this.columns);
    }
}
