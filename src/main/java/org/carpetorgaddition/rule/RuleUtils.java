package org.carpetorgaddition.rule;

import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.validator.MaxBlockPlaceDistanceValidator;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class RuleUtils {

    /**
     * 潜影盒是否可以触发更新抑制器
     */
    public static boolean canUpdateSuppression(@Nullable String blockName) {
        if ("false".equalsIgnoreCase(CarpetOrgAdditionSettings.CCEUpdateSuppression)) {
            return false;
        }
        if (blockName == null) {
            return false;
        }
        if ("true".equalsIgnoreCase(CarpetOrgAdditionSettings.CCEUpdateSuppression)) {
            return "更新抑制器".equals(blockName) || "updateSuppression".equalsIgnoreCase(blockName);
        }
        // 比较字符串并忽略大小写
        return Objects.equals(CarpetOrgAdditionSettings.CCEUpdateSuppression.toLowerCase(), blockName.toLowerCase());
    }

    public static boolean isDefaultDistance() {
        return CarpetOrgAdditionSettings.maxBlockPlaceDistance == -1;
    }

    /**
     * 获取Carpet Org Addition设置的玩家最大交互距离并进行判断，小于0的值会被视为6.0，超过256的值会被视为256.0
     *
     * @return 当前设置的最大交互距离，最大不超过128
     */
    public static double getPlayerMaxInteractionDistance() {
        double distance = CarpetOrgAdditionSettings.maxBlockPlaceDistance;
        if (distance < 0) {
            return 6.0;
        }
        return Math.min(distance, MaxBlockPlaceDistanceValidator.MAX_VALUE);
    }
}
