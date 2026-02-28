package boat.carpetorgaddition.util;

import boat.carpetorgaddition.CarpetOrgAdditionMetadata;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class IdentifierUtils {
    private IdentifierUtils() {
    }

    public static Identifier getId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public static Identifier getId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    @SuppressWarnings("unused")
    public static Optional<Identifier> getId(Level world, Enchantment enchantment) {
        Holder<Enchantment> entry = Holder.direct(enchantment);
        entry.unwrapKey().map(ResourceKey::identifier);
        return getId(world.registryAccess(), enchantment);
    }

    public static Optional<Identifier> getId(MinecraftServer server, Enchantment enchantment) {
        return getId(server.registryAccess(), enchantment);
    }

    public static Optional<Identifier> getId(RegistryAccess registryManager, Enchantment enchantment) {
        Optional<Registry<Enchantment>> optional = registryManager.lookup(Registries.ENCHANTMENT);
        if (optional.isEmpty()) {
            return Optional.empty();
        }
        Registry<Enchantment> enchantments = optional.get();
        return Optional.ofNullable(enchantments.getKey(enchantment));
    }


    public static String getIdAsString(Item item) {
        return getId(item).toString();
    }


    public static String getIdAsString(Block block) {
        return getId(block).toString();
    }

    /**
     * 将字符串ID转换为物品
     */
    public static Item getItem(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    /**
     * 将字符串ID转换为方块
     */
    public static Block getBlock(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
    }

    @SuppressWarnings("unused")
    public static Optional<UUID> uuidFromString(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(str));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Identifier ofIdentifier(String id) {
        return Identifier.fromNamespaceAndPath(CarpetOrgAdditionMetadata.MOD_ID, id);
    }
}
