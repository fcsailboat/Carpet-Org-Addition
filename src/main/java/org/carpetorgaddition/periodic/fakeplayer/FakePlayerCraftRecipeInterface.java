package org.carpetorgaddition.periodic.fakeplayer;

import net.minecraft.inventory.RecipeInputInventory;

public interface FakePlayerCraftRecipeInterface {

    /**
     * @return 获取工作台输入物品栏
     */
    RecipeInputInventory getInput();
}
