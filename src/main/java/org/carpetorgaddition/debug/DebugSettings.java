package org.carpetorgaddition.debug;

import org.carpetorgaddition.exception.ProductionEnvironmentError;

public class DebugSettings {
    static {
        // 不要在非开发环境加载
        ProductionEnvironmentError.assertDevelopmentEnvironment();
    }

    @DebugRule(name = "打开玩家物品栏")
    public static boolean openFakePlayerInventory = false;

    @DebugRule(name = "显示灵魂沙上物品的数量")
    public static boolean showSoulSandItemCount = false;

    @DebugRule(name = "显示比较器输出等级")
    public static boolean showComparatorLevel = false;

    @DebugRule(name = "幻翼立即生成")
    public static boolean phantomImmediatelySpawn = false;

    @DebugRule(name = "显示方块挖掘速度")
    public static boolean showBlockBreakingSpeed = false;
}
