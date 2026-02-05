package org.carpetorgaddition.wheel;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.carpetorgaddition.CarpetOrgAdditionSettings;

import java.util.List;

public class BlockHardnessModifiers {
    /**
     * 深板岩和与深板岩硬度相同的变种
     */
    private static final List<Block> DEEPSLATE = List.of(
            // 深板岩
            Blocks.DEEPSLATE,
            // 雕纹深板岩
            Blocks.CHISELED_DEEPSLATE,
            // 磨制深板岩
            Blocks.POLISHED_DEEPSLATE,
            // 磨制深板岩台阶
            Blocks.POLISHED_DEEPSLATE_SLAB,
            // 磨制深板岩楼梯
            Blocks.POLISHED_DEEPSLATE_STAIRS,
            // 磨制深板岩墙
            Blocks.POLISHED_DEEPSLATE_WALL,
            // 深板岩砖台阶
            Blocks.DEEPSLATE_BRICK_SLAB,
            // 深板岩砖楼梯
            Blocks.DEEPSLATE_BRICK_STAIRS,
            // 深板岩砖墙
            Blocks.DEEPSLATE_BRICK_WALL
    );

    /**
     * 深板岩圆石和与深板岩圆石硬度相同的变种
     */
    private static final List<Block> COBBLED_DEEPSLATE = List.of(
            // 深板岩圆石
            Blocks.COBBLED_DEEPSLATE,
            // 深板岩圆石台阶
            Blocks.COBBLED_DEEPSLATE_SLAB,
            // 深板岩圆石楼梯
            Blocks.COBBLED_DEEPSLATE_STAIRS,
            // 深板岩圆石墙
            Blocks.COBBLED_DEEPSLATE_WALL,
            // 裂纹深板岩砖
            Blocks.DEEPSLATE_BRICKS,
            // 裂纹深板岩瓦
            Blocks.DEEPSLATE_TILES,
            // 深板岩瓦台阶
            Blocks.DEEPSLATE_TILE_SLAB,
            // 深板岩瓦楼梯
            Blocks.DEEPSLATE_TILE_STAIRS,
            // 深板岩瓦墙
            Blocks.DEEPSLATE_TILE_WALL,
            // 裂纹深板岩砖
            Blocks.CRACKED_DEEPSLATE_BRICKS,
            // 裂纹深板岩瓦
            Blocks.CRACKED_DEEPSLATE_TILES
    );

    /**
     * 普通矿石
     */
    private static final List<Block> ORE = List.of(
            // 煤矿石
            Blocks.COAL_ORE,
            // 铁矿石
            Blocks.IRON_ORE,
            // 铜矿石
            Blocks.COPPER_ORE,
            // 青金石矿石
            Blocks.LAPIS_ORE,
            // 金矿石
            Blocks.GOLD_ORE,
            // 红石矿石
            Blocks.REDSTONE_ORE,
            // 钻石矿石
            Blocks.DIAMOND_ORE,
            // 绿宝石矿石
            Blocks.EMERALD_ORE
    );

    /**
     * 深层矿石
     */
    private static final List<Block> DEEPSLATE_ORE = List.of(
            // 深层煤矿石
            Blocks.DEEPSLATE_COAL_ORE,
            // 深层铁矿石
            Blocks.DEEPSLATE_IRON_ORE,
            // 深层铜矿石
            Blocks.DEEPSLATE_COPPER_ORE,
            // 深层青金石矿石
            Blocks.DEEPSLATE_LAPIS_ORE,
            // 深层金矿石
            Blocks.DEEPSLATE_GOLD_ORE,
            // 深层红石矿石
            Blocks.DEEPSLATE_REDSTONE_ORE,
            // 深层钻石矿石
            Blocks.DEEPSLATE_DIAMOND_ORE,
            // 深层绿宝石矿石
            Blocks.DEEPSLATE_EMERALD_ORE
    );

    /**
     * 下界矿石
     */
    private static final List<Block> NETHER_ORE = List.of(
            // 下界石英矿石
            Blocks.NETHER_QUARTZ_ORE,
            // 下界金矿石
            Blocks.NETHER_GOLD_ORE
    );

    // 获取方块硬度
    public static float getHardness(Block block, BlockView world, BlockPos pos, float defaultValue) {
        // 设置基岩硬度
        if (block == Blocks.BEDROCK) {
            float hardness = CarpetOrgAdditionSettings.setBedrockHardness.get();
            if (CarpetOrgAdditionSettings.setBedrockHardness.get() != -1F) {
                return hardness;
            }
        }
        // 易碎深板岩
        if (CarpetOrgAdditionSettings.softDeepslate.get()) {
            // 深板岩
            if (DEEPSLATE.contains(block)) {
                return Blocks.STONE.getHardness();
            }
            // 深板岩圆石
            if (COBBLED_DEEPSLATE.contains(block)) {
                return Blocks.COBBLESTONE.getHardness();
            }
        }
        // 易碎黑曜石
        if (CarpetOrgAdditionSettings.softObsidian.get() && (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN)) {
            return Blocks.END_STONE.getHardness();
        }
        // 易碎矿石
        if (CarpetOrgAdditionSettings.softOres.get()) {
            // 普通矿石
            if (ORE.contains(block)) {
                return Blocks.STONE.getHardness();
            }
            // 深层矿石
            if (DEEPSLATE_ORE.contains(block)) {
                // 让深板岩矿石硬度随着深板岩硬度的改变而改变，其他方块硬度不需要做此处理
                return Blocks.DEEPSLATE.getDefaultState().getHardness(world, pos);
            }
            // 下界矿石
            if (NETHER_ORE.contains(block)) {
                return Blocks.NETHERRACK.getHardness();
            }
        }
        if (CarpetOrgAdditionSettings.softNetherite.get() && (block == Blocks.ANCIENT_DEBRIS || block == Blocks.NETHERITE_BLOCK)) {
            // 调整硬度后，远古残骸比能改瞬间挖掘的方块略硬，避免瞬间挖掘
            return defaultValue / 18F;
        }
        return defaultValue;
    }
}
