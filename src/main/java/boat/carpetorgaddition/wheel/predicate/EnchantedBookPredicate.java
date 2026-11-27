package boat.carpetorgaddition.wheel.predicate;

import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.util.IdentityUtils;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.function.Predicate;

public abstract class EnchantedBookPredicate implements Predicate<Holder<Enchantment>> {
    public static EnchantedBookPredicate of(MinecraftServer server, Holder<Enchantment> enchantment) {
        return new Single(server, enchantment);
    }

    public static EnchantedBookPredicate of(MinecraftServer server, List<Holder<Enchantment>> enchantments, String name) {
        if (enchantments.size() == 1) {
            return of(server, enchantments.getFirst());
        } else {
            return new AnyOf(server, enchantments, name);
        }
    }

    public abstract Component getDisplayName(int level);

    public abstract Component getDisplayName();

    private static class Single extends EnchantedBookPredicate {
        private final Holder<Enchantment> enchantment;
        private final String id;

        private Single(MinecraftServer server, Holder<Enchantment> enchantment) {
            this.enchantment = enchantment;
            this.id = IdentityUtils.getId(server, this.enchantment)
                    .map(Identifier::toString)
                    .orElse("<unregistered>");
        }

        @Override
        public boolean test(Holder<Enchantment> enchantment) {
            return this.enchantment.equals(enchantment);
        }

        @Override
        public Component getDisplayName(int level) {
            TextBuilder builder = TextBuilder.of(EnchantmentUtils.getName(this.enchantment, level));
            builder.setHover(this.id);
            return builder.build();
        }

        @Override
        public Component getDisplayName() {
            // 获取附魔名称，不带等级
            TextBuilder builder = TextBuilder.of(EnchantmentUtils.getName(enchantment));
            builder.setHover(this.id);
            return builder.build();
        }
    }

    private static class AnyOf extends EnchantedBookPredicate {
        private final List<Single> predicates;
        private final String name;

        private AnyOf(MinecraftServer server, List<Holder<Enchantment>> enchantments, String name) {
            this.name = name;
            this.predicates = enchantments.stream().map(enchantment -> new Single(server, enchantment)).toList();
        }

        @Override
        public boolean test(Holder<Enchantment> enchantment) {
            return this.predicates.stream().anyMatch(predicate -> predicate.test(enchantment));
        }

        @Override
        public Component getDisplayName(int level) {
            return null;
        }

        @Override
        public Component getDisplayName() {
            TextBuilder builder = TextBuilder.of(this.name);
            TextJoiner joiner = new TextJoiner();
            for (Single single : this.predicates) {
                Component component = IdentityUtils.getName(single.enchantment);
                joiner.newline(component);
            }
            return builder.setColor(ChatFormatting.GRAY).setHover(joiner.join()).build();
        }
    }
}
