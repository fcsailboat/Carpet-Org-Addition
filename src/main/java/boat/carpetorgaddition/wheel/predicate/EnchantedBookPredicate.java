package boat.carpetorgaddition.wheel.predicate;

import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.function.Predicate;

public class EnchantedBookPredicate implements Predicate<ItemStack> {
    private final MinecraftServer server;
    private final Enchantment enchantment;
    private final String id;

    public EnchantedBookPredicate(MinecraftServer server, Enchantment enchantment) {
        this.server = server;
        this.enchantment = enchantment;
        this.id = ServerUtils.getId(server, this.enchantment)
                .map(Identifier::toString)
                .orElse("<unregistered>");
    }

    @Override
    public boolean test(ItemStack itemStack) {
        return itemStack.is(Items.ENCHANTED_BOOK) && getLevel(itemStack) > 0;
    }

    public int getLevel(ItemStack itemStack) {
        int level = EnchantmentUtils.getLevel(this.server, this.enchantment, itemStack);
        if (level > 0) {
            return level;
        }
        return -1;
    }

    public Component getWithLevel(int level) {
        TextBuilder builder = TextBuilder.of(EnchantmentUtils.getName(this.enchantment, level));
        builder.setHover(this.id);
        return builder.build();
    }

    public Component getDisplayName() {
        // 获取附魔名称，不带等级
        TextBuilder builder = TextBuilder.of(EnchantmentUtils.getName(enchantment));
        builder.setHover(this.id);
        return builder.build();
    }
}
