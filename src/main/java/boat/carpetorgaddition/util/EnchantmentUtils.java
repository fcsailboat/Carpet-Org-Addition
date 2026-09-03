package boat.carpetorgaddition.util;

import boat.carpetorgaddition.wheel.CommandRegistryAccessor;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
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

    /**
     * @return 获取指定物品上指定附魔的等级
     */
    public static int getLevel(Holder<Enchantment> enchantment, ItemStack itemStack) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(itemStack);
        return enchantments.getLevel(enchantment);
    }

    public static Component getName(Holder<Enchantment> holder) {
        TextBuilder builder = TextBuilder.of(holder.value().description());
        // 如果是诅咒附魔，设置为红色，否则，设置为灰色
        ChatFormatting color = holder.is(EnchantmentTags.CURSE) ? ChatFormatting.RED : ChatFormatting.GRAY;
        builder.setColor(color);
        return builder.build();
    }

    public static Component getName(Holder<Enchantment> holder, int level) {
        return Enchantment.getFullname(holder, level);
    }

    public static int getMaxLevel(Holder.Reference<Enchantment> enchantment) {
        return enchantment.value().getMaxLevel();
    }

    public static boolean hasComponent(ItemStack itemStack, DataComponentType<?> type) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(itemStack);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            if (holder.value().effects().has(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return 指定物品是否可以使用经验修复
     */
    public static boolean canRepairWithXp(ItemStack itemStack) {
        return hasComponent(itemStack, EnchantmentEffectComponents.REPAIR_WITH_XP);
    }

    public static boolean hasSilkTouch(ItemStack itemStack) {
        return hasEnchantment(itemStack, Enchantments.SILK_TOUCH);
    }

    public static boolean hasEnchantment(ItemStack itemStack, ResourceKey<Enchantment> enchantment) {
        ItemEnchantments enchantments = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getKey().is(enchantment)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasBookEnchantment(ItemStack itemStack, ResourceKey<Enchantment> enchantment) {
        if (itemStack.is(Items.ENCHANTED_BOOK)) {
            ItemEnchantments enchantments = itemStack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                if (entry.getKey().is(enchantment)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasEnchantment(ItemStack itemStack, Holder<Enchantment> enchantment) {
        ItemEnchantments enchantments = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getKey().equals(enchantment)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasBookEnchantment(ItemStack itemStack, Holder<Enchantment> enchantment) {
        if (itemStack.is(Items.ENCHANTED_BOOK)) {
            ItemEnchantments enchantments = itemStack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                if (entry.getKey().equals(enchantment)) {
                    return true;
                }
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
