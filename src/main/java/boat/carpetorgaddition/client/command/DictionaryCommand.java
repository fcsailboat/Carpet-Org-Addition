package boat.carpetorgaddition.client.command;

import boat.carpetorgaddition.client.command.argument.ClientObjectArgumentType;
import boat.carpetorgaddition.client.util.ClientMessageUtils;
import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gamerules.GameRule;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class DictionaryCommand extends AbstractClientCommand {
    public static final String DEFAULT_COMMAND_NAME = "dictionary";
    private static final String UNREGISTERED = "[<unregistered>]";
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("dictionary");
    public static final LocalizationKey MULTIPLE = KEY.then("multiple");

    public DictionaryCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommands.literal(name);
        // 注册每一项子命令
        for (DictionaryType value : DictionaryType.values()) {
            builder.then(ClientCommands.literal(value.name)
                    .then(ClientCommands.argument(value.name, value.getArgumentType())
                            .executes(context -> getId(context, value))));
        }
        this.dispatcher.register(builder);
    }

    // 获取对象id
    private int getId(CommandContext<FabricClientCommandSource> context, DictionaryType type) {
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
    private void sendFeedback(Component text, String id) {
        ClientMessageUtils.sendMessage(KEY.then("single").translate(text, canCopyId(id)));
    }

    private void sendFeedback(int count) {
        ClientMessageUtils.sendMessage(MULTIPLE.then("head").translate(count));
    }

    private void sendFeedback(String id) {
        ClientMessageUtils.sendMessage(MULTIPLE.then("each").translate(canCopyId(id)));
    }

    // 将字符串id转换成可以单击复制的形式
    @NonNull
    private Component canCopyId(String id) {
        return TextBuilder.of(id)
                .setCopyToClipboard(id)
                .setHover(TextProvider.COPY_CLICK)
                .setColor(ChatFormatting.GREEN)
                .build();
    }

    @Override
    public String getDefaultName() {
        return DEFAULT_COMMAND_NAME;
    }

    public enum DictionaryType {
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
        ENCHANTMENT("enchant"),
        /**
         * 状态效果
         */
        STATUS_EFFECT("effect"),
        /**
         * 生物群系
         */
        BIOME("biome"),
        /**
         * 游戏模式
         */
        GAME_MODE("gamemode"),
        /**
         * 游戏规则
         */
        GAME_RULE("gamerule");
        private final String name;

        DictionaryType(String name) {
            this.name = name;
        }

        // 获取对象id
        private String id(Object obj) {
            LocalPlayer player = ClientUtils.getPlayer();
            RegistryAccess.Frozen registry = player.connection.registryAccess();
            return switch (this) {
                case ITEM -> BuiltInRegistries.ITEM.getKey((Item) obj).toString();
                case BLOCK -> BuiltInRegistries.BLOCK.getKey((Block) obj).toString();
                case ENTITY -> BuiltInRegistries.ENTITY_TYPE.getKey((EntityType<?>) obj).toString();
                case ENCHANTMENT -> registry.lookup(Registries.ENCHANTMENT)
                        .map(enchantment -> enchantment.getKey((Enchantment) obj))
                        .map(Identifier::toString)
                        .orElse(UNREGISTERED);
                case STATUS_EFFECT -> registry.lookup(Registries.MOB_EFFECT)
                        .map(effect -> effect.getKey((MobEffect) obj))
                        .map(Identifier::toString)
                        .orElse(UNREGISTERED);
                case BIOME -> registry.lookup(Registries.BIOME)
                        .map(biome -> biome.getKey((Biome) obj))
                        .map(Identifier::toString)
                        .orElse(UNREGISTERED);
                case GAME_MODE -> ((GameType) obj).getSerializedName();
                case GAME_RULE -> registry.lookup(Registries.GAME_RULE)
                        .map(rules -> rules.getKey((GameRule<?>) obj))
                        .map(Identifier::toString)
                        .orElse("[<unregistered>]");
            };
        }

        // 获取对象名称
        private Component name(Object obj) {
            // 获取客户端玩家
            LocalPlayer player = ClientUtils.getPlayer();
            // 获取注册管理器
            RegistryAccess.Frozen registry = player.connection.registryAccess();
            return switch (this) {
                case ITEM -> ServerUtils.getName((Item) obj);
                case BLOCK -> ((Block) obj).getName();
                case ENTITY -> ((EntityType<?>) obj).getDescription();
                case ENCHANTMENT -> EnchantmentUtils.getName((Enchantment) obj);
                case STATUS_EFFECT -> ((MobEffect) obj).getDisplayName();
                case BIOME -> registry.lookup(Registries.BIOME)
                        .map(biome -> biome.getKey((Biome) obj))
                        .map(identifier -> identifier.toLanguageKey("biome"))
                        .map(LocalizationKey::literal)
                        .map(LocalizationKey::translate)
                        .orElse(TextBuilder.create(UNREGISTERED));
                case GAME_MODE -> ((GameType) obj).getLongDisplayName();
                case GAME_RULE -> LocalizationKey.literal(((GameRule<?>) obj).getDescriptionId()).translate();
            };
        }

        // 获取参数类型
        private ArgumentType<?> getArgumentType() {
            return switch (this) {
                case ITEM -> new ClientObjectArgumentType.ClientItemArgumentType(false);
                case BLOCK -> new ClientObjectArgumentType.ClientBlockArgumentType(false);
                case ENTITY -> new ClientObjectArgumentType.ClientEntityArgumentType();
                case ENCHANTMENT -> new ClientObjectArgumentType.ClientEnchantmentArgumentType();
                case STATUS_EFFECT -> new ClientObjectArgumentType.ClientStatusEffectArgumentType();
                case BIOME -> new ClientObjectArgumentType.ClientBiomeArgumentType();
                case GAME_MODE -> new ClientObjectArgumentType.ClientGameModeArgumentType();
                case GAME_RULE -> new ClientObjectArgumentType.ClientGameRuleArgumentType();
            };
        }
    }
}
