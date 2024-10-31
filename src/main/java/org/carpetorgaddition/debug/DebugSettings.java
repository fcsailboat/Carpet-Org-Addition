package org.carpetorgaddition.debug;

public class DebugSettings {
    @DebugRule(name = "打开玩家物品栏")
    public static boolean openFakePlayerInventory = false;

    @DebugRule(name = "显示灵魂沙上物品的数量")
    public static boolean showSoulSandItemCount = false;

    @DebugRule(name = "显示比较器输出等级")
    public static boolean showComparatorLevel = false;
}
