package org.carpetorgaddition.rule.value;

public enum ReusableSmithingTemplate {
    /**
     * 所有锻造模板都不会消耗
     */
    TRUE,
    /**
     * 原版行为
     */
    FALSE,
    /**
     * 仅下界合金升级模板不会被消耗
     */
    UPGRADE
}
