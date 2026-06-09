package boat.carpetorgaddition.util;

import boat.carpetorgaddition.wheel.CommandRegistryAccessor;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class EnchantmentUtils {
    /**
     * @return 指定物品上是否有指定附魔
     */
    public static boolean hasEnchantment(Level world, ResourceKey<Enchantment> key, ItemStack itemStack) {
        Optional<Registry<Enchantment>> optional = world.registryAccess().lookup(Registries.ENCHANTMENT);
        if (optional.isEmpty()) {
            return false;
        }
        Enchantment enchantment = optional.get().getValue(key);
        return getLevel(world, enchantment, itemStack) > 0;
    }

    /**
     * @return 附魔是否与注册项对应
     */
    public static boolean isSpecified(Level world, ResourceKey<Enchantment> key, Enchantment enchantment) {
        Optional<Registry<Enchantment>> optional = world.registryAccess().lookup(Registries.ENCHANTMENT);
        if (optional.isEmpty()) {
            return false;
        }
        Enchantment value = optional.get().getValue(key);
        return value != null && value.equals(enchantment);
    }

    /**
     * 判断指定魔咒是否是保护类魔咒
     *
     * @return 指定魔咒是否是 {@code 保护}、{@code 爆炸保护}，{@code 火焰保护}或{@code 弹射物保护}
     */
    public static boolean isProtectionEnchantment(ResourceKey<Enchantment> key) {
        return key == Enchantments.PROTECTION || key == Enchantments.BLAST_PROTECTION || key == Enchantments.FIRE_PROTECTION || key == Enchantments.PROJECTILE_PROTECTION;
    }

    /**
     * 判断指定魔咒是否为伤害类魔咒
     *
     * @return 指定魔咒是否是 {@code 锋利}、{@code 亡灵杀手}，{@code 节肢杀手}、{@code 穿刺}，{@code 致密}或{@code 破甲}
     */
    public static boolean isDamageEnchantment(ResourceKey<Enchantment> key) {
        return key == Enchantments.SHARPNESS || key == Enchantments.SMITE || key == Enchantments.BANE_OF_ARTHROPODS || key == Enchantments.IMPALING || key == Enchantments.DENSITY || key == Enchantments.BREACH;
    }

    public static int getLevel(Level world, Enchantment enchantment, ItemStack itemStack) {
        RegistryAccess registryManager = world.registryAccess();
        return getLevel(enchantment, itemStack, registryManager);
    }

    public static int getLevel(MinecraftServer server, Enchantment enchantment, ItemStack itemStack) {
        RegistryAccess.Frozen registryManager = server.registryAccess();
        return getLevel(enchantment, itemStack, registryManager);
    }

    /**
     * @return 获取指定物品上指定附魔的等级
     */
    private static int getLevel(Enchantment enchantment, ItemStack itemStack, RegistryAccess registryManager) {
        Optional<Registry<Enchantment>> optional = registryManager.lookup(Registries.ENCHANTMENT);
        if (optional.isEmpty()) {
            return 0;
        }
        Holder<Enchantment> entry = optional.get().wrapAsHolder(enchantment);
        if (itemStack.is(Items.ENCHANTED_BOOK)) {
            ItemEnchantments component = itemStack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            return component.getLevel(entry);
        }
        return EnchantmentHelper.getItemEnchantmentLevel(entry, itemStack);
    }

    /**
     * @return 获取一个附魔的名字，不带等级
     */
    @Deprecated
    public static Component getName(Enchantment enchantment) {
        TextBuilder builder = TextBuilder.of(enchantment.description());
        Holder<Enchantment> entry = Holder.direct(enchantment);
        // 如果是诅咒附魔，设置为红色，否则，设置为灰色
        ChatFormatting color = entry.is(EnchantmentTags.CURSE) ? ChatFormatting.RED : ChatFormatting.GRAY;
        builder.setColor(color);
        return builder.build();
    }

    /**
     * @param level 附魔的等级
     * @return 获取一个附魔的名字，带有等级
     */
    @Deprecated
    public static Component getName(Enchantment enchantment, int level) {
        TextJoiner joiner = new TextJoiner();
        joiner.append(getName(enchantment));
        if (level != 1 || enchantment.getMaxLevel() != 1) {
            joiner.append(CommonComponents.SPACE).append(LocalizationKey.literal("enchantment.level." + level).translate());
        }
        return joiner.join();
    }

    public static Component getName(Holder<Enchantment> holder) {
        TextBuilder builder = TextBuilder.of(holder.value().description());
        // 如果是诅咒附魔，设置为红色，否则，设置为灰色
        ChatFormatting color = holder.is(EnchantmentTags.CURSE) ? ChatFormatting.RED : ChatFormatting.GRAY;
        builder.setColor(color);
        return builder.build();
    }

    public static Component getName(Holder<Enchantment> holder, int level) {
        TextJoiner joiner = new TextJoiner();
        joiner.append(holder.value().description());
        if (level != 1 || holder.value().getMaxLevel() != 1) {
            joiner.append(CommonComponents.SPACE).append(LocalizationKey.literal("enchantment.level." + level).translate());
        }
        return TextBuilder.of(joiner.join())
                .setColor(holder.is(EnchantmentTags.CURSE) ? ChatFormatting.RED : ChatFormatting.GRAY)
                .build();
    }

    public static int getMaxLevel(Holder.Reference<Enchantment> enchantment) {
        return enchantment.value().getMaxLevel();
    }

    /**
     * @return 指定物品是否可以使用经验修复
     */
    public static boolean canRepairWithXp(ItemStack itemStack) {
        ItemEnchantments component = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : component.entrySet()) {
            Holder<Enchantment> registryEntry = entry.getKey();
            if (registryEntry.value().effects().has(EnchantmentEffectComponents.REPAIR_WITH_XP)) {
                return true;
            }
        }
        return false;
    }

    public static Optional<Holder.Reference<Enchantment>> parse(MinecraftServer server, Identifier id) {
        CommandRegistryAccessor accessor = (CommandRegistryAccessor) server.getCommands();
        CommandBuildContext access = accessor.carpet_Org_Addition$getAccess();
        ResourceArgument<Enchantment> resourced = ResourceArgument.resource(access, Registries.ENCHANTMENT);
        try {
            Holder.Reference<Enchantment> enchantment = resourced.parse(new StringReader(id.toString()));
            return Optional.of(enchantment);
        } catch (CommandSyntaxException e) {
            return Optional.empty();
        }
    }
}
