package boat.carpetorgaddition.wheel.inventory;

import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum PlayerInventoryType {
    INVENTORY(LocalizationKeys.Misc.INVENTORY.translate()),
    ENDER_CHEST(LocalizationKeys.Misc.ENDER_CHEST.translate());
    private final Component displayName;
    public static final Codec<PlayerInventoryType> CODEC = Codec.STRING
            .comapFlatMap(PlayerInventoryType::parse, PlayerInventoryType::toString)
            .stable();

    PlayerInventoryType(Component displayName) {
        this.displayName = displayName;
    }

    private static DataResult<PlayerInventoryType> parse(String type) {
        try {
            PlayerInventoryType value = PlayerInventoryType.valueOf(type.toUpperCase(Locale.ROOT));
            return DataResult.success(value);
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> CommandUtils.getExceptionString(e));
        }
    }

    public Component getDisplayName() {
        return this.displayName;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
