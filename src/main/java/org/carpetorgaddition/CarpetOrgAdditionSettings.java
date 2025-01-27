package org.carpetorgaddition;

import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.rule.Hidden;
import org.carpetorgaddition.rule.Removed;
import org.carpetorgaddition.rule.validator.*;
import org.carpetorgaddition.rule.value.*;

@SuppressWarnings("CanBeFinal")
public class CarpetOrgAdditionSettings {
    /**
     * 控制玩家登录登出的消息是否显示
     */
    public static boolean hiddenLoginMessages = false;
    /**
     * 潜影盒是否允许被堆叠，这还需要同时启用{@link CarpetOrgAdditionSettings#shulkerBoxStackable}
     */
    public static final ThreadLocal<Boolean> shulkerBoxStackCountChanged = ThreadLocal.withInitial(() -> true);
    /**
     * 当前方块的破坏者，启用{@link CarpetOrgAdditionSettings#blockDropsDirectlyEnterInventory}后，方块掉落物会直接进入玩家物品栏
     */
    public static final ThreadLocal<ServerPlayerEntity> blockBreaking = new ThreadLocal<>();
    /**
     * 当前正在使用铁砧附魔的玩家
     */
    public static final ThreadLocal<PlayerEntity> enchanter = new ThreadLocal<>();

    private CarpetOrgAdditionSettings() {
    }

    // TODO 不支持翻译
    public static final String ORG = "Org";
    public static final String Hidden = "Hidden";

    // 制作物品分身
    @Rule(
            categories = {ORG, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandItemShadowing = "ops";

    // 设置基岩硬度
    @Rule(
            categories = {ORG},
            validators = {BedrockHardnessValidator.class}
    )
    public static float setBedrockHardness = -1F;

    // 绑定诅咒无效化
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean bindingCurseInvalidation = false;

    // 禁用钓鱼开阔水域检测
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean disableOpenOrWaterDetection = false;

    // 幽匿尖啸体放置时状态
    @Removed
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean sculkShriekerCanSummon = false;

    // 创造玩家免疫/kill
    @Rule(categories = {ORG, RuleCategory.CREATIVE})
    public static boolean creativeImmuneKill = false;

    // 掉落物不消失
    @Removed
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean itemNeverDespawn = false;

    // 滑翔时不能对方块使用烟花
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean flyingUseOnBlockFirework = false;

    // 盯着末影人眼睛看时不会激怒末影人
    @Rule(categories = {ORG, RuleCategory.SURVIVAL, RuleCategory.FEATURE})
    public static boolean staringEndermanNotAngry = false;

    // 耕地防踩踏
    @Rule(categories = {ORG, RuleCategory.SURVIVAL, RuleCategory.FEATURE})
    public static boolean farmlandPreventStepping = false;

    // 最大方块交互距离
    @Rule(
            categories = {ORG, RuleCategory.SURVIVAL, RuleCategory.FEATURE},
            validators = {MaxBlockPlaceDistanceValidator.class}
    )
    public static double maxBlockPlaceDistance = -1;

    // 简易更新跳略器
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean simpleUpdateSkipper = false;

    // 强化引雷
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean channelingIgnoreWeather = false;

    // 无伤末影珍珠
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean notDamageEnderPearl = false;

    // 禁用伤害免疫
    @Removed
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean disableDamageImmunity = false;

    // 干草捆完全抵消摔落伤害
    @Removed
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean hayBlockCompleteOffsetFall = false;

    // 蓝冰上不能刷怪
    @Removed
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean blueIceCanSpawn = false;

    // 禁止蝙蝠生成
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean disableBatCanSpawn = false;

    // 海龟蛋快速孵化
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean turtleEggFastHatch = false;

    // 强制开启潜影盒
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean openShulkerBoxForcibly = false;

    // 村民无限交易
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean villagerInfiniteTrade = false;

    // 烟花火箭使用冷却
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean fireworkRocketUseCooldown = false;

    // 强化激流
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean riptideIgnoreWeather = false;

    // 禁止猪灵僵尸化
    @Removed
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean disablePiglinZombify = false;

    // 禁止村民女巫化
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    @Removed
    public static boolean disableVillagerWitch = false;

    // 禁止铁傀儡攻击玩家
    @Removed
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean disableIronGolemAttackPlayer = false;

    // 将镐作为基岩的有效采集工具
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean pickaxeMinedBedrock = false;

    // 村民回血
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean villagerHeal = false;

    // 假玩家回血
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean fakePlayerHeal = false;

    // 假玩家工具命令
    @Rule(
            categories = {ORG, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandPlayerTools = "false";

    // 最大方块交互距离适用于实体
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean maxBlockPlaceDistanceReferToEntity = false;

    // 击退棒
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean knockbackStick = false;

    // 禁止重生方块爆炸
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean disableRespawnBlocksExplode = false;

    // CCE更新抑制器
    @Rule(
            categories = {ORG, RuleCategory.FEATURE},
            options = {"true", "false"},
            strict = false
    )
    public static String CCEUpdateSuppression = "false";

    // 开放/seed命令权限
    @Rule(categories = {ORG, RuleCategory.COMMAND})
    public static boolean openSeedPermissions = false;

    // 发送消息命令
    @Rule(
            categories = {ORG, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandSendMessage = "ops";

    // 开放/carpet命令权限
    @Rule(categories = {ORG, RuleCategory.COMMAND, RuleCategory.CLIENT})
    public static boolean openCarpetPermissions = false;

    // 开放/gamerule命令权限
    @Rule(categories = {ORG, RuleCategory.COMMAND})
    public static boolean openGameRulePermissions = false;

    // 打开村民物品栏
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean openVillagerInventory = false;

    // 和平的苦力怕
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean peacefulCreeper = false;

    // 经验转移
    @Rule(
            categories = {ORG, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandXpTransfer = "ops";

    // 生存旁观切换命令
    @Rule(
            categories = {ORG, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandSpectator = "ops";

    // 查找器命令
    @Rule(
            categories = {ORG, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandFinder = "true";

    // 自杀
    @Rule(
            categories = {ORG, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandKillMe = "ops";

    // 路径点管理器
    @Rule(
            categories = {ORG, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandLocations = "ops";

    // 生命值不满可以进食
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean healthNotFullCanEat = false;

    // 可采集刷怪笼
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean canMineSpawner = false;

    // 假玩家生成时无击退
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean fakePlayerSpawnNoKnockback = false;

    // 可激活侦测器
    @Rule(categories = {ORG, RuleCategory.FEATURE, RuleCategory.SURVIVAL})
    public static boolean canActivatesObserver = false;

    // 禁止水结冰
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean disableWaterFreezes = false;

    // 假玩家合成保留物品
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean fakePlayerCraftKeepItem = false;

    // 绘制粒子线命令
    @Removed
    @Rule(
            categories = {ORG, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandParticleLine = "false";

    // 禁止特定生物在和平模式下被清除
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean disableMobPeacefulDespawn = false;

    // 船可以直接走向一格高的方块
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static boolean climbingBoat = false;

    // 可重复使用的锻造模板
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static ReusableSmithingTemplate reusableSmithingTemplate = ReusableSmithingTemplate.FALSE;

    // 开放/tp命令权限
    @Rule(categories = {ORG, RuleCategory.COMMAND})
    public static boolean openTpPermissions = false;

    // 易碎深板岩
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean softDeepslate = false;

    // 易碎黑曜石
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean softObsidian = false;

    // 易碎矿石
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean softOres = false;

    // 更好的不死图腾
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static BetterTotemOfUndying betterTotemOfUndying = BetterTotemOfUndying.FALSE;

    // 假玩家动作命令
    @Rule(
            categories = {ORG, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandPlayerAction = "ops";

    // 假玩家合成支持潜影盒
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean fakePlayerCraftPickItemFromShulkerBox = false;

    // 自定义猪灵交易时间
    @Rule(
            categories = {ORG, RuleCategory.SURVIVAL},
            validators = PiglinBarteringTimeValidator.class
    )
    public static long customPiglinBarteringTime = -1;

    // 快速设置假玩家合成
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static QuickSettingFakePlayerCraft quickSettingFakePlayerCraft = QuickSettingFakePlayerCraft.FALSE;

    // 湿海绵立即干燥
    @Removed
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static WetSpongeImmediatelyDry wetSpongeImmediatelyDry = WetSpongeImmediatelyDry.FALSE;

    // 假玩家死亡不掉落
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean fakePlayerKeepInventory = false;

    // 苦力怕命令
    @Rule(
            categories = {ORG, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandCreeper = "false";

    // 规则搜索命令
    @Rule(
            categories = {ORG, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandRuleSearch = "ops";

    // 增强闪电苦力怕
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean superChargedCreeper = false;

    // 玩家掉落头颅
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean playerDropHead = false;

    // 信标范围扩展
    @Rule(
            categories = {ORG, RuleCategory.SURVIVAL},
            validators = BeaconRangeExpandValidator.class
    )
    public static int beaconRangeExpand = 0;

    // 信标世界高度
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean beaconWorldHeight = false;

    // 生物是否可以捡起物品
    @Removed
    @Rule(categories = {ORG, RuleCategory.FEATURE})
    public static MobWhetherOrNotCanPickItem mobWhetherOrNotCanPickItem = MobWhetherOrNotCanPickItem.VANILLA;

    // 可高亮方块坐标
    @Rule(categories = {ORG, RuleCategory.SURVIVAL, RuleCategory.CLIENT})
    public static CanHighlightBlockPos canHighlightBlockPos = CanHighlightBlockPos.DEFAULT;

    // 玩家管理器命令
    @Rule(
            categories = {ORG, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandPlayerManager = "ops";

    // 方块掉落物直接进入物品栏
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean blockDropsDirectlyEnterInventory = false;

    // 海龟蛋快速采集
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean turtleEggFastMine = false;

    // 导航器
    @Rule(
            categories = {ORG, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandNavigate = "true";

    // 玩家死亡产生的掉落物不会自然消失
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean playerDropsNotDespawning = false;

    // 假玩家最大合成次数
    @Rule(
            categories = {ORG, RuleCategory.SURVIVAL},
            options = {"1", "3", "5", "-1"},
            strict = false,
            validators = FakePlayerMaxCraftCountValidator.class
    )
    public static int fakePlayerMaxCraftCount = 3;

    // 假玩家生成时内存泄漏修复
    @Rule(categories = {ORG, RuleCategory.BUGFIX})
    public static boolean fakePlayerSpawnMemoryLeakFix = false;

    // 快递命令
    @Rule(
            categories = {ORG, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"}
    )
    public static String commandMail = "ops";

    // 抑制方块破坏位置不匹配警告
    @Rule(categories = {ORG, RuleCategory.EXPERIMENTAL})
    public static boolean suppressionMismatchInDestroyBlockPosWarn = false;

    // 同步导航器航点
    @Removed
    @Rule(
            categories = {ORG, RuleCategory.CLIENT},
            validators = SyncNavigateWaypointObserver.class
    )
    public static boolean syncNavigateWaypoint = true;

    // 潜影盒堆叠
    @Rule(categories = {ORG, RuleCategory.EXPERIMENTAL})
    public static boolean shulkerBoxStackable = false;

    // 最大服务器交互距离同步客户端
    @Rule(categories = {ORG, RuleCategory.CLIENT})
    public static boolean maxBlockPlaceDistanceSyncClient = true;

    // 限制幻翼生成
    @Rule(categories = {ORG, RuleCategory.SURVIVAL})
    public static boolean limitPhantomSpawn = false;

    // 立即应用工具效果
    @Rule(categories = {ORG, RuleCategory.BUGFIX})
    public static boolean applyToolEffectsImmediately = false;

    // 强制补货
    @Hidden
    @Rule(categories = {ORG, Hidden})
    public static boolean forceRestock = false;

    // 自动同步玩家状态
    @Hidden
    @Rule(categories = {ORG, Hidden})
    public static boolean autoSyncPlayerStatus = false;

    // 查找可能影响世吞运行的方块
    @Hidden
    @Rule(categories = {ORG, RuleCategory.COMMAND, Hidden})
    public static boolean finderCommandSearchWorldEater = false;

    // 记录玩家命令
    @Rule(categories = {ORG, RuleCategory.COMMAND})
    public static boolean recordPlayerCommand = false;
}
