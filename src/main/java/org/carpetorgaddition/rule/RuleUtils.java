package org.carpetorgaddition.rule;

import org.carpetorgaddition.CarpetOrgAdditionSettings;
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
}
