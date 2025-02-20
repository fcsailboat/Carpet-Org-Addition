package org.carpetorgaddition.util.wheel;

import carpet.CarpetServer;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemPredicateArgumentType;
import net.minecraft.command.argument.ItemPredicateArgumentType.ItemStackPredicateArgument;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.world.World;
import org.carpetorgaddition.util.TextUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class ItemStackPredicate implements Predicate<ItemStack> {
    private final Predicate<ItemStack> predicate;
    private final String input;
    public static final ItemStackPredicate EMPTY = new ItemStackPredicate(Items.AIR);

    public ItemStackPredicate(CommandContext<ServerCommandSource> context, String arguments) {
        for (ParsedCommandNode<ServerCommandSource> commandNode : context.getNodes()) {
            if (commandNode.getNode() instanceof ArgumentCommandNode<?, ?> node && Objects.equals(node.getName(), arguments)) {
                StringRange range = commandNode.getRange();
                this.input = context.getInput().substring(range.getStart(), range.getEnd());
                ItemStackPredicateArgument predicate = ItemPredicateArgumentType.getItemStackPredicate(context, arguments);
                if (this.input.startsWith("*")) {
                    this.predicate = itemStack -> !itemStack.isEmpty() && predicate.test(itemStack);
                } else {
                    this.predicate = predicate;
                }
                return;
            }
        }
        throw new IllegalArgumentException();
    }

    public ItemStackPredicate(Item item) {
        this.predicate = itemStack -> itemStack.isOf(item);
        this.input = Registries.ITEM.getId(item).toString();
    }

    private ItemStackPredicate(Predicate<ItemStack> predicate, String input) {
        this.predicate = predicate;
        this.input = input;
    }

    /**
     * @return 自身是否可以与空气物品匹配
     */
    public boolean isEmpty() {
        return this.test(Items.AIR.getDefaultStack());
    }

    @Override
    public boolean test(ItemStack itemStack) {
        return this.predicate.test(itemStack);
    }

    public static ItemStackPredicate load(String input) {
        CommandRegistryAccessor accessor = (CommandRegistryAccessor) CarpetServer.minecraft_server.getCommandManager();
        CommandRegistryAccess commandRegistryAccess = accessor.getAccess();
        try {
            ItemPredicateArgumentType.ItemStackPredicateArgument predicate = ItemPredicateArgumentType.itemPredicate(commandRegistryAccess).parse(new StringReader(input));
            return new ItemStackPredicate(predicate, input);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return 谓词的字符串首字母
     */
    public Text getInitialUpperCase() {
        Text result = null;
        if (this.isEmpty()) {
            result = TextUtils.createText("[A]");
            result = TextUtils.setColor(result.copy(), Formatting.DARK_GRAY);
            return TextUtils.hoverText(result, Items.AIR.getName());
        }
        if (this.input.startsWith("#")) {
            result = TextUtils.createText("[#]");
        } else if (this.input.startsWith("*")) {
            result = TextUtils.createText("[*]");
        } else if (this.input.contains("[")) {
            result = TextUtils.createText("[@]");
        }
        if (result != null) {
            return TextUtils.hoverText(result.copy(), this.input);
        }
        // 如果有命名空间，将“:”后的单词首字母取出，否则直接获取首字母
        String[] split = this.input.split(":");
        int index = split.length == 1 ? 0 : 1;
        result = TextUtils.createText("[" + Character.toUpperCase(split[index].charAt(0)) + "]");
        Text name = Registries.ITEM.get(Identifier.of(this.input)).getName();
        return TextUtils.hoverText(result, name);
    }

    /**
     * 将当前命令参数输入转换成文本对象
     *
     * @return 如果是谓词，返回封装的字符串，如果是物品，返回物品名称
     * @throws InvalidIdentifierException 如果解析到了不正确的字符串
     */
    public Text toText() throws InvalidIdentifierException {
        if (this.isEmpty()) {
            return Items.AIR.getName();
        }
        if (canConvertItem()) {
            Identifier identifier = Identifier.of(this.input);
            return Registries.ITEM.get(identifier).getName();
        }
        if (this.input.length() > 30) {
            String substring = this.input.substring(0, 30);
            MutableText ellipsis = TextUtils.createText("...");
            MutableText result = TextUtils.appendAll(substring, ellipsis);
            return TextUtils.toGrayItalic(TextUtils.hoverText(result, this.input));
        }
        return TextUtils.createText(this.input);
    }

    /**
     * 将命令参数转换为对应物品
     */
    public Item asItem() {
        if (this.isEmpty()) {
            return Items.AIR;
        }
        if (this.canConvertItem()) {
            return Registries.ITEM.get(Identifier.of(this.input));
        }
        throw new IllegalArgumentException();
    }

    /**
     * 获取指定配方的输出物品
     *
     * @param predicates  合成配方
     * @param widthHeight 合成方格的宽高，工作台是3，物品栏是2
     * @param fakePlayer  合成该物品的假玩家
     * @return 如果能够合成物品，返回合成输出物品，否则返回空物品，如果配方中包含不能转换为物品的元素，也返回空物品
     */
    public static ItemStack getCraftOutput(ItemStackPredicate[] predicates, int widthHeight, EntityPlayerMPFake fakePlayer) {
        for (ItemStackPredicate predicate : predicates) {
            if (predicate.canConvertItem()) {
                continue;
            }
            return ItemStack.EMPTY;
        }
        // 前面的循环中已经判断了字符串是否能转换成物品，所以这里不需要再判断
        List<ItemStack> list = Arrays.stream(predicates)
                .map(ItemStackPredicate::getInput)
                .map(Identifier::of)
                .map(Registries.ITEM::get)
                .map(Item::getDefaultStack)
                .toList();
        CraftingRecipeInput input = CraftingRecipeInput.create(widthHeight, widthHeight, list);
        World world = fakePlayer.getWorld();
        Optional<RecipeEntry<CraftingRecipe>> optional = fakePlayer.server.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, input, world);
        return optional.map(recipe -> recipe.value().craft(input, world.getRegistryManager())).orElse(ItemStack.EMPTY);
    }

    /**
     * 将字符串ID转换为物品
     */
    public static Item stringAsItem(String id) {
        return Registries.ITEM.get(Identifier.of(id));
    }

    public boolean canConvertItem() {
        return !(this.input.startsWith("#") || this.input.startsWith("*") || this.input.matches(".*\\[.*]"));
    }

    public String getInput() {
        return this.input;
    }

    @Override
    public String toString() {
        return this.input;
    }
}
