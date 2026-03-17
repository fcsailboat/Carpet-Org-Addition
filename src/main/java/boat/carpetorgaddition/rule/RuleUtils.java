package boat.carpetorgaddition.rule;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.mixin.accessor.DamageTrackerAccessor;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.patches.EntityPlayerMPFake;
import carpet.utils.TranslationKeys;
import carpet.utils.Translations;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionProviderCheck;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.PermissionSetSupplier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class RuleUtils {
    /**
     * 最大方块交互距离的最大值
     */
    public static final double MAX_DISTANCE = 256.0;
    public static final int MAX_BEACON_RANGE = 1024;
    /**
     * 最小合成次数
     */
    public static final int MIN_CRAFT_COUNT = 1;
    public static final Supplier<Boolean> hopperCountersUnlimitedSpeed = getCarpetRule("hopperCountersUnlimitedSpeed");
    public static final Supplier<Boolean> hopperNoItemCost = getCarpetRule("hopperNoItemCost");
    /**
     * 是否正在打开箱子
     */
    public static final ScopedValue<Boolean> OPENING_THE_CHEST = ScopedValue.newInstance();
    /**
     * 是否允许非空潜影盒堆叠
     */
    private static final boolean NON_EMPTY_SHULKER_BOX_STACKABLE = false;

    static {
        if (!CarpetOrgAddition.isDebugMode() && NON_EMPTY_SHULKER_BOX_STACKABLE) {
            throw new AssertionError();
        }
    }

    /**
     * 潜影盒是否可以触发更新抑制器
     */
    public static boolean canUpdateSuppression(@Nullable String blockName) {
        if ("false".equalsIgnoreCase(CarpetOrgAdditionSettings.CCE_UPDATE_SUPPRESSION.value())) {
            return false;
        }
        if (blockName == null) {
            return false;
        }
        if ("true".equalsIgnoreCase(CarpetOrgAdditionSettings.CCE_UPDATE_SUPPRESSION.value())) {
            return "更新抑制器".equals(blockName) || "updateSuppression".equalsIgnoreCase(blockName);
        }
        // 比较字符串并忽略大小写
        return Objects.equals(CarpetOrgAdditionSettings.CCE_UPDATE_SUPPRESSION.value().toLowerCase(), blockName.toLowerCase());
    }

    public static boolean isDefaultDistance() {
        return CarpetOrgAdditionSettings.MAX_BLOCK_PLACE_DISTANCE.value() == -1;
    }

    /**
     * 获取Carpet Org Addition设置的玩家最大交互距离并进行判断，小于0的值会被视为6.0，超过256的值会被视为256.0
     *
     * @return 当前设置的最大交互距离，最大不超过256.0
     */
    public static double getPlayerMaxInteractionDistance() {
        double distance = CarpetOrgAdditionSettings.MAX_BLOCK_PLACE_DISTANCE.value();
        if (distance < 0) {
            return 6.0;
        }
        return Math.min(distance, MAX_DISTANCE);
    }

    public static <T> T shulkerBoxStackableWrap(Supplier<T> supplier) {
        return ScopedValue.where(CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED, false).call(supplier::get);
    }

    private static Supplier<Boolean> getCarpetRule(String rule) {
        if (CarpetOrgAdditionConstants.CARPET_TIS_ADDITION) {
            CarpetRule<?> carpetRule = CarpetServer.settingsManager.getCarpetRule(rule);
            if (carpetRule == null) {
                return () -> false;
            }
            return () -> carpetRule.value() instanceof Boolean value ? value : false;
        }
        return () -> false;
    }

    /**
     * @param supplier  命令权限对应规则的开关
     * @param predicate 原版的权限谓词
     */
    public static <T extends PermissionSetSupplier> PermissionProviderCheck<T> requireOrOpenPermissionLevel(
            RuleAccessor<Boolean> supplier,
            PermissionProviderCheck<T> predicate
    ) {
        return new PermissionProviderCheck<>(new PermissionCheck() {
            @Override
            public boolean check(@NonNull PermissionSet permissions) {
                if (supplier.value()) {
                    return true;
                }
                return predicate.test().check(permissions);
            }

            @Override
            public @NonNull MapCodec<? extends PermissionCheck> codec() {
                return predicate.test().codec();
            }
        });
    }

    /**
     * 获取规则的名称
     */
    public static Component simpleTranslationName(CarpetRule<?> rule) {
        String key = String.format(TranslationKeys.RULE_NAME_PATTERN, rule.settingsManager().identifier(), rule.name());
        TextBuilder builder = LocalizationKey.literal(key).builder();
        if (Translations.hasTranslation(key)) {
            return builder.setHover(rule.name()).build();
        }
        return TextBuilder.create(rule.name());
    }

    public static List<Component> ruleExtraInfo(CarpetRule<?> rule) {
        String key = String.format(TranslationKeys.RULE_EXTRA_PREFIX_PATTERN, rule.settingsManager().identifier(), rule.name());
        List<String> list = new ArrayList<>();
        for (int i = 0; Translations.hasTranslation(key + i); i++) {
            list.add(Translations.tr(key + i));
        }
        return list.stream()
                .map(LocalizationKey::literal)
                .map(LocalizationKey::builder)
                .map(builder -> builder.setColor(ChatFormatting.GRAY))
                .map(TextBuilder::build)
                .toList();
    }

    /**
     * 如果{@code 假玩家死亡不掉落条件}启用，则是否应该保留物品栏
     */
    public static boolean shouldKeepInventory(EntityPlayerMPFake fakePlayer) {
        return switch (CarpetOrgAdditionSettings.FAKE_PLAYER_KEEP_INVENTORY_CONDITION.value()) {
            // 无条件保留物品栏
            case UNCONDITIONAL -> true;
            // 被玩家杀死或落入虚空时保留物品栏
            case KILLED_BY_PLAYER_OR_THE_VOID -> {
                CombatTracker damageTracker = fakePlayer.getCombatTracker();
                List<CombatEntry> records = ((DamageTrackerAccessor) damageTracker).getRecentDamage();
                DamageSource directCause = records.getLast().source();
                // 伤害的源发实体为玩家时不掉落（例如玩家射出的箭）
                if (directCause.getDirectEntity() instanceof ServerPlayer) {
                    yield true;
                }
                // 伤害的直接实体为玩家时不掉落
                if (directCause.getEntity() instanceof ServerPlayer) {
                    yield true;
                }
                // 假玩家最近一段时间曾受到来自玩家的伤害
                if (fakePlayer.getKillCredit() instanceof ServerPlayer) {
                    yield true;
                }
                // 假玩家落入虚空时不掉落
                yield directCause.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
            }
        };
    }

    /**
     * 潜影盒堆叠是否已启用，并且物品是空潜影盒
     */
    public static boolean shulkerBoxStackableEnabled(ItemInstance instance) {
        return instance instanceof ItemStack itemStack
               && CarpetOrgAdditionSettings.SHULKER_BOX_STACKABLE.value()
               && InventoryUtils.isShulkerBoxItem(itemStack)
               && (NON_EMPTY_SHULKER_BOX_STACKABLE || InventoryUtils.isEmptyShulkerBox(itemStack));
    }
}
