package boat.carpetorgaddition.debug;

import boat.carpetorgaddition.exception.ProductionEnvironmentError;
import boat.carpetorgaddition.rule.RuleContext;
import boat.carpetorgaddition.rule.RuleFactory;
import carpet.api.settings.CarpetRule;

import java.util.function.Supplier;

public class DebugSettings {
    static {
        // 不要在非开发环境加载
        ProductionEnvironmentError.assertDevelopmentEnvironment();
    }

    public static final Supplier<Boolean> openFakePlayerInventory = register(
            RuleFactory.of("openFakePlayerInventory", false)
                    .addCategories("Debug")
                    .setDisplayName("打开玩家物品栏")
                    .build()
    );
    public static final Supplier<Boolean> showSoulSandItemCount = register(
            RuleFactory.of("showSoulSandItemCount", false)
                    .addCategories("Debug")
                    .setDisplayName("显示灵魂沙上物品的数量")
                    .build()
    );
    public static final Supplier<Boolean> showComparatorLevel = register(
            RuleFactory.of("showComparatorLevel", false)
                    .addCategories("Debug")
                    .setDisplayName("显示比较器输出等级")
                    .build()
    );
    public static final Supplier<Boolean> phantomImmediatelySpawn = register(
            RuleFactory.of("phantomImmediatelySpawn", false)
                    .addCategories("Debug")
                    .setDisplayName("幻翼立即生成")
                    .build()
    );
    public static final Supplier<Boolean> showBlockBreakingSpeed = register(
            RuleFactory.of("showBlockBreakingSpeed", false)
                    .addCategories("Debug")
                    .setDisplayName("显示方块挖掘速度")
                    .build()
    );
    public static final Supplier<Boolean> HUDInformationDisplay = register(
            RuleFactory.of("HUDInformationDisplay", false)
                    .addCategories("Debug")
                    .setDisplayName("HUD信息显示")
                    .build()
    );
    public static final Supplier<Boolean> disableExperienceOrbSurround = register(
            RuleFactory.of("disableExperienceOrbSurround", false)
                    .addCategories("Debug")
                    .setDisplayName("禁用经验球环绕")
                    .build()
    );
    public static final Supplier<Integer> fixedFishingHookTime = register(
            RuleFactory.of("fixedFishingHookTime", -1)
                    .addCategories("Debug")
                    .setDisplayName("固定钓鱼上钩时间")
                    .build()
    );
    @SuppressWarnings("all")
    public static final Supplier<Boolean> tempDebugSwitch = register(
            RuleFactory.of("temporaryDebuggingSwitch", false)
                    .addCategories("Debug")
                    .build()
    );
    public static final Supplier<Boolean> prohibitTaskTimeout = register(
            RuleFactory.of("prohibitTaskTimeout", false)
                    .addCategories("Debug")
                    .setDisplayName("禁止任务超时")
                    .build()
    );
    public static final Supplier<Boolean> showPlayerExperience = register(
            RuleFactory.of("showPlayerExperience", false)
                    .addCategories("Debug")
                    .setDisplayName("显示玩家经验")
                    .build()
    );
    public static final Supplier<Boolean> displayItemTagTooltip = register(
            RuleFactory.of("displayItemTagTooltip", false)
                    .addCategories("Debug")
                    .setDisplayName("显示物品标签工具提示")
                    .build()
    );
    public static final Supplier<Boolean> mergeShulkerTooltip = register(
            RuleFactory.of("mergeShulkerTooltip", false)
                    .addCategories("Debug")
                    .setDisplayName("合并潜影盒工具提示信息")
                    .build()
    );

    private static <T> Supplier<T> register(RuleContext<T> context) {
        CarpetRule<T> rule = context.rule();
        DebugRuleRegistrar.getInstance().extensionSettingsManager().addCarpetRule(rule);
        return rule::value;
    }
}
