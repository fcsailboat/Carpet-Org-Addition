package org.carpetorgaddition.client.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.biome.Biome;
import org.carpetorgaddition.client.util.ClientCommandUtils;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.EnchantmentUtils;
import org.carpetorgaddition.util.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public abstract class ClientObjectArgumentType<T> implements ArgumentType<List<T>> {
    private ClientObjectArgumentType() {
    }

    public static List<?> getType(CommandContext<FabricClientCommandSource> context, String name) {
        return context.getArgument(name, List.class);
    }

    @Override
    public List<T> parse(StringReader reader) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        String itemName = ClientCommandUtils.readWord(reader);
        // 由于可以使用资源包更改对象名称，因此一个名称可能对应多个对象
        ArrayList<T> list = new ArrayList<>();
        for (T t : getRegistry().toList()) {
            // 获取所有与字符串对应的对象
            if (Objects.equals(itemName, objectToString(t))) {
                list.add(t);
            }
        }
        // 没有对象与字符串对应
        if (list.isEmpty()) {
            reader.setCursor(cursor);
            throw CommandUtils.createException("carpet.client.commands.dictionary.not_matched");
        }
        return list;
    }

    protected abstract String objectToString(T t);

    /**
     * 列出命令建议
     */
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (context.getSource() instanceof CommandSource) {
            String[] array = getRegistry()
                    .map(this::objectToString)
                    .map(s -> s.contains(" ") ? "\"" + s + "\"" : s)
                    .toArray(String[]::new);
            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
            for (String candidate : array) {
                // 列出所有名称中包含输入字符串的对象
                if (candidate.toLowerCase(Locale.ROOT).contains(remaining)) {
                    builder.suggest(candidate);
                }
            }
            return builder.buildFuture();
        } else {
            return Suggestions.empty();
        }
    }

    /**
     * 获取对象对应的注册表
     */
    protected abstract Stream<T> getRegistry();

    /**
     * 物品参数
     */
    public static class ClientItemArgumentType extends ClientObjectArgumentType<Item> {

        @Override
        protected String objectToString(Item item) {
            return item.getName().getString();
        }

        @Override
        protected Stream<Item> getRegistry() {
            return Registries.ITEM.stream();
        }
    }

    /**
     * 方块参数
     */
    public static class ClientBlockArgumentType extends ClientObjectArgumentType<Block> {

        @Override
        protected String objectToString(Block block) {
            return block.getName().getString();
        }

        @Override
        protected Stream<Block> getRegistry() {
            return Registries.BLOCK.stream();
        }
    }

    /**
     * 实体参数
     */
    public static class ClientEntityArgumentType extends ClientObjectArgumentType<EntityType<?>> {

        @Override
        protected String objectToString(EntityType<?> entityType) {
            return entityType.getName().getString();
        }

        @Override
        protected Stream<EntityType<?>> getRegistry() {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                return player.networkHandler.getRegistryManager().get(RegistryKeys.ENTITY_TYPE).stream();
            }
            return Stream.empty();
        }
    }

    /**
     * 附魔参数
     */
    public static class ClientEnchantmentArgumentType extends ClientObjectArgumentType<Enchantment> {

        @Override
        protected String objectToString(Enchantment enchantment) {
            return EnchantmentUtils.getName(enchantment).getString();
        }

        @Override
        protected Stream<Enchantment> getRegistry() {
            if (MinecraftClient.getInstance().player != null) {
                Registry<Enchantment> registry = MinecraftClient.getInstance().player.networkHandler.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
                return registry.stream();
            }
            return Stream.empty();
        }
    }

    /**
     * 状态效果参数
     */
    public static class ClientStatusEffectArgumentType extends ClientObjectArgumentType<StatusEffect> {

        @Override
        protected String objectToString(StatusEffect statusEffect) {
            return statusEffect.getName().getString();
        }

        @Override
        protected Stream<StatusEffect> getRegistry() {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                return player.networkHandler.getRegistryManager().get(RegistryKeys.STATUS_EFFECT).stream();
            }
            return Stream.empty();
        }
    }

    public static class ClientBiomeArgumentType extends ClientObjectArgumentType<Biome> {

        @Override
        protected String objectToString(Biome biome) {
            assert MinecraftClient.getInstance().player != null;
            Registry<Biome> biomes = MinecraftClient.getInstance().player.networkHandler.getRegistryManager().get(RegistryKeys.BIOME);
            return TextUtils.translate(Objects.requireNonNull(biomes.getId(biome)).toTranslationKey("biome")).getString();

        }

        @Override
        protected Stream<Biome> getRegistry() {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) {
                return Stream.empty();
            }
            return player.networkHandler.getRegistryManager().get(RegistryKeys.BIOME).stream();
        }
    }
}
