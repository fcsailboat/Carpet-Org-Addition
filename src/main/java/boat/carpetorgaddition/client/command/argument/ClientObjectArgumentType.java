package boat.carpetorgaddition.client.command.argument;

import boat.carpetorgaddition.client.util.ClientCommandUtils;
import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
import java.util.stream.StreamSupport;

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

    public static Map<String, ClientObjectArgumentType<?>> getDictionaryTypes() {
        return DictionaryHolder.DICTIONARY_TYPES;
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
                if (pattern.match(name.toLowerCase(Locale.ROOT), this.getNameAsString(t).toLowerCase(Locale.ROOT))) {
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
    public String getNameAsString(T t) {
        return getName(t).getString();
    }

    public Map.Entry<String, String> entry(T t) {
        return Map.entry(this.getNameAsString(t), this.getIdAsString(t));
    }

    public abstract String getIdAsString(T t);

    public String getObjectIdAsString(Object obj) {
        return getIdAsString(this.type().cast(obj));
    }

    public abstract Component getName(T t);

    public Component getObjectName(Object obj) {
        return this.getName(this.type().cast(obj));
    }

    protected abstract Class<T> type();

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
                        .map(entry -> Map.entry(quoteIfContainsSpace(entry.getKey()), getIdValue(entry.getValue())))
                        .forEach(entry -> {
                            String key = entry.getKey();
                            if (key.toLowerCase(Locale.ROOT).contains(remaining) || MathUtils.isPinyinMatch(key, remaining) || (ID_COMPLETION_NAME && entry.getValue().contains(remaining))) {
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

    private static String getIdValue(String id) {
        String[] split = id.split(":");
        return split.length == 2 ? split[1] : id;
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
        public Component getName(Item item) {
            return ServerUtils.getName(item);
        }

        @Override
        public String getIdAsString(Item item) {
            return ServerUtils.getIdAsString(item);
        }

        @Override
        protected Stream<Item> stream() {
            return BuiltInRegistries.ITEM.stream();
        }

        @Override
        protected Class<Item> type() {
            return Item.class;
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
        public Component getName(Block block) {
            return ServerUtils.getName(block);
        }

        @Override
        public String getIdAsString(Block block) {
            return ServerUtils.getIdAsString(block);
        }

        @Override
        protected Stream<Block> stream() {
            return BuiltInRegistries.BLOCK.stream();
        }

        @Override
        protected Class<Block> type() {
            return Block.class;
        }
    }

    /**
     * 实体参数
     */
    private static class ClientEntityArgumentType extends ClientObjectArgumentType<EntityType<?>> {
        @Override
        public Component getName(EntityType<?> entityType) {
            return ServerUtils.getName(entityType);
        }

        @Override
        public String getIdAsString(EntityType<?> entityType) {
            return ServerUtils.getIdAsString(entityType);
        }

        @Override
        protected Stream<EntityType<?>> stream() {
            return ClientUtils.getRegistryAccess().lookupOrThrow(Registries.ENTITY_TYPE).stream();
        }

        @SuppressWarnings("unchecked")
        @Override
        protected Class<EntityType<?>> type() {
            return (Class<EntityType<?>>) (Class<?>) EntityType.class;
        }
    }

    /**
     * 魔咒参数
     */
    private static class ClientEnchantmentArgumentType extends ClientObjectArgumentType<Holder<Enchantment>> {
        @Override
        public Component getName(Holder<Enchantment> holder) {
            return ServerUtils.getName(holder);
        }

        @Override
        public String getIdAsString(Holder<Enchantment> holder) {
            return ServerUtils.getIdAsString(ClientUtils.getRegistryAccess(), holder);
        }

        @Override
        protected Stream<Holder<Enchantment>> stream() {
            RegistryAccess registryAccess = ClientUtils.getRegistryAccess();
            Registry<Enchantment> registry = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
            return StreamSupport.stream(registry.asHolderIdMap().spliterator(), false);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected Class<Holder<Enchantment>> type() {
            return (Class<Holder<Enchantment>>) (Class<?>) Holder.class;
        }
    }

    /**
     * 状态效果参数
     */
    private static class ClientStatusEffectArgumentType extends ClientObjectArgumentType<MobEffect> {
        @Override
        public Component getName(MobEffect mobEffect) {
            return ServerUtils.getName(mobEffect);
        }

        @Override
        public String getIdAsString(MobEffect mobEffect) {
            return ServerUtils.getIdAsString(ClientUtils.getRegistryAccess(), mobEffect);
        }

        @Override
        protected Stream<MobEffect> stream() {
            return ClientUtils.getRegistryAccess().lookupOrThrow(Registries.MOB_EFFECT).stream();
        }

        @Override
        protected Class<MobEffect> type() {
            return MobEffect.class;
        }
    }

    private static class ClientBiomeArgumentType extends ClientObjectArgumentType<Biome> {
        @Override
        public Component getName(Biome biome) {
            return ServerUtils.getName(ClientUtils.getRegistryAccess(), biome);
        }

        @Override
        public String getIdAsString(Biome biome) {
            return ServerUtils.getIdAsString(ClientUtils.getRegistryAccess(), biome);
        }

        @Override
        protected Stream<Biome> stream() {
            return ClientUtils.getRegistryAccess().lookupOrThrow(Registries.BIOME).stream();
        }

        @Override
        protected Class<Biome> type() {
            return Biome.class;
        }
    }

    /**
     * 游戏模式参数
     */
    private static class ClientGameModeArgumentType extends ClientObjectArgumentType<GameType> {
        @Override
        public Component getName(GameType gameType) {
            return ServerUtils.getName(gameType);
        }

        @Override
        public String getIdAsString(GameType gameType) {
            return ServerUtils.getIdAsString(gameType);
        }

        @Override
        protected Stream<GameType> stream() {
            return Stream.of(GameType.values());
        }

        @Override
        protected Class<GameType> type() {
            return GameType.class;
        }
    }

    /**
     * 游戏规则参数
     */
    private static class ClientGameRuleArgumentType extends ClientObjectArgumentType<GameRule<?>> {
        @Override
        public Component getName(GameRule<?> gameRule) {
            return ServerUtils.getName(gameRule);
        }

        @Override
        public String getIdAsString(GameRule<?> gameRule) {
            return ServerUtils.getIdAsString(ClientUtils.getRegistryAccess(), gameRule);
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

        @SuppressWarnings("unchecked")
        @Override
        protected Class<GameRule<?>> type() {
            return (Class<GameRule<?>>) (Class<?>) GameRule.class;
        }
    }

    private static class DictionaryHolder {
        static final Map<String, ClientObjectArgumentType<?>> DICTIONARY_TYPES = new HashMap<>();

        static {
            DICTIONARY_TYPES.put("item", new ClientItemArgumentType(false));
            DICTIONARY_TYPES.put("block", new ClientBlockArgumentType(false));
            DICTIONARY_TYPES.put("entity", new ClientEntityArgumentType());
            DICTIONARY_TYPES.put("enchant", new ClientEnchantmentArgumentType());
            DICTIONARY_TYPES.put("effect", new ClientStatusEffectArgumentType());
            DICTIONARY_TYPES.put("biome", new ClientBiomeArgumentType());
            DICTIONARY_TYPES.put("gamemode", new ClientGameModeArgumentType());
            DICTIONARY_TYPES.put("gamerule", new ClientGameRuleArgumentType());
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
