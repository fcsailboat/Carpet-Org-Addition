package org.carpetorgaddition.rule;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.patches.EntityPlayerMPFake;
import carpet.utils.TranslationKeys;
import carpet.utils.Translations;
import net.minecraft.entity.damage.DamageRecord;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.entity.mob.Monster;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.mixin.accessor.DamageTrackerAccessor;
import org.carpetorgaddition.rule.value.FakePlayerKeepInventory;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.jetbrains.annotations.Nullable;

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
     * 潜影盒是否可以触发更新抑制器
     */
    public static boolean canUpdateSuppression(@Nullable String blockName) {
        if ("false".equalsIgnoreCase(CarpetOrgAdditionSettings.CCEUpdateSuppression.get())) {
            return false;
        }
        if (blockName == null) {
            return false;
        }
        if ("true".equalsIgnoreCase(CarpetOrgAdditionSettings.CCEUpdateSuppression.get())) {
            return "更新抑制器".equals(blockName) || "updateSuppression".equalsIgnoreCase(blockName);
        }
        // 比较字符串并忽略大小写
        return Objects.equals(CarpetOrgAdditionSettings.CCEUpdateSuppression.get().toLowerCase(), blockName.toLowerCase());
    }

    public static boolean isDefaultDistance() {
        return CarpetOrgAdditionSettings.maxBlockPlaceDistance.get() == -1;
    }

    /**
     * 获取Carpet Org Addition设置的玩家最大交互距离并进行判断，小于0的值会被视为6.0，超过256的值会被视为256.0
     *
     * @return 当前设置的最大交互距离，最大不超过256.0
     */
    public static double getPlayerMaxInteractionDistance() {
        double distance = CarpetOrgAdditionSettings.maxBlockPlaceDistance.get();
        if (distance < 0) {
            return 6.0;
        }
        return Math.min(distance, MAX_DISTANCE);
    }

    public static <T> T shulkerBoxStackableWrap(Supplier<T> supplier) {
        boolean changed = CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.get();
        try {
            CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.set(false);
            return supplier.get();
        } finally {
            CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.set(changed);
        }
    }

    private static Supplier<Boolean> getCarpetRule(String rule) {
        if (CarpetOrgAddition.CARPET_TIS_ADDITION) {
            CarpetRule<?> carpetRule = CarpetServer.settingsManager.getCarpetRule(rule);
            if (carpetRule == null) {
                return () -> false;
            }
            return () -> carpetRule.value() instanceof Boolean value ? value : false;
        }
        return () -> false;
    }

    /**
     * @return 规则方块掉落物直接进入物品栏是否可用
     */
    public static boolean canCollectBlock(@Nullable ServerPlayerEntity player) {
        if (player == null) {
            return false;
        }
        return switch (CarpetOrgAdditionSettings.blockDropsDirectlyEnterInventory.get()) {
            case TRUE -> true;
            case FALSE -> false;
            case CUSTOM -> {
                RuleSelfManager ruleSelfManager = FetcherUtils.getRuleSelfManager(player);
                yield ruleSelfManager.isEnabled(player, RuleSelfConstants.blockDropsDirectlyEnterInventory);
            }
        };
    }

    /**
     * 获取规则的名称
     */
    public static Text simpleTranslationName(CarpetRule<?> rule) {
        String key = String.format(TranslationKeys.RULE_NAME_PATTERN, rule.settingsManager().identifier(), rule.name());
        TextBuilder builder = TextBuilder.of(key);
        if (Translations.hasTranslation(key)) {
            return builder.setHover(rule.name()).build();
        }
        return TextBuilder.create(rule.name());
    }

    public static List<Text> ruleExtraInfo(CarpetRule<?> rule) {
        String key = String.format(TranslationKeys.RULE_EXTRA_PREFIX_PATTERN, rule.settingsManager().identifier(), rule.name());
        List<String> list = new ArrayList<>();
        for (int i = 0; Translations.hasTranslation(key + i); i++) {
            list.add(Translations.tr(key + i));
        }
        return list.stream()
                .map(TextBuilder::of)
                .map(builder -> builder.setColor(Formatting.GRAY))
                .map(TextBuilder::build)
                .toList();
    }

    /**
     * 如果{@code 假玩家死亡不掉落条件}启用，则是否应该保留物品栏
     */
    public static boolean shouldKeepInventory(EntityPlayerMPFake fakePlayer) {
        return switch (CarpetOrgAdditionSettings.fakePlayerKeepInventoryCondition.get()) {
            // 无条件保留/掉落物品栏
            case UNCONDITIONAL -> getFakePlayerKeepInventory();
            // 被玩家杀死或落入虚空时保留物品栏
            case KILLED_BY_PLAYER_OR_THE_VOID -> {
                DamageTracker damageTracker = fakePlayer.getDamageTracker();
                List<DamageRecord> records = ((DamageTrackerAccessor) damageTracker).getRecentDamage();
                DamageSource directCause = records.getLast().damageSource();
                // 伤害的源发实体为玩家时不掉落（例如玩家射出的箭）
                if (directCause.getSource() instanceof ServerPlayerEntity) {
                    yield true;
                }
                // 伤害的直接实体为玩家时不掉落
                if (directCause.getAttacker() instanceof ServerPlayerEntity) {
                    yield true;
                }
                // 假玩家最近一段时间曾受到来自玩家的伤害
                if (fakePlayer.getPrimeAdversary() instanceof ServerPlayerEntity) {
                    yield true;
                }
                // 假玩家落入虚空时不掉落
                yield directCause.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY);
            }
            case KILLED_BY_PLAYER_OR_THE_VOID_OR_FALL_OR_BY_MOBS -> {
                    DamageTracker damageTracker = fakePlayer.getDamageTracker();
                    List<DamageRecord> records = ((DamageTrackerAccessor) damageTracker).getRecentDamage();
                    DamageSource directCause = records.getLast().damageSource();
                    // 伤害的源发实体为玩家时不掉落（例如玩家射出的箭）
                    if (directCause.getSource() instanceof ServerPlayerEntity) {
                        yield true;
                    }
                    // 伤害的直接实体为玩家时不掉落
                    if (directCause.getAttacker() instanceof ServerPlayerEntity) {
                        yield true;
                    }
                    // 假玩家最近一段时间曾受到来自玩家的伤害
                    if (fakePlayer.getPrimeAdversary() instanceof ServerPlayerEntity) {
                        yield true;
                    }
                    //摔死时不掉落
                    if (directCause == fakePlayer.getDamageSources().fall()) {
                        yield true;
                    }
                    // 被怪物杀死时不掉落
                    if (directCause.getAttacker() instanceof Monster) {
                        yield true;
                    }
                    // 假玩家落入虚空时不掉落
                    yield directCause.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY);
            }
        };
    }
    public static boolean getFakePlayerKeepInventory() {
        if (CarpetOrgAdditionSettings.fakePlayerKeepInventory.get() == FakePlayerKeepInventory.TRUE) {
            return true;
        } else if (CarpetOrgAdditionSettings.fakePlayerKeepInventory.get() == FakePlayerKeepInventory.FALSE) {
            return false;
        }
        return false;
    }}
