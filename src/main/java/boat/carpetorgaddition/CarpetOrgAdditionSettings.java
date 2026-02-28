package boat.carpetorgaddition;

import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.navigator.AbstractNavigator;
import boat.carpetorgaddition.periodic.navigator.NavigatorManager;
import boat.carpetorgaddition.rule.*;
import boat.carpetorgaddition.rule.helper.CompatibilityDialogProvider;
import boat.carpetorgaddition.rule.value.*;
import boat.carpetorgaddition.util.PlayerUtils;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleCategory;
import carpet.api.settings.SettingsManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CarpetOrgAdditionSettings {
    /**
     * 潜影盒是否允许被堆叠，这还需要同时启用{@link CarpetOrgAdditionSettings#shulkerBoxStackable}
     */
    public static final ThreadLocal<Boolean> shulkerBoxStackCountChanged = ThreadLocal.withInitial(() -> true);
    /**
     * 玩家是否正在执行{@code /killMe}命令
     */
    public static final ThreadLocal<Boolean> committingSuicide = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<ServerPlayer> playerSummoner = new ThreadLocal<>();
    public static final ThreadLocal<ServerPlayer> internalPlayerSummoner = new ThreadLocal<>();
    /**
     * 当前正在使用铁砧附魔的玩家
     */
    public static final ThreadLocal<Player> enchanter = new ThreadLocal<>();
    /**
     * 当前方块的破坏者，启用{@link CarpetOrgAdditionSettings#blockDropsDirectlyEnterInventory}后，方块掉落物会直接进入玩家物品栏
     */
    public static final ScopedValue<ServerPlayer> BLOCK_BREAKER = ScopedValue.newInstance();
    /**
     * 是否正在使用引雷三叉戟
     */
    public static final ScopedValue<Boolean> USE_CHANNELING_TRIDENT = ScopedValue.newInstance();
    /**
     * 是否确认启用规则，用于潜影盒可堆叠的兼容性警告对话框
     */
    public static final ScopedValue<Boolean> CONFIRM_ENABLE = ScopedValue.newInstance();
    private static final Set<RuleContext<?>> RULES = new LinkedHashSet<>();
    public static final String OPS = "ops";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    private static final List<String> COMMAND_OPTIONS = List.of(TRUE, FALSE, OPS, "0", "1", "2", "3", "4");
    public static final String ORG = "Org";
    public static final String HIDDEN = "Hidden";

    private CarpetOrgAdditionSettings() {
    }

    /**
     * 制作物品分身
     */
    public static final RuleAccessor<String> commandItemShadowing = register(
            RuleFactory.create(String.class, "commandItemShadowing", OPS)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 设置基岩硬度
     */
    public static final RuleAccessor<Float> setBedrockHardness = register(
            RuleFactory.create(Float.class, "setBedrockHardness", -1F)
                    .setRemoved()
                    .addValidator(
                            value -> value >= 0F || value == -1F,
                            () -> ValidatorFeedbacks.greaterOrEqualOrValue(0, -1)
                    )
                    .build()
    );

    /**
     * 绑定诅咒无效化
     */
    public static final RuleAccessor<Boolean> bindingCurseInvalidation = register(
            RuleFactory.create(Boolean.class, "bindingCurseInvalidation", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 禁用钓鱼开阔水域检测
     */
    public static final RuleAccessor<Boolean> disableOpenOrWaterDetection = register(
            RuleFactory.create(Boolean.class, "disableOpenOrWaterDetection", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 创造玩家免疫/kill
     */
    public static final RuleAccessor<Boolean> creativeImmuneKill = register(
            RuleFactory.create(Boolean.class, "creativeImmuneKill", false)
                    .addCategories(RuleCategory.CREATIVE)
                    .build()
    );

    /**
     * 盯着末影人眼睛看时不会激怒末影人
     */
    public static final RuleAccessor<Boolean> staringEndermanNotAngry = register(
            RuleFactory.create(Boolean.class, "staringEndermanNotAngry", false)
                    .addCategories(RuleCategory.SURVIVAL, RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 耕地防踩踏
     */
    public static final RuleAccessor<Boolean> farmlandPreventStepping = register(
            RuleFactory.create(Boolean.class, "farmlandPreventStepping", false)
                    .addCategories(RuleCategory.SURVIVAL, RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 最大方块交互距离
     */
    public static final RuleAccessor<Double> maxBlockPlaceDistance = register(
            RuleFactory.create(Double.class, "maxBlockPlaceDistance", -1.0)
                    .addCategories(RuleCategory.SURVIVAL, RuleCategory.FEATURE)
                    .addValidator(
                            newValue -> (newValue >= 0.0 && newValue <= RuleUtils.MAX_DISTANCE) || newValue == -1.0,
                            () -> ValidatorFeedbacks.rangeOrValue(0, (int) RuleUtils.MAX_DISTANCE, -1)
                    )
                    .build()
    );

    /**
     * 简易更新跳略器
     */
    public static final RuleAccessor<Boolean> simpleUpdateSkipper = register(
            RuleFactory.create(Boolean.class, "simpleUpdateSkipper", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 引雷忽略条件
     */
    public static final RuleAccessor<IgnoreChannelingConditions> channelingIgnoreConditions = register(
            RuleFactory.create(IgnoreChannelingConditions.class, "channelingIgnoreConditions", IgnoreChannelingConditions.FALSE)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 无伤末影珍珠
     */
    public static final RuleAccessor<Boolean> notDamageEnderPearl = register(
            RuleFactory.create(Boolean.class, "notDamageEnderPearl", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 禁用伤害免疫
     */
    public static final RuleAccessor<Boolean> disableDamageImmunity = register(
            RuleFactory.create(Boolean.class, "disableDamageImmunity", false)
                    .setRemoved()
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 禁止蝙蝠生成
     */
    public static final RuleAccessor<Boolean> disableBatCanSpawn = register(
            RuleFactory.create(Boolean.class, "disableBatCanSpawn", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 海龟蛋快速孵化
     */
    public static final RuleAccessor<Boolean> turtleEggFastHatch = register(
            RuleFactory.create(Boolean.class, "turtleEggFastHatch", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 强制打开容器
     */
    public static final RuleAccessor<ForceOpenContainer> forceOpenContainer = register(
            RuleFactory.create(ForceOpenContainer.class, "forceOpenContainer", ForceOpenContainer.FALSE)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 村民无限交易
     */
    public static final RuleAccessor<Boolean> villagerInfiniteTrade = register(
            RuleFactory.create(Boolean.class, "villagerInfiniteTrade", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 烟花火箭使用冷却
     */
    public static final RuleAccessor<Boolean> fireworkRocketUseCooldown = register(
            RuleFactory.create(Boolean.class, "fireworkRocketUseCooldown", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 激流忽略条件
     */
    public static final RuleAccessor<Boolean> riptideIgnoreConditions = register(
            RuleFactory.create(Boolean.class, "riptideIgnoreConditions", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 将镐作为基岩的有效采集工具
     */
    public static final RuleAccessor<Boolean> pickaxeMinedBedrock = register(
            RuleFactory.create(Boolean.class, "pickaxeMinedBedrock", false)
                    .setRemoved()
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 村民回血
     */
    public static final RuleAccessor<Boolean> villagerHeal = register(
            RuleFactory.create(Boolean.class, "villagerHeal", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 假玩家回血
     */
    public static final RuleAccessor<Boolean> fakePlayerHeal = register(
            RuleFactory.create(Boolean.class, "fakePlayerHeal", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 最大方块交互距离适用于实体
     */
    public static final RuleAccessor<Boolean> maxBlockPlaceDistanceReferToEntity = register(
            RuleFactory.create(Boolean.class, "maxBlockPlaceDistanceReferToEntity", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 击退棒
     */
    public static final RuleAccessor<Boolean> knockbackStick = register(
            RuleFactory.create(Boolean.class, "knockbackStick", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 禁止重生方块爆炸
     */
    public static final RuleAccessor<Boolean> disableRespawnBlocksExplode = register(
            RuleFactory.create(Boolean.class, "disableRespawnBlocksExplode", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * CCE更新抑制器
     */
    public static final RuleAccessor<String> CCEUpdateSuppression = register(
            RuleFactory.create(String.class, "CCEUpdateSuppression", "false")
                    .addCategories(RuleCategory.FEATURE)
                    .addOptions("true", "false")
                    .setLenient()
                    .build()
    );

    /**
     * 开放{@code /seed}命令权限
     */
    public static final RuleAccessor<Boolean> openSeedPermission = register(
            RuleFactory.create(Boolean.class, "openSeedPermission", false)
                    .addCategories(RuleCategory.COMMAND)
                    .build()
    );

    /**
     * 开放{@code /carpet}命令权限
     */
    public static final RuleAccessor<Boolean> openCarpetPermission = register(
            RuleFactory.create(Boolean.class, "openCarpetPermission", false)
                    .addCategories(RuleCategory.COMMAND, RuleCategory.CLIENT)
                    .build()
    );

    /**
     * 开放{@code /gamerule}命令权限
     */
    public static final RuleAccessor<Boolean> openGameRulePermission = register(
            RuleFactory.create(Boolean.class, "openGameRulePermission", false)
                    .addCategories(RuleCategory.COMMAND)
                    .build()
    );

    /**
     * 打开村民物品栏
     */
    public static final RuleAccessor<Boolean> openVillagerInventory = register(
            RuleFactory.create(Boolean.class, "openVillagerInventory", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 和平的苦力怕
     */
    public static final RuleAccessor<Boolean> peacefulCreeper = register(
            RuleFactory.create(Boolean.class, "peacefulCreeper", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 经验转移
     */
    public static final RuleAccessor<String> commandXpTransfer = register(
            RuleFactory.create(String.class, "commandXpTransfer", OPS)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 生存旁观切换命令
     */
    public static final RuleAccessor<String> commandSpectator = register(
            RuleFactory.create(String.class, "commandSpectator", OPS)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 查找器命令
     */
    public static final RuleAccessor<String> commandFinder = register(
            RuleFactory.create(String.class, "commandFinder", TRUE)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 自杀
     */
    public static final RuleAccessor<String> commandKillMe = register(
            RuleFactory.create(String.class, "commandKillMe", OPS)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 路径点管理器
     */
    public static final RuleAccessor<String> commandLocations = register(
            RuleFactory.create(String.class, "commandLocations", OPS)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 生命值不满可以进食
     */
    public static final RuleAccessor<Boolean> healthNotFullCanEat = register(
            RuleFactory.create(Boolean.class, "healthNotFullCanEat", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 可采集刷怪笼
     */
    public static final RuleAccessor<Boolean> canMineSpawner = register(
            RuleFactory.create(Boolean.class, "canMineSpawner", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 假玩家生成时无击退
     */
    public static final RuleAccessor<Boolean> fakePlayerSpawnNoKnockback = register(
            RuleFactory.create(Boolean.class, "fakePlayerSpawnNoKnockback", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 可激活侦测器
     */
    public static final RuleAccessor<Boolean> canActivatesObserver = register(
            RuleFactory.create(Boolean.class, "canActivatesObserver", false)
                    .addCategories(RuleCategory.FEATURE, RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 禁止水结冰
     */
    public static final RuleAccessor<Boolean> disableWaterFreezes = register(
            RuleFactory.create(Boolean.class, "disableWaterFreezes", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 假玩家动作保留物品
     */
    public static final RuleAccessor<Boolean> fakePlayerActionKeepItem = register(
            RuleFactory.create(Boolean.class, "fakePlayerActionKeepItem", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 绘制粒子线命令
     */
    public static final RuleAccessor<String> commandParticleLine = register(
            RuleFactory.create(String.class, "commandParticleLine", FALSE)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .setRemoved()
                    .build()
    );

    /**
     * 禁止特定生物在和平模式下被清除
     */
    public static final RuleAccessor<Boolean> disableMobPeacefulDespawn = register(
            RuleFactory.create(Boolean.class, "disableMobPeacefulDespawn", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 船可以直接走向一格高的方块
     */
    public static final RuleAccessor<Boolean> climbingBoat = register(
            RuleFactory.create(Boolean.class, "climbingBoat", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 可重复使用的锻造模板
     */
    public static final RuleAccessor<ReusableSmithingTemplate> reusableSmithingTemplate = register(
            RuleFactory.create(ReusableSmithingTemplate.class, "reusableSmithingTemplate", ReusableSmithingTemplate.FALSE)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 开放/tp命令权限
     */
    public static final RuleAccessor<Boolean> openTpPermission = register(
            RuleFactory.create(Boolean.class, "openTpPermission", false)
                    .addCategories(RuleCategory.COMMAND)
                    .build()
    );

    /**
     * 易碎深板岩
     */
    public static final RuleAccessor<Boolean> softDeepslate = register(
            RuleFactory.create(Boolean.class, "softDeepslate", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 易碎黑曜石
     */
    public static final RuleAccessor<Boolean> softObsidian = register(
            RuleFactory.create(Boolean.class, "softObsidian", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 易碎矿石
     */
    public static final RuleAccessor<Boolean> softOres = register(
            RuleFactory.create(Boolean.class, "softOres", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 更好的不死图腾
     */
    public static final RuleAccessor<BetterTotemOfUndying> betterTotemOfUndying = register(
            RuleFactory.create(BetterTotemOfUndying.class, "betterTotemOfUndying", BetterTotemOfUndying.VANILLA)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 假玩家动作命令
     */
    public static final RuleAccessor<String> commandPlayerAction = register(
            RuleFactory.create(String.class, "commandPlayerAction", OPS)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 假玩家合成支持潜影盒
     */
    public static final RuleAccessor<Boolean> fakePlayerPickItemFromShulkerBox = register(
            RuleFactory.create(Boolean.class, "fakePlayerShulkerBoxItemHandling", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 自定义猪灵交易时间
     */
    public static final RuleAccessor<Long> customPiglinBarteringTime = register(
            RuleFactory.create(Long.class, "customPiglinBarteringTime", -1L)
                    .addCategories(RuleCategory.SURVIVAL)
                    .addValidator(
                            newValue -> newValue >= 0 || newValue == -1,
                            () -> ValidatorFeedbacks.greaterOrEqualOrValue(0, -1)
                    )
                    .build()
    );

    /**
     * 快速设置假玩家合成
     */
    public static final RuleAccessor<QuickSettingFakePlayerCraft> quickSettingFakePlayerCraft = register(
            RuleFactory.create(QuickSettingFakePlayerCraft.class, "quickSettingFakePlayerCraft", QuickSettingFakePlayerCraft.FALSE)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 假玩家死亡不掉落
     */
    public static final RuleAccessor<Boolean> fakePlayerKeepInventory = register(
            RuleFactory.create(Boolean.class, "fakePlayerKeepInventory", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 假玩家死亡不掉落条件
     */
    public static final RuleAccessor<FakePlayerKeepInventoryCondition> fakePlayerKeepInventoryCondition = register(
            RuleFactory.create(FakePlayerKeepInventoryCondition.class, "fakePlayerKeepInventoryCondition", FakePlayerKeepInventoryCondition.UNCONDITIONAL)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 苦力怕命令
     */
    public static final RuleAccessor<String> commandCreeper = register(
            RuleFactory.create(String.class, "commandCreeper", FALSE)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 规则搜索命令
     */
    public static final RuleAccessor<String> commandRuleSearch = register(
            RuleFactory.create(String.class, "commandRuleSearch", OPS)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 增强闪电苦力怕
     */
    public static final RuleAccessor<Boolean> superChargedCreeper = register(
            RuleFactory.create(Boolean.class, "superChargedCreeper", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 玩家掉落头颅
     */
    public static final RuleAccessor<Boolean> playerDropHead = register(
            RuleFactory.create(Boolean.class, "playerDropHead", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 信标范围扩展
     */
    public static final RuleAccessor<Integer> beaconRangeExpand = register(
            RuleFactory.create(Integer.class, "beaconRangeExpand", 0)
                    .addCategories(RuleCategory.SURVIVAL)
                    .addValidator(
                            integer -> integer <= RuleUtils.MAX_BEACON_RANGE,
                            () -> ValidatorFeedbacks.lessThanOrEqual(RuleUtils.MAX_BEACON_RANGE)
                    )
                    .build()
    );

    /**
     * 信标世界高度
     */
    public static final RuleAccessor<Boolean> beaconWorldHeight = register(
            RuleFactory.create(Boolean.class, "beaconWorldHeight", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 可高亮方块坐标
     */
    public static final RuleAccessor<CanHighlightBlockPos> canHighlightBlockPos = register(
            RuleFactory.create(CanHighlightBlockPos.class, "canHighlightBlockPos", CanHighlightBlockPos.DEFAULT)
                    .addCategories(RuleCategory.SURVIVAL, RuleCategory.CLIENT)
                    .setClient()
                    .build()
    );

    /**
     * 玩家管理器命令
     */
    public static final RuleAccessor<String> commandPlayerManager = register(
            RuleFactory.create(String.class, "commandPlayerManager", OPS)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 方块掉落物直接进入物品栏
     */
    public static final RuleAccessor<BlockDropsDirectlyEnterInventory> blockDropsDirectlyEnterInventory = register(
            RuleFactory.create(
                            BlockDropsDirectlyEnterInventory.class,
                            "blockDropsDirectlyEnterInventory",
                            BlockDropsDirectlyEnterInventory.FALSE
                    )
                    .addCategories(RuleCategory.SURVIVAL)
                    .setCustomRuleSwitch(enabled -> {
                                BlockDropsDirectlyEnterInventory value = CarpetOrgAdditionSettings.blockDropsDirectlyEnterInventory.value();
                                return switch (value) {
                                    case TRUE, FALSE -> value;
                                    case CUSTOM -> BlockDropsDirectlyEnterInventory.active(enabled);
                                };
                            },
                            () -> CarpetOrgAdditionSettings.blockDropsDirectlyEnterInventory.value().isCustom()
                    )
                    .build()
    );

    /**
     * 海龟蛋快速采集
     */
    public static final RuleAccessor<Boolean> turtleEggFastMine = register(
            RuleFactory.create(Boolean.class, "turtleEggFastMine", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 导航器
     */
    public static final RuleAccessor<String> commandNavigate = register(
            RuleFactory.create(String.class, "commandNavigate", TRUE)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 玩家死亡产生的掉落物不会自然消失
     */
    public static final RuleAccessor<Boolean> playerDropsNotDespawning = register(
            RuleFactory.create(Boolean.class, "playerDropsNotDespawning", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 假玩家最大物品操作次数
     */
    public static final RuleAccessor<Integer> fakePlayerMaxItemOperationCount = register(
            RuleFactory.create(Integer.class, "fakePlayerMaxItemOperationCount", 3)
                    .addCategories(RuleCategory.SURVIVAL)
                    .addOptions(1, 3, 5, -1)
                    .setLenient()
                    .addValidator(
                            newValue -> newValue >= RuleUtils.MIN_CRAFT_COUNT || newValue == -1,
                            () -> ValidatorFeedbacks.greaterOrEqualOrValue(RuleUtils.MIN_CRAFT_COUNT, -1)
                    )
                    .build()
    );

    /**
     * 假玩家生成时内存泄漏修复
     */
    public static final RuleAccessor<Boolean> fakePlayerSpawnMemoryLeakFix = register(
            RuleFactory.create(Boolean.class, "fakePlayerSpawnMemoryLeakFix", false)
                    .addCategories(RuleCategory.BUGFIX)
                    .build()
    );

    /**
     * 快递命令
     */
    public static final RuleAccessor<String> commandMail = register(
            RuleFactory.create(String.class, "commandMail", OPS)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 抑制方块破坏位置不匹配警告
     */
    public static final RuleAccessor<Boolean> suppressionMismatchInDestroyBlockPosWarn = register(
            RuleFactory.create(Boolean.class, "suppressionMismatchInDestroyBlockPosWarn", false)
                    .addCategories(RuleCategory.EXPERIMENTAL)
                    .build()
    );

    /**
     * 同步导航器航点
     */
    public static final RuleAccessor<Boolean> syncNavigateWaypoint = register(
            RuleFactory.create(Boolean.class, "syncNavigateWaypoint", true)
                    .addCategories(RuleCategory.CLIENT)
                    .addListener((source, value) -> {
                        if (source == null) {
                            return;
                        }
                        List<AbstractNavigator> list = source.getServer().getPlayerList().getPlayers()
                                .stream()
                                .map(PlayerComponentCoordinator::getCoordinator)
                                .map(PlayerComponentCoordinator::getNavigatorManager)
                                .map(NavigatorManager::getNavigator)
                                .filter(Objects::nonNull)
                                .toList();
                        // 设置玩家路径点
                        if (value) {
                            list.forEach(navigator -> navigator.syncWaypoint(true));
                        } else {
                            list.forEach(AbstractNavigator::clear);
                        }
                    })
                    .setClient()
                    .setRemoved()
                    .build()
    );

    /**
     * 潜影盒堆叠
     */
    public static final RuleAccessor<Boolean> shulkerBoxStackable = register(
            RuleFactory.create(Boolean.class, "shulkerBoxStackable", false)
                    .addCategories(RuleCategory.EXPERIMENTAL)
                    .addSilenceValidator((source, value) -> {
                        if (CONFIRM_ENABLE.orElse(false)) {
                            return true;
                        }
                        if (!CarpetOrgAdditionConstants.LITHIUM || !value || source == null) {
                            return true;
                        }
                        ServerPlayer player = source.getPlayer();
                        if (player == null) {
                            return true;
                        }
                        MinecraftServer server = source.getServer();
                        Dialog dialog = CompatibilityDialogProvider.getShulkerBoxStackableDialog(server);
                        PlayerUtils.openDialog(player, dialog);
                        return false;
                    })
                    .build()
    );

    /**
     * 最大服务器交互距离同步客户端
     */
    public static final RuleAccessor<Boolean> maxBlockPlaceDistanceSyncClient = register(
            RuleFactory.create(Boolean.class, "maxBlockPlaceDistanceSyncClient", true)
                    .addCategories(RuleCategory.CLIENT)
                    .setClient()
                    .build()
    );

    /**
     * 限制幻翼生成
     */
    public static final RuleAccessor<Boolean> limitPhantomSpawn = register(
            RuleFactory.create(Boolean.class, "limitPhantomSpawn", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 立即应用工具效果
     */
    public static final RuleAccessor<Boolean> applyToolEffectsImmediately = register(
            RuleFactory.create(Boolean.class, "applyToolEffectsImmediately", false)
                    .addCategories(RuleCategory.BUGFIX)
                    .setHidden()
                    .build()
    );

    /**
     * 强制补货
     */
    public static final RuleAccessor<Boolean> forceRestock = register(
            RuleFactory.create(Boolean.class, "forceRestock", false)
                    .setHidden()
                    .build()
    );

    /**
     * 自动同步玩家状态
     */
    public static final RuleAccessor<Boolean> autoSyncPlayerStatus = register(
            RuleFactory.create(Boolean.class, "autoSyncPlayerStatus", false)
                    .setHidden()
                    .build()
    );

    /**
     * 记录玩家命令
     */
    public static final RuleAccessor<Boolean> recordPlayerCommand = register(
            RuleFactory.create(Boolean.class, "recordPlayerCommand", false)
                    .addCategories(RuleCategory.COMMAND)
                    .build()
    );

    /**
     * 保护类魔咒兼容
     */
    public static final RuleAccessor<Boolean> protectionEnchantmentCompatible = register(
            RuleFactory.create(Boolean.class, "protectionEnchantmentCompatible", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 伤害类魔咒兼容
     */
    public static final RuleAccessor<Boolean> damageEnchantmentCompatible = register(
            RuleFactory.create(Boolean.class, "damageEnchantmentCompatible", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 每页最大行数
     */
    public static final RuleAccessor<Integer> maxLinesPerPage = register(
            RuleFactory.create(Integer.class, "maxLinesPerPage", 10)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(10, 15, 20, 25)
                    .addValidator(newValue -> newValue > 0, () -> ValidatorFeedbacks.greaterThan(0))
                    .setLenient()
                    .build()
    );

    /**
     * 不死图腾无敌时间
     */
    public static final RuleAccessor<Boolean> totemOfUndyingInvincibleTime = register(
            RuleFactory.create(Boolean.class, "totemOfUndyingInvincibleTime", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .setHidden()
                    .build()
    );

    /**
     * /player命令打开玩家物品栏
     */
    public static final RuleAccessor<String> playerCommandOpenPlayerInventory = register(
            RuleFactory.create(String.class, "playerCommandOpenPlayerInventory", FALSE)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * /player命令假玩家传送
     */
    public static final RuleAccessor<String> playerCommandTeleportFakePlayer = register(
            RuleFactory.create(String.class, "playerCommandTeleportFakePlayer", FALSE)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 村民虚空交易
     */
    public static final RuleAccessor<Boolean> villagerVoidTrading = register(
            RuleFactory.create(Boolean.class, "villagerVoidTrading", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 经验球合并
     */
    public static final RuleAccessor<Boolean> experienceOrbMerge = register(
            RuleFactory.create(Boolean.class, "experienceOrbMerge", false)
                    .addCategories(RuleCategory.FEATURE)
                    .setHidden()
                    .build()
    );

    /**
     * 快捷潜影盒
     */
    public static final RuleAccessor<Boolean> quickShulker = register(
            RuleFactory.create(Boolean.class, "quickShulker", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .setHidden()
                    .build()
    );

    /**
     * 禁用创造容器掉落
     */
    public static final RuleAccessor<Boolean> disableCreativeContainerDrops = register(
            RuleFactory.create(Boolean.class, "disableCreativeContainerDrops", false)
                    .addCategories(RuleCategory.CREATIVE)
                    .build()
    );

    /**
     * 显示假玩家召唤者
     */
    public static final RuleAccessor<Boolean> displayPlayerSummoner = register(
            RuleFactory.create(Boolean.class, "displayPlayerSummoner", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 设置铁砧经验消耗上限
     */
    public static final RuleAccessor<Integer> setAnvilCostLimit = register(
            RuleFactory.create(Integer.class, "setAnvilExperienceConsumptionLimit", -1)
                    .addCategories(RuleCategory.SURVIVAL)
                    .addValidator(
                            integer -> integer == -1 || integer > 0 && integer <= 10000,
                            () -> ValidatorFeedbacks.rangeOrValue(1, 10000, -1)
                    )
                    .addOptions(100, 1000, 10000, -1)
                    .setLenient()
                    .build()
    );

    /**
     * 禁用熔炉掉落经验
     */
    public static final RuleAccessor<Boolean> disableFurnaceDropExperience = register(
            RuleFactory.create(Boolean.class, "disableFurnaceDropExperience", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .setHidden()
                    .build()
    );

    /**
     * /player命令打开玩家物品栏选项
     */
    public static final RuleAccessor<OpenPlayerInventoryCommandOption> playerCommandOpenPlayerInventoryOption = register(
            RuleFactory.create(OpenPlayerInventoryCommandOption.class, "playerCommandOpenPlayerInventoryOption", OpenPlayerInventoryCommandOption.FAKE_PLAYER)
                    .addCategories(RuleCategory.COMMAND)
                    .build()
    );

    /**
     * 玩家管理器强制添加注释
     */
    public static final RuleAccessor<Boolean> playerManagerForceComment = register(
            RuleFactory.create(Boolean.class, "playerManagerForceComment", false)
                    .addCategories(RuleCategory.COMMAND)
                    .build()
    );

    /**
     * 物品拾取范围扩展
     */
    @SuppressWarnings("Convert2MethodRef")
    public static final RuleAccessor<Integer> itemPickupRangeExpand = register(
            RuleFactory.create(Integer.class, "itemPickupRangeExpand", 0)
                    .addCategories(RuleCategory.FEATURE)
                    .addValidator(integer -> integer >= 0, () -> ValidatorFeedbacks.greaterThanOrEqual(0))
                    .setHidden()
                    .addOptions(0, 3, 5)
                    .setLenient()
                    .setCustomRuleSwitch(
                            enabled -> enabled ? CarpetOrgAdditionSettings.itemPickupRangeExpand.value() : 0,
                            // 此处不能使用方法引用，因为方法引用会在创建时立即求值，但此时值还未初始化，执行到这里时会抛出空指针异常
                            // 直接交换成员顺序也能解决问题，但未来格式化或重构代码时可能无意间将成员顺序改回来
                            () -> CarpetOrgAdditionSettings.itemPickupRangeExpandPlayerControl.value()
                    )
                    .build()
    );

    /**
     * 物品拾取范围扩展玩家控制
     */
    public static final RuleAccessor<Boolean> itemPickupRangeExpandPlayerControl = register(
            RuleFactory.create(Boolean.class, "itemPickupRangeExpandPlayerControl", false)
                    .addCategories(RuleCategory.FEATURE)
                    .setHidden()
                    .build()
    );

    /**
     * 打开玩家物品栏GCA样式
     */
    public static final RuleAccessor<Boolean> playerCommandOpenPlayerInventoryGcaStyle = register(
            RuleFactory.create(Boolean.class, "playerCommandOpenPlayerInventoryGcaStyle", true)
                    .addCategories(RuleCategory.COMMAND)
                    .build()
    );

    /**
     * 易碎下界合金
     */
    public static final RuleAccessor<Boolean> softNetherite = register(
            RuleFactory.create(Boolean.class, "softNetherite", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 禁用风弹效果
     */
    public static final RuleAccessor<Boolean> disableWindChargeEffect = register(
            RuleFactory.create(Boolean.class, "disableWindChargeEffect", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * {@code /player}命令召唤玩家模型
     */
    public static final RuleAccessor<String> playerCommandSummonMannequin = register(
            RuleFactory.create(String.class, "playerCommandSummonMannequin", FALSE)
                    .addCategories(RuleCategory.COMMAND)
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 打开玩家物品栏
     */
    public static final RuleAccessor<OpenPlayerInventory> openPlayerInventory = register(
            RuleFactory.create(OpenPlayerInventory.class, "openPlayerInventory", OpenPlayerInventory.FALSE)
                    .addCategories(RuleCategory.SURVIVAL)
                    .setHidden()
                    .build()
    );

    private static <T> RuleAccessor<T> register(RuleContext<T> context) {
        RULES.add(context);
        return new RuleAccessor<>(context);
    }

    public static void register() {
        int count = 0;
        SettingsManager settingManager = CarpetOrgAdditionExtension.getSettingManager();
        for (RuleContext<?> context : RULES) {
            if (context.shouldRegister()) {
                CarpetRule<?> rule = context.rule();
                try {
                    settingManager.addCarpetRule(rule);
                    count++;
                } catch (UnsupportedOperationException e) {
                    CarpetOrgAddition.LOGGER.error("{}: {} conflicts with another Carpet extension, disabling rule", CarpetOrgAdditionMetadata.MOD_NAME, rule.name());
                }
                CustomRuleControl<?> control = context.getCustomRuleControl();
                if (control != null) {
                    CustomRuleValueManager.put(control, rule);
                }
            }
        }
        CarpetOrgAddition.LOGGER.debug("{} rules registered", count);
        CarpetOrgAddition.LOGGER.debug("Total {} rules", RULES.size());
    }

    public static Set<RuleContext<?>> listRules() {
        return RULES;
    }
}
