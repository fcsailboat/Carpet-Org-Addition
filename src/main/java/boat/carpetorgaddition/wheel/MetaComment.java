package boat.carpetorgaddition.wheel;

import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

/**
 * 用来给一些功能添加注释
 */
public class MetaComment {
    @NonNull
    private String comment = "";

    public MetaComment() {
    }

    public MetaComment(@NonNull String comment) {
        this.comment = comment;
    }

    /**
     * @return 此注释是否有内容
     */
    public boolean hasContent() {
        return !this.isEmpty();
    }

    public boolean isEmpty() {
        return this.comment.isBlank();
    }

    public @NonNull String getComment() {
        if (this.isEmpty()) {
            return "";
        }
        return this.comment;
    }

    public Component getText() {
        return TextBuilder.create(this.comment);
    }

    @Override
    public String toString() {
        return this.comment;
    }
}
