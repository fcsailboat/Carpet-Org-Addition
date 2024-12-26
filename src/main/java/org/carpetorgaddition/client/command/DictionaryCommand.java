package org.carpetorgaddition.client.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import org.carpetorgaddition.client.command.argument.ClientObjectArgumentType;
import org.carpetorgaddition.client.util.ClientMessageUtils;
import org.carpetorgaddition.util.EnchantmentUtils;
import org.carpetorgaddition.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class DictionaryCommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal("dictionary");
            // 注册每一项子命令
            // TODO 获取手上的物品id
            for (DictionaryType value : DictionaryType.values()) {
                builder.then(ClientCommandManager.literal(value.name)
                        .then(ClientCommandManager.argument(value.name, value.getArgumentType())
                                .executes(context -> getId(context, value))));
            }
            dispatcher.register(builder);
        });
    }

    // 获取对象id
    private static int getId(CommandContext<FabricClientCommandSource> context, DictionaryType type) {
        List<?> list = ClientObjectArgumentType.getType(context, type.name);
        // list集合至少有一个元素，不与任何对象匹配的字符串在解析命令时不会成功
        if (list.size() == 1) {
            // 字符串只对应一个对象
            Object t = list.getFirst();
            // 获取对象id
            String id = type.id(t);
            sendFeedback(type.name(t), id);
        } else {
            // 字符串对应多个对象
            sendFeedback(list.size());
            for (Object t : list) {
                sendFeedback(type.id(t));
            }
        }
        return list.size();
    }

    // 发送命令反馈
    private static void sendFeedback(Text text, String id) {
        ClientMessageUtils.sendMessage("carpet.client.commands.dictionary.id", text, canCopyId(id));
    }

    private static void sendFeedback(int count) {
        // TODO 指定字符串对应%s个对象： -> 字符串匹配%s个对象：
        ClientMessageUtils.sendMessage("carpet.client.commands.dictionary.multiple.id", count);
    }

    private static void sendFeedback(String id) {
        ClientMessageUtils.sendMessage("carpet.client.commands.dictionary.multiple.each", canCopyId(id));
    }

    // 将字符串id转换成可以单击复制的形式
    @NotNull
    private static MutableText canCopyId(String id) {
        return TextUtils.copy(id, id, TextUtils.translate("chat.copy.click"), Formatting.GREEN);
    }

    private enum DictionaryType {
        /**
         * 物品
         */
        ITEM("item"),
        /**
         * 方块
         */
        BLOCK("block"),
        /**
         * 实体
         */
        ENTITY("entity"),
        /**
         * 附魔
         */
        ENCHANTMENT("enchantment"),
        /**
         * 状态效果
         */
        STATUS_EFFECT("statusEffect"),
        /**
         * 生物群系
         */
        BIOME("biome");
        /**
         * 子命令和子命令参数
         */
        private final String name;

        DictionaryType(String name) {
            this.name = name;
        }

        // 获取对象id
        private String id(Object obj) {
            ClientPlayerEntity player = Objects.requireNonNull(MinecraftClient.getInstance().player);
            DynamicRegistryManager.Immutable registry = player.networkHandler.getRegistryManager();
            return switch (this) {
                case ITEM -> Registries.ITEM.getId((Item) obj).toString();
                case BLOCK -> Registries.BLOCK.getId((Block) obj).toString();
                case ENTITY -> Registries.ENTITY_TYPE.getId((EntityType<?>) obj).toString();
                case ENCHANTMENT -> {
                    Identifier id = registry.get(RegistryKeys.ENCHANTMENT).getId((Enchantment) obj);
                    yield Objects.requireNonNull(id, "无法获取附魔id").toString();
                }
                case STATUS_EFFECT -> {
                    Identifier id = registry.get(RegistryKeys.STATUS_EFFECT).getId((StatusEffect) obj);
                    yield Objects.requireNonNull(id, "无法获取状态效果id").toString();
                }
                case BIOME -> {
                    Identifier id = registry.get(RegistryKeys.BIOME).getId((Biome) obj);
                    yield Objects.requireNonNull(id, "无法获取生物群系id").toString();
                }
            };
        }

        // 获取对象名称
        private Text name(Object obj) {
            // 获取客户端玩家
            ClientPlayerEntity player = Objects.requireNonNull(MinecraftClient.getInstance().player);
            // 获取注册管理器
            DynamicRegistryManager.Immutable registry = player.networkHandler.getRegistryManager();
            return switch (this) {
                case ITEM -> ((Item) obj).getName();
                case BLOCK -> ((Block) obj).getName();
                case ENTITY -> ((EntityType<?>) obj).getName();
                case ENCHANTMENT -> EnchantmentUtils.getName((Enchantment) obj);
                case STATUS_EFFECT -> ((StatusEffect) obj).getName();
                case BIOME -> {
                    Identifier id = Objects.requireNonNull(registry.get(RegistryKeys.BIOME).getId((Biome) obj), "无法获取生物群系id");
                    String key = id.toTranslationKey("biome");
                    yield TextUtils.translate(key);
                }
            };
        }

        // 获取参数类型
        private ArgumentType<?> getArgumentType() {
            return switch (this) {
                case ITEM -> new ClientObjectArgumentType.ClientItemArgumentType();
                case BLOCK -> new ClientObjectArgumentType.ClientBlockArgumentType();
                case ENTITY -> new ClientObjectArgumentType.ClientEntityArgumentType();
                case ENCHANTMENT -> new ClientObjectArgumentType.ClientEnchantmentArgumentType();
                case STATUS_EFFECT -> new ClientObjectArgumentType.ClientStatusEffectArgumentType();
                case BIOME -> new ClientObjectArgumentType.ClientBiomeArgumentType();
            };
        }
    }
}
