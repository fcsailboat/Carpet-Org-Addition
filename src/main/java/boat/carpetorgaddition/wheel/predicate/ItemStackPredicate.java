package boat.carpetorgaddition.wheel.predicate;

import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.CommandRegistryAccessor;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.IdentifierException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument.Result;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Predicate;

public class ItemStackPredicate implements Predicate<ItemStack>, Comparable<ItemStackPredicate> {
    private final Predicate<ItemStack> predicate;
    private final String input;
    private final boolean isWildcard;
    @Nullable
    private final Item convert;
    public static final ItemStackPredicate EMPTY = new ItemStackPredicate(Items.AIR);
    /**
     * 匹配任何非空气物品
     */
    public static final ItemStackPredicate WILDCARD = new ItemStackPredicate();

    /**
     * 创建一个匹配任意非空气物品的物品谓词
     */
    private ItemStackPredicate() {
        this.predicate = itemStack -> !itemStack.isEmpty();
        this.input = "*";
        this.isWildcard = true;
        this.convert = null;
    }

    public ItemStackPredicate(CommandContext<CommandSourceStack> context, String arguments) {
        for (ParsedCommandNode<CommandSourceStack> commandNode : context.getNodes()) {
            if (commandNode.getNode() instanceof ArgumentCommandNode<?, ?> node && Objects.equals(node.getName(), arguments)) {
                StringRange range = commandNode.getRange();
                this.input = context.getInput().substring(range.getStart(), range.getEnd());
                Result predicate = ItemPredicateArgument.getItemPredicate(context, arguments);
                this.isWildcard = this.isWildcard();
                this.predicate = this.isWildcard ? itemStack -> !itemStack.isEmpty() && predicate.test(itemStack) : predicate;
                this.convert = tryConvert(this.input);
                return;
            }
        }
        throw new IllegalArgumentException("Argument '%s' not found in command context".formatted(arguments));
    }

    public ItemStackPredicate(@NotNull Item item) {
        this.predicate = itemStack -> itemStack.is(item);
        this.input = ServerUtils.getIdAsString(item);
        this.isWildcard = false;
        this.convert = item;
    }

    public static ItemStackPredicate of(Collection<Item> collection, String name) {
        LinkedHashSet<Item> set = new LinkedHashSet<>(collection);
        return switch (set.size()) {
            case 0 -> EMPTY;
            case 1 -> new ItemStackPredicate(set.getFirst());
            default -> new AnyOfItemPredicate(set, name);
        };
    }

    private ItemStackPredicate(LinkedHashSet<Item> set) {
        this.predicate = itemStack -> set.contains(itemStack.getItem());
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (Item item : set) {
            joiner.add(ServerUtils.getIdAsString(item));
        }
        this.input = joiner.toString();
        this.isWildcard = false;
        this.convert = null;
    }

    private ItemStackPredicate(Predicate<ItemStack> predicate, String input) {
        this.input = input;
        this.isWildcard = this.isWildcard();
        this.predicate = this.isWildcard ? itemStack -> !itemStack.isEmpty() && predicate.test(itemStack) : predicate;
        this.convert = tryConvert(input);
    }

    @Nullable
    private static Item tryConvert(String input) {
        if (input.startsWith("#") || input.startsWith("*") || input.matches(".*\\[.*]")) {
            return null;
        }
        try {
            return BuiltInRegistries.ITEM.getValue(Identifier.parse(input));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * @return 自身是否可以与空气物品匹配
     */
    public boolean isEmpty() {
        return this.test(ItemStack.EMPTY);
    }

    @Override
    public boolean test(ItemStack itemStack) {
        return this.predicate.test(itemStack);
    }

    /**
     * 从字符串解析物品谓词
     *
     * @throws IllegalStateException 如果在游戏外加载物品谓词则抛出
     */
    public static ItemStackPredicate parse(String input) {
        Optional<MinecraftServer> optional = ServerUtils.getCurrentServer();
        if (optional.isEmpty()) {
            throw new IllegalStateException("Server not initialized");
        }
        CommandRegistryAccessor accessor = (CommandRegistryAccessor) optional.get().getCommands();
        CommandBuildContext access = accessor.carpet_Org_Addition$getAccess();
        try {
            StringReader reader = new StringReader(input);
            Result predicate = ItemPredicateArgument.itemPredicate(access).parse(reader);
            return new ItemStackPredicate(predicate, input);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return 当前谓词字符串是否为通配符
     */
    private boolean isWildcard() {
        return "*".equals(this.input) || "*[]".equals(this.input);
    }

    /**
     * @return 谓词的字符串首字母
     */
    public Component getInitialUpperCase() {
        if (this.isEmpty()) {
            TextBuilder builder = TextBuilder.of("[A]");
            builder.setColor(ChatFormatting.DARK_GRAY);
            builder.setHover(ServerUtils.getName(Items.AIR));
            return builder.build();
        }
        TextBuilder builder = null;
        if (this.input.startsWith("#")) {
            builder = TextBuilder.of("[#]");
        } else if (this.input.startsWith("*")) {
            builder = TextBuilder.of("[*]");
        } else if (this.input.contains("[")) {
            // 不会执行到这里
            builder = TextBuilder.of("[@]");
        }
        if (builder != null) {
            return builder.setHover(this.input).build();
        }
        // 如果有命名空间，将“:”后的单词首字母取出，否则直接获取首字母
        String[] split = this.input.split(":");
        int index = split.length == 1 ? 0 : 1;
        builder = TextBuilder.of("[" + Character.toUpperCase(split[index].charAt(0)) + "]");
        Component name = ServerUtils.getName(BuiltInRegistries.ITEM.getValue(Identifier.parse(this.input)));
        return builder.setHover(name).build();
    }

    /**
     * 将当前命令参数输入转换成文本对象
     *
     * @return 如果是谓词，返回封装的字符串，如果是物品，返回物品名称
     * @throws IdentifierException 如果解析到了不正确的字符串
     */
    public Component getDisplayName() throws IdentifierException {
        if (this.isEmpty()) {
            return ServerUtils.getName(Items.AIR);
        }
        if (this.isWildcard) {
            return LocalizationKeys.Item.ANY_ITEM.translate();
        }
        if (this.convert != null) {
            return ServerUtils.getName(this.convert);
        }
        if (this.input.length() > 30) {
            String substring = this.input.substring(0, 30);
            Component ellipsis = TextBuilder.create("...");
            Component result = TextBuilder.combineAll(substring, ellipsis);
            TextBuilder builder = TextBuilder.of(result).setGrayItalic().setHover(this.input);
            return builder.build();
        }
        return TextBuilder.create(this.input);
    }

    /**
     * 将命令参数转换为对应物品
     */
    @Deprecated(forRemoval = true)
    public Item asItem() {
        if (this.isEmpty()) {
            return Items.AIR;
        }
        if (this.isConvertible()) {
            return BuiltInRegistries.ITEM.getValue(Identifier.parse(this.input));
        }
        throw new UnsupportedOperationException(this.input + " cannot be converted to item");
    }

    /**
     * 获取input转换而来的物品
     */
    public Optional<Item> getConvert() {
        return Optional.ofNullable(this.convert);
    }

    /**
     * @return {@code input}字符串是否可以转换为物品
     */
    @Deprecated(forRemoval = true)
    public boolean isConvertible() {
        return !(this.input.startsWith("#") || this.input.startsWith("*") || this.input.matches(".*\\[.*]"));
    }

    @Override
    public String toString() {
        return this.input;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ItemStackPredicate that = (ItemStackPredicate) o;
        if (this.isEmpty() && that.isEmpty()) {
            return true;
        }
        return Objects.equals(input, that.input);
    }

    @Override
    public int hashCode() {
        return this.isEmpty() ? 0 : Objects.hashCode(input);
    }

    @Override
    public int compareTo(@NonNull ItemStackPredicate o) {
        if (this.isEmpty() && o.isEmpty()) {
            return 0;
        }
        if (this.isEmpty() && !o.isEmpty()) {
            return 1;
        }
        if (!this.isEmpty() && o.isEmpty()) {
            return -1;
        }
        return String.CASE_INSENSITIVE_ORDER.compare(this.input, o.input);
    }

    public static class AnyOfItemPredicate extends ItemStackPredicate {
        private final String name;

        private AnyOfItemPredicate(LinkedHashSet<Item> items, String name) {
            super(items);
            this.name = name;
        }

        @Override
        public Component getDisplayName() throws IdentifierException {
            if (this.name.length() > 30) {
                String substring = this.name.substring(0, 30);
                Component ellipsis = TextBuilder.create("...");
                Component result = TextBuilder.combineAll(substring, ellipsis);
                TextBuilder builder = TextBuilder.of(result).setGrayItalic().setHover(name);
                return builder.build();
            }
            return TextBuilder.of(this.name).setColor(ChatFormatting.GRAY).build();
        }
    }
}
