package boat.carpetorgaddition.wheel.permission;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionSet;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class CommandPermission implements PermissionCheck {
    private static final MapCodec<CommandPermission> CODEC = Codec.STRING
            .fieldOf("level")
            .flatXmap(value -> {
                try {
                    PermissionLevel level = PermissionLevel.fromString(value);
                    return DataResult.success(new CommandPermission(level));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(e::getMessage);
                }
            }, permission -> DataResult.success(permission.level.asString()));

    private PermissionLevel level;

    /*package-private*/ CommandPermission(PermissionLevel level) {
        this.level = level;
    }

    public PermissionLevel getLevel() {
        return this.level;
    }

    public void setLevel(PermissionLevel level) {
        this.level = level;
    }

    @Override
    public boolean check(PermissionSet source) {
        return this.level.test(source);
    }

    @Override
    public MapCodec<? extends PermissionCheck> codec() {
        return CODEC;
    }
}
