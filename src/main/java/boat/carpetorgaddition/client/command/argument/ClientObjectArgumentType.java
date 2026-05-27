package boat.carpetorgaddition.client.command.argument;

import boat.carpetorgaddition.client.util.ClientCommandUtils;
import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gamerules.GameRule;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ClientObjectArgumentType<T> implements ArgumentType<List<T>> {
    private static final List<String> PATTERNS = Arrays.stream(MatchPattern.values()).map(MatchPattern::toString).toList();
    /**
     * 是否允许通过id补全名称<br>
     * 启用后，可以通过输入部分或全部对象id来补全对象名称<br>
     * 例如：输入apple，则补全候选中会出现苹果、金苹果和附魔金苹果。<br>
     * 但是，该命令参数本身就是为了通过对象名称查询对象id，允许反向补全可能没有实际意义。
     */
    private static final boolean ID_COMPLETION_NAME = false;
    /**
     * 字符串是否使用匹配模式
     */
    private final boolean patternMatching;

    private ClientObjectArgumentType() {
        this(false);
    }

    private ClientObjectArgumentType(boolean patternMatching) {
        this.patternMatching = patternMatching;
    }

    public static List<?> getType(CommandContext<FabricClientCommandSource> context, String name) {
        return context.getArgument(name, List.class);
    }

    @Override
    public List<T> parse(StringReader reader) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        String name = ClientCommandUtils.readWord(reader);
        MatchPattern pattern = this.patternMatching ? readParameters(reader) : MatchPattern.EQUAL;
        // 由于可以使用资源包更改对象名称，因此一个名称可能对应多个对象
        ArrayList<T> list = new ArrayList<>();
        if (!name.isEmpty()) {
            for (T t : stream().toList()) {
                // 获取所有与字符串对应的对象
                if (pattern.match(name.toLowerCase(Locale.ROOT), objectToString(t).toLowerCase(Locale.ROOT))) {
                    list.add(t);
                }
            }
        }
        // 没有对象与字符串对应
        if (list.isEmpty()) {
            reader.setCursor(cursor);
            throw CommandUtils.createException(LocalizationKeys.Argument.Object.MISMATCH.translate());
        }
        // 字符串过于宽泛
        if (this.patternMatching && list.size() > 40 && pattern != MatchPattern.EQUAL) {
            reader.setCursor(cursor);
            throw CommandUtils.createException(LocalizationKeys.Argument.Object.BROAD.translate());
        }
        return list;
    }

    private MatchPattern readParameters(StringReader reader) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        reader.skipWhitespace();
        String argument = reader.readUnquotedString().toLowerCase(Locale.ROOT);
        if (argument.startsWith("-")) {
            MatchPattern pattern = MatchPattern.MATCH_PATTERNS.get(argument);
            if (pattern == null) {
                throw CommandUtils.createException(LocalizationKeys.Argument.Object.INVALID_PATTERN.translate());
            }
            return pattern;
        } else {
            reader.setCursor(cursor);
            return MatchPattern.EQUAL;
        }
    }

    /**
     * @return 对象的翻译后名称
     */
    protected abstract String objectToString(T t);

    protected abstract Map.Entry<String, String> entry(T t);

    /**
     * 列出命令建议
     */
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (context.getSource() instanceof SharedSuggestionProvider) {
            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
            String[] split = this.splitArguments(remaining);
            if (this.patternMatching && (split.length > 1 || remaining.endsWith(" "))) {
                // 补全匹配模式
                StringReader reader = new StringReader(builder.getInput());
                // 跳过名称字符串和空格
                if (split.length > 0) {
                    reader.setCursor(builder.getStart() + split[0].length());
                }
                reader.skipWhitespace();
                SuggestionsBuilder offset = builder.createOffset(reader.getCursor());
                for (String pattern : PATTERNS) {
                    if (!reader.canRead() || (split.length > 1 && pattern.startsWith(split[1].toLowerCase(Locale.ROOT)))) {
                        offset.suggest(pattern);
                    }
                }
                return offset.buildFuture();
            } else {
                // 列出所有名称中包含输入字符串的对象
                this.stream()
                        .map(this::entry)
                        .distinct()
                        .map(entry -> Map.entry(quoteIfContainsSpace(entry.getKey()), entry.getValue()))
                        .forEach(entry -> {
                            String key = entry.getKey();
                            if (key.toLowerCase(Locale.ROOT).contains(remaining) || (ID_COMPLETION_NAME && entry.getValue().contains(remaining))) {
                                builder.suggest(key);
                            }
                        });
                return builder.buildFuture();
            }
        }
        return Suggestions.empty();
    }

    private static String quoteIfContainsSpace(String str) {
        return str.contains(" ") ? "\"" + str + "\"" : str;
    }

    /**
     * 将参数切割为数组，允许被引号包裹的字符串中出现空格
     */
    private String[] splitArguments(String remaining) {
        ArrayList<String> list = new ArrayList<>();
        StringReader reader = new StringReader(remaining);
        while (reader.canRead()) {
            int cursor = reader.getCursor();
            try {
                ClientCommandUtils.readWord(reader);
                list.add(remaining.substring(cursor, reader.getCursor()));
                reader.skipWhitespace();
            } catch (CommandSyntaxException e) {
                break;
            }
        }
        return list.toArray(String[]::new);
    }

    /**
     * 获取对象对应的注册表
     */
    protected abstract Stream<T> stream();

    /**
     * 物品参数
     */
    public static class ClientItemArgumentType extends ClientObjectArgumentType<Item> {
        public ClientItemArgumentType(boolean patternMatching) {
            super(patternMatching);
        }

        @Override
        protected String objectToString(Item item) {
            return ServerUtils.getName(item).getString();
        }

        @Override
        protected Map.Entry<String, String> entry(Item item) {
            return Map.entry(ServerUtils.getNameAsString(item), ServerUtils.getIdAsString(item));
        }

        @Override
        protected Stream<Item> stream() {
            return BuiltInRegistries.ITEM.stream();
        }
    }

    /**
     * 方块参数
     */
    public static class ClientBlockArgumentType extends ClientObjectArgumentType<Block> {
        public ClientBlockArgumentType(boolean patternMatching) {
            super(patternMatching);
        }

        @Override
        protected String objectToString(Block block) {
            return block.getName().getString();
        }

        @Override
        protected Map.Entry<String, String> entry(Block block) {
            return Map.entry(ServerUtils.getNameAsString(block), ServerUtils.getIdAsString(block));
        }

        @Override
        protected Stream<Block> stream() {
            return BuiltInRegistries.BLOCK.stream();
        }
    }

    /**
     * 实体参数
     */
    public static class ClientEntityArgumentType extends ClientObjectArgumentType<EntityType<?>> {
        @Override
        protected String objectToString(EntityType<?> entityType) {
            return entityType.getDescription().getString();
        }

        @Override
        protected Map.Entry<String, String> entry(EntityType<?> entityType) {
            return Map.entry(ServerUtils.getNameAsString(entityType), ServerUtils.getIdAsString(entityType));
        }

        @Override
        protected Stream<EntityType<?>> stream() {
            return ClientUtils.getRegistryAccess().lookupOrThrow(Registries.ENTITY_TYPE).stream();
        }
    }

    /**
     * 魔咒参数
     */
    public static class ClientEnchantmentArgumentType extends ClientObjectArgumentType<Enchantment> {
        @Override
        protected String objectToString(Enchantment enchantment) {
            return EnchantmentUtils.getName(enchantment).getString();
        }

        @Override
        protected Map.Entry<String, String> entry(Enchantment enchantment) {
            RegistryAccess registryAccess = ClientUtils.getRegistryAccess();
            return Map.entry(ServerUtils.getNameAsString(enchantment), ServerUtils.getIdAsString(registryAccess, enchantment));
        }

        @Override
        protected Stream<Enchantment> stream() {
            Registry<Enchantment> registry = ClientUtils.getRegistryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            return registry.stream();
        }
    }

    /**
     * 状态效果参数
     */
    public static class ClientStatusEffectArgumentType extends ClientObjectArgumentType<MobEffect> {
        @Override
        protected String objectToString(MobEffect statusEffect) {
            return statusEffect.getDisplayName().getString();
        }

        @Override
        protected Map.Entry<String, String> entry(MobEffect statusEffect) {
            RegistryAccess registryAccess = ClientUtils.getRegistryAccess();
            return Map.entry(ServerUtils.getNameAsString(statusEffect), ServerUtils.getIdAsString(registryAccess, statusEffect));
        }

        @Override
        protected Stream<MobEffect> stream() {
            return ClientUtils.getRegistryAccess().lookupOrThrow(Registries.MOB_EFFECT).stream();
        }
    }

    public static class ClientBiomeArgumentType extends ClientObjectArgumentType<Biome> {
        @Override
        protected String objectToString(Biome biome) {
            RegistryAccess registryAccess = ClientUtils.getRegistryAccess();
            Registry<Biome> biomes = registryAccess.lookupOrThrow(Registries.BIOME);
            String key = Objects.requireNonNull(biomes.getKey(biome)).toLanguageKey("biome");
            return LocalizationKey.literal(key).translate().getString();
        }

        @Override
        protected Map.Entry<String, String> entry(Biome biome) {
            RegistryAccess registryAccess = ClientUtils.getRegistryAccess();
            return Map.entry(ServerUtils.getNameAsString(registryAccess, biome), ServerUtils.getIdAsString(registryAccess, biome));
        }

        @Override
        protected Stream<Biome> stream() {
            return ClientUtils.getRegistryAccess().lookupOrThrow(Registries.BIOME).stream();
        }
    }

    /**
     * 游戏模式参数
     */
    public static class ClientGameModeArgumentType extends ClientObjectArgumentType<GameType> {
        @Override
        protected String objectToString(GameType gameMode) {
            return gameMode.getLongDisplayName().getString();
        }

        @Override
        protected Map.Entry<String, String> entry(GameType gameType) {
            return Map.entry(ServerUtils.getNameAsString(gameType), ServerUtils.getIdAsString(gameType));
        }

        @Override
        protected Stream<GameType> stream() {
            return Stream.of(GameType.values());
        }
    }

    /**
     * 游戏规则参数
     */
    public static class ClientGameRuleArgumentType extends ClientObjectArgumentType<GameRule<?>> {
        @Override
        protected String objectToString(GameRule<?> gameRule) {
            String key = gameRule.getDescriptionId();
            return LocalizationKey.literal(key).translate().getString();
        }

        @Override
        protected Map.Entry<String, String> entry(GameRule<?> gameRule) {
            RegistryAccess registryAccess = ClientUtils.getRegistryAccess();
            return Map.entry(ServerUtils.getNameAsString(gameRule), ServerUtils.getIdAsString(registryAccess, gameRule));
        }

        @Override
        protected Stream<GameRule<?>> stream() {
            Optional<Registry<GameRule<?>>> optional = ClientUtils.getRegistryAccess().lookup(Registries.GAME_RULE);
            if (optional.isEmpty()) {
                return Stream.empty();
            }
            Registry<GameRule<?>> gameRules = optional.get();
            return gameRules.stream();
        }
    }

    public enum MatchPattern {
        /**
         * 完全匹配
         */
        EQUAL,
        /**
         * 包含
         */
        CONTAIN,
        /**
         * 匹配开头
         */
        START,
        /**
         * 匹配结尾
         */
        END,
        /**
         * 正则表达式
         */
        REGEX;

        private static final Map<String, MatchPattern> MATCH_PATTERNS = Arrays.stream(MatchPattern.values())
                .map(pattern -> Map.entry(pattern.toString(), pattern))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        private boolean match(String argument, String name) {
            return switch (this) {
                case EQUAL -> Objects.equals(name, argument);
                case CONTAIN -> name.contains(argument);
                case START -> name.startsWith(argument);
                case END -> name.endsWith(argument);
                case REGEX -> name.matches(argument);
            };
        }

        @Override
        public String toString() {
            return switch (this) {
                case EQUAL -> "-equal";
                case CONTAIN -> "-contain";
                case START -> "-start";
                case END -> "-end";
                case REGEX -> "-regex";
            };
        }
    }
}
