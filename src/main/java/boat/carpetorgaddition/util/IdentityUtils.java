package boat.carpetorgaddition.util;

import boat.carpetorgaddition.wheel.text.LocalizationKey;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gamerules.GameRule;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class IdentityUtils {
    public static Component getName(Item item) {
        return item.components().getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
    }

    public static Component getName(Block block) {
        return block.getName();
    }

    public static Component getName(EntityType<?> entityType) {
        return entityType.getDescription();
    }

    public static Component getName(Holder<Enchantment> enchantment) {
        return EnchantmentUtils.getName(enchantment);
    }

    public static Component getName(MobEffect statusEffect) {
        return statusEffect.getDisplayName();
    }

    public static Component getName(RegistryAccess registryAccess, Biome biome) {
        Registry<Biome> biomes = registryAccess.lookupOrThrow(Registries.BIOME);
        String key = Objects.requireNonNull(biomes.getKey(biome)).toLanguageKey("biome");
        return LocalizationKey.literal(key).translate();
    }

    public static Component getName(GameType gameType) {
        return gameType.getLongDisplayName();
    }

    public static Component getName(GameRule<?> gameRule) {
        String key = gameRule.getDescriptionId();
        return LocalizationKey.literal(key).translate();
    }

    public static Component getDefaultName(ItemStack itemStack) {
        return getName(itemStack.getItem());
    }

    public static String getNameAsString(Item item) {
        return getName(item).getString();
    }

    public static String getNameAsString(Block block) {
        return getName(block).getString();
    }

    public static String getNameAsString(EntityType<?> entityType) {
        return getName(entityType).getString();
    }

    public static String getNameAsString(Holder<Enchantment> enchantment) {
        return getName(enchantment).getString();
    }

    public static String getNameAsString(MobEffect statusEffect) {
        return getName(statusEffect).getString();
    }

    public static String getNameAsString(GameType gameType) {
        return gameType.getLongDisplayName().getString();
    }

    public static String getNameAsString(GameRule<?> gameRule) {
        return getName(gameRule).getString();
    }

    public static String getNameAsString(RegistryAccess registryAccess, Biome biome) {
        return getName(registryAccess, biome).getString();
    }

    public static Identifier getId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public static Identifier getId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    public static Identifier getId(Level world) {
        return world.dimension().identifier();
    }

    public static Identifier getId(EntityType<?> entityType) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
    }

    @SuppressWarnings("unused")
    public static Optional<Identifier> getId(Level world, Holder<Enchantment> enchantment) {
        return getId(world.registryAccess(), enchantment);
    }

    public static Optional<Identifier> getId(MinecraftServer server, Holder<Enchantment> enchantment) {
        return getId(server.registryAccess(), enchantment);
    }

    public static Optional<Identifier> getId(RegistryAccess registryAccess, Holder<Enchantment> enchantment) {
        return registryAccess.lookup(Registries.ENCHANTMENT).map(registry -> registry.getKey(enchantment.value()));
    }

    public static Optional<Identifier> getId(RegistryAccess registryAccess, MobEffect statusEffect) {
        return registryAccess.lookup(Registries.MOB_EFFECT).map(registry -> registry.getKey(statusEffect));
    }

    public static Optional<Identifier> getId(RegistryAccess registryAccess, Biome biome) {
        return registryAccess.lookup(Registries.BIOME).map(registry -> registry.getKey(biome));
    }

    public static Optional<Identifier> getId(RegistryAccess registryAccess, GameRule<?> gameRule) {
        return registryAccess.lookup(Registries.GAME_RULE).map(registry -> registry.getKey(gameRule));
    }

    public static <T> Identifier getId(Holder.Reference<T> holder) {
        return holder.key().identifier();
    }

    public static String getIdAsString(Item item) {
        return getId(item).toString();
    }

    public static String getIdAsString(Block block) {
        return getId(block).toString();
    }

    /**
     * 获取当前维度的ID
     *
     * @param world 当前世界的对象
     * @return 当前维度的ID
     */
    public static String getIdAsString(Level world) {
        return getId(world).toString();
    }

    public static String getIdAsString(EntityType<?> entityType) {
        return getId(entityType).toString();
    }

    public static String getIdAsString(GameType gameType) {
        return gameType.getSerializedName();
    }

    public static String getIdAsString(RegistryAccess registryAccess, Holder<Enchantment> enchantment) {
        return getId(registryAccess, enchantment).map(Identifier::toString).orElse(ServerUtils.UNREGISTERED);
    }

    public static String getIdAsString(RegistryAccess registryAccess, MobEffect statusEffect) {
        return getId(registryAccess, statusEffect).map(Identifier::toString).orElse(ServerUtils.UNREGISTERED);
    }

    public static String getIdAsString(RegistryAccess registryAccess, Biome biome) {
        return getId(registryAccess, biome).map(Identifier::toString).orElse(ServerUtils.UNREGISTERED);
    }

    public static String getIdAsString(RegistryAccess registryAccess, GameRule<?> gameRule) {
        return getId(registryAccess, gameRule).map(Identifier::toString).orElse(ServerUtils.UNREGISTERED);
    }

    public static <T> String getIdAsString(Holder.Reference<T> holder) {
        return getId(holder).toString();
    }

    /**
     * 将字符串ID转换为物品
     */
    public static Item asItem(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    /**
     * 将字符串ID转换为方块
     */
    public static Block asBlock(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
    }

    public static Holder<Enchantment> asEnchantment(RegistryAccess registryAccess, Identifier id) {
        return registryAccess.lookupOrThrow(Registries.ENCHANTMENT).get(id).orElseThrow();
    }
}
