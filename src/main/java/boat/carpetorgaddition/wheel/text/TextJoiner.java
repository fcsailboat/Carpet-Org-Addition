package boat.carpetorgaddition.wheel.text;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

@SuppressWarnings("UnusedReturnValue")
public class TextJoiner {
    private final ArrayList<Component> list = new ArrayList<>();
    /**
     * 当前缩进层级的深度
     */
    private int depth = 0;
    /**
     * 项目符号
     */
    private String bullet = "- ";
    private String indent = "  ";

    public TextJoiner() {
    }

    /**
     * 换行
     */
    public TextJoiner newline() {
        return this.newline(TextBuilder.empty());
    }

    /**
     * 追加文本并换行
     */
    public TextJoiner newline(Component text) {
        if (this.depth == 0) {
            this.list.add(text);
        } else {
            this.list.add(TextBuilder.combineAll(this.indent(), text));
        }
        return this;
    }

    public TextJoiner space() {
        return this.append(" ");
    }

    /**
     * 追加字符串，不换行
     */
    public TextJoiner append(String str) {
        return this.append(TextBuilder.create(str));
    }

    /**
     * 追加文本但不换行
     */
    public TextJoiner append(Component text) {
        if (this.list.isEmpty()) {
            this.newline(text);
        } else {
            Component last = this.list.removeLast();
            Component combined = TextBuilder.combineAll(last, text);
            this.list.addLast(combined);
        }
        return this;
    }

    public TextJoiner append(Number number) {
        return this.append(number.toString());
    }

    /**
     * 进入下一缩进层级并追加文本
     */
    public TextJoiner enter(Component text) {
        return this.enter(() -> this.newline(text));
    }

    /**
     * 进入下一缩进层级并执行方法
     */
    public TextJoiner enter(Runnable runnable) {
        this.depth++;
        runnable.run();
        this.depth--;
        return this;
    }

    private String indent() {
        if (this.depth == 0) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("", "", this.bullet);
        for (int i = 0; i < this.depth; i++) {
            joiner.add(this.indent);
        }
        return joiner.toString();
    }

    /**
     * 设置项目符号
     */
    @SuppressWarnings("unused")
    public void setBullet(char bullet) {
        this.bullet = bullet + " ";
    }

    /**
     * 取消项目符号
     */
    public void unsetBullet() {
        this.bullet = "";
    }

    public void setIndent(int indent) {
        if (indent < 0) {
            throw new IllegalArgumentException("Invalid indentation value: " + indent);
        }
        this.indent = switch (indent) {
            case 0 -> "";
            case 1 -> " ";
            case 2 -> "  ";
            case 3 -> "   ";
            case 4 -> "    ";
            default -> " ".repeat(indent);
        };
    }

    public Component join() {
        return TextBuilder.joinList(this.list);
    }

    public TextBuilder builder() {
        return TextBuilder.of(this.join());
    }

    public List<Component> collect() {
        return Collections.unmodifiableList(this.list);
    }
}
