package boat.carpetorgaddition.dialog.builder;

import boat.carpetorgaddition.dialog.DialogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogListDialog;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DialogListDialogBuilder extends DialogBuilder<DialogListDialogBuilder, DialogListDialog> {
    /**
     * 对话框列表
     */
    private final List<Holder<Dialog>> dialogs = new ArrayList<>();
    /**
     * 返回按钮
     */
    @Nullable
    private ActionButton exitAction;
    /**
     * 对话框每行显示的按钮个数
     */
    private int columns = 1;
    /**
     * 对话框跳转按钮的宽度
     */
    private int buttonWidth = 150;

    private DialogListDialogBuilder(Component title) {
        super(title);
    }

    public static DialogListDialogBuilder of(Component title) {
        return new DialogListDialogBuilder(title);
    }

    public DialogListDialogBuilder addDialog(Dialog dialog) {
        this.dialogs.add(Holder.direct(dialog));
        return this.self();
    }

    @SuppressWarnings("unused")
    public DialogListDialogBuilder setColumns(int columns) {
        this.columns = columns;
        return this.self();
    }

    @SuppressWarnings("unused")
    public DialogListDialogBuilder setButtonWidth(int buttonWidth) {
        this.buttonWidth = buttonWidth;
        return this.self();
    }

    public DialogListDialogBuilder setExitAction(@Nullable ActionButton exitAction) {
        this.exitAction = exitAction;
        return this;
    }

    /// 设定对话框的父级，添加返回按钮，用于在单击返回按钮后回到上一级对话框<br>
    /// 如果为`null`，则没有上一级，添加关闭按钮
    public DialogListDialogBuilder setParent(MinecraftServer server, @Nullable Identifier parent) {
        return this.setExitAction(DialogUtils.createBackButton(server, parent));
    }

    @Override
    protected DialogListDialogBuilder self() {
        return this;
    }

    @Override
    public DialogListDialog build() {
        CommonDialogData dialogData = this.createDialogData();
        HolderSet<Dialog> set = HolderSet.direct(this.dialogs);
        return new DialogListDialog(dialogData, set, Optional.ofNullable(this.exitAction), this.columns, this.buttonWidth);
    }
}
