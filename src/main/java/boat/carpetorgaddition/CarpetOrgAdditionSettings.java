package boat.carpetorgaddition;

import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.navigator.AbstractNavigator;
import boat.carpetorgaddition.periodic.navigator.NavigatorManager;
import boat.carpetorgaddition.rule.*;
import boat.carpetorgaddition.rule.value.*;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleCategory;
import carpet.api.settings.SettingsManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CarpetOrgAdditionSettings {
    /**
     * 潜影盒是否允许被堆叠，这还需要同时启用{@link CarpetOrgAdditionSettings#SHULKER_BOX_STACKABLE}
     */
    public static final ScopedValue<Boolean> SHULKER_BOX_STACK_COUNT_CHANGED = ScopedValue.newInstance();
    /**
     * 玩家是否正在执行{@code /killMe}命令
     */
    public static final ScopedValue<Boolean> COMMITTING_SUICIDE = ScopedValue.newInstance();
    /**
     * 当前正在使用铁砧附魔的玩家
     */
    public static final ScopedValue<Player> ENCHANTER = ScopedValue.newInstance();
    /**
     * 当前方块的破坏者，启用{@link CarpetOrgAdditionSettings#BLOCK_DROPS_DIRECTLY_ENTER_INVENTORY}后，方块掉落物会直接进入玩家物品栏
     */
    public static final ScopedValue<ServerPlayer> BLOCK_BREAKER = ScopedValue.newInstance();
    /**
     * 是否正在使用引雷三叉戟
     */
    public static final ScopedValue<Boolean> USE_CHANNELING_TRIDENT = ScopedValue.newInstance();
    private static final Set<RuleContext<?>> RULES = new LinkedHashSet<>();
    private static final String OPS = "ops";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final List<String> COMMAND_OPTIONS = List.of(TRUE, FALSE, OPS, "0", "1", "2", "3", "4");
    public static final String ORG = "Org";
    public static final String HIDDEN = "Hidden";

    private CarpetOrgAdditionSettings() {
    }

    /**
     * 制作物品分身
     */
    public static final RuleAccessor<String> COMMAND_ITEM_SHADOWING = register(
            RuleFactory.create("commandItemShadowing", OPS)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 设置基岩硬度
     */
    public static final RuleAccessor<Float> SET_BEDROCK_HARDNESS = register(
            RuleFactory.create("setBedrockHardness", -1F)
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
    public static final RuleAccessor<Boolean> BINDING_CURSE_INVALIDATION = register(
            RuleFactory.create("bindingCurseInvalidation", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 禁用钓鱼开阔水域检测
     */
    public static final RuleAccessor<Boolean> DISABLE_OPEN_OR_WATER_DETECTION = register(
            RuleFactory.create("disableOpenOrWaterDetection", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 创造玩家免疫/kill
     */
    public static final RuleAccessor<Boolean> CREATIVE_IMMUNE_KILL = register(
            RuleFactory.create("creativeImmuneKill", false)
                    .addCategories(RuleCategory.CREATIVE)
                    .build()
    );

    /**
     * 盯着末影人眼睛看时不会激怒末影人
     */
    public static final RuleAccessor<Boolean> STARING_ENDERMAN_NOT_ANGRY = register(
            RuleFactory.create("staringEndermanNotAngry", false)
                    .addCategories(RuleCategory.SURVIVAL, RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 耕地防踩踏
     */
    public static final RuleAccessor<Boolean> FARMLAND_PREVENT_STEPPING = register(
            RuleFactory.create("farmlandPreventStepping", false)
                    .addCategories(RuleCategory.SURVIVAL, RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 最大方块交互距离
     */
    public static final RuleAccessor<Double> MAX_BLOCK_PLACE_DISTANCE = register(
            RuleFactory.create("maxBlockPlaceDistance", -1.0)
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
    public static final RuleAccessor<Boolean> SIMPLE_UPDATE_SKIPPER = register(
            RuleFactory.create("simpleUpdateSkipper", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 引雷忽略条件
     */
    public static final RuleAccessor<IgnoreChannelingConditions> CHANNELING_IGNORE_CONDITIONS = register(
            RuleFactory.create("channelingIgnoreConditions", IgnoreChannelingConditions.FALSE)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 无伤末影珍珠
     */
    public static final RuleAccessor<Boolean> NOT_DAMAGE_ENDER_PEARL = register(
            RuleFactory.create("notDamageEnderPearl", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 禁用伤害免疫
     */
    public static final RuleAccessor<Boolean> DISABLE_DAMAGE_IMMUNITY = register(
            RuleFactory.create("disableDamageImmunity", false)
                    .setRemoved()
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 禁止蝙蝠生成
     */
    public static final RuleAccessor<Boolean> DISABLE_BAT_CAN_SPAWN = register(
            RuleFactory.create("disableBatCanSpawn", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 海龟蛋快速孵化
     */
    public static final RuleAccessor<Boolean> TURTLE_EGG_FAST_HATCH = register(
            RuleFactory.create("turtleEggFastHatch", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 强制打开容器
     */
    public static final RuleAccessor<ForceOpenContainer> FORCE_OPEN_CONTAINER = register(
            RuleFactory.create("forceOpenContainer", ForceOpenContainer.FALSE)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 村民无限交易
     */
    public static final RuleAccessor<Boolean> VILLAGER_INFINITE_TRADE = register(
            RuleFactory.create("villagerInfiniteTrade", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 烟花火箭使用冷却
     */
    public static final RuleAccessor<Boolean> FIREWORK_ROCKET_USE_COOLDOWN = register(
            RuleFactory.create("fireworkRocketUseCooldown", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 激流忽略条件
     */
    public static final RuleAccessor<Boolean> RIPTIDE_IGNORE_CONDITIONS = register(
            RuleFactory.create("riptideIgnoreConditions", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 将镐作为基岩的有效采集工具
     */
    public static final RuleAccessor<Boolean> PICKAXE_MINED_BEDROCK = register(
            RuleFactory.create("pickaxeMinedBedrock", false)
                    .setRemoved()
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 村民回血
     */
    public static final RuleAccessor<Boolean> VILLAGER_HEAL = register(
            RuleFactory.create("villagerHeal", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 假玩家回血
     */
    public static final RuleAccessor<Boolean> FAKE_PLAYER_HEAL = register(
            RuleFactory.create("fakePlayerHeal", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 最大方块交互距离适用于实体
     */
    public static final RuleAccessor<Boolean> MAX_BLOCK_PLACE_DISTANCE_REFER_TO_ENTITY = register(
            RuleFactory.create("maxBlockPlaceDistanceReferToEntity", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 击退棒
     */
    public static final RuleAccessor<Boolean> KNOCKBACK_STICK = register(
            RuleFactory.create("knockbackStick", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 禁止重生方块爆炸
     */
    public static final RuleAccessor<Boolean> DISABLE_RESPAWN_BLOCKS_EXPLODE = register(
            RuleFactory.create("disableRespawnBlocksExplode", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * CCE更新抑制器
     */
    public static final RuleAccessor<String> CCE_UPDATE_SUPPRESSION = register(
            RuleFactory.create("CCEUpdateSuppression", FALSE)
                    .addCategories(RuleCategory.FEATURE)
                    .addOptions(TRUE, FALSE)
                    .setLenient()
                    .build()
    );

    /**
     * 开放{@code /seed}命令权限
     */
    public static final RuleAccessor<Boolean> OPEN_SEED_PERMISSION = register(
            RuleFactory.create("openSeedPermission", false)
                    .setCommand()
                    .build()
    );

    /**
     * 开放{@code /carpet}命令权限
     */
    public static final RuleAccessor<Boolean> OPEN_CARPET_PERMISSION = register(
            RuleFactory.create("openCarpetPermission", false)
                    .setCommand()
                    .setClient()
                    .build()
    );

    /**
     * 开放{@code /gamerule}命令权限
     */
    public static final RuleAccessor<Boolean> OPEN_GAME_RULE_PERMISSION = register(
            RuleFactory.create("openGameRulePermission", false)
                    .setCommand()
                    .build()
    );

    /**
     * 打开村民物品栏
     */
    public static final RuleAccessor<Boolean> OPEN_VILLAGER_INVENTORY = register(
            RuleFactory.create("openVillagerInventory", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 和平的苦力怕
     */
    public static final RuleAccessor<Boolean> PEACEFUL_CREEPER = register(
            RuleFactory.create("peacefulCreeper", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 经验转移
     */
    public static final RuleAccessor<String> COMMAND_XP_TRANSFER = register(
            RuleFactory.create("commandXpTransfer", OPS)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 生存旁观切换命令
     */
    public static final RuleAccessor<String> COMMAND_SPECTATOR = register(
            RuleFactory.create("commandSpectator", OPS)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 查找器命令
     */
    public static final RuleAccessor<String> COMMAND_FINDER = register(
            RuleFactory.create("commandFinder", TRUE)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 自杀
     */
    public static final RuleAccessor<String> COMMAND_KILL_ME = register(
            RuleFactory.create("commandKillMe", OPS)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 路径点管理器
     */
    public static final RuleAccessor<String> COMMAND_LOCATIONS = register(
            RuleFactory.create("commandLocations", OPS)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 生命值不满可以进食
     */
    public static final RuleAccessor<Boolean> HEALTH_NOT_FULL_CAN_EAT = register(
            RuleFactory.create("healthNotFullCanEat", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 可采集刷怪笼
     */
    public static final RuleAccessor<Boolean> CAN_MINE_SPAWNER = register(
            RuleFactory.create("canMineSpawner", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 假玩家生成时无击退
     */
    public static final RuleAccessor<Boolean> FAKE_PLAYER_SPAWN_NO_KNOCKBACK = register(
            RuleFactory.create("fakePlayerSpawnNoKnockback", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 可激活侦测器
     */
    public static final RuleAccessor<Boolean> CAN_ACTIVATES_OBSERVER = register(
            RuleFactory.create("canActivatesObserver", false)
                    .addCategories(RuleCategory.FEATURE, RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 禁止水结冰
     */
    public static final RuleAccessor<Boolean> DISABLE_WATER_FREEZES = register(
            RuleFactory.create("disableWaterFreezes", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 假玩家动作保留物品
     */
    public static final RuleAccessor<Boolean> FAKE_PLAYER_ACTION_KEEP_ITEM = register(
            RuleFactory.create("fakePlayerActionKeepItem", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 绘制粒子线命令
     */
    public static final RuleAccessor<String> COMMAND_PARTICLE_LINE = register(
            RuleFactory.create("commandParticleLine", FALSE)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .setRemoved()
                    .build()
    );

    /**
     * 禁止特定生物在和平模式下被清除
     */
    public static final RuleAccessor<Boolean> DISABLE_MOB_PEACEFUL_DESPAWN = register(
            RuleFactory.create("disableMobPeacefulDespawn", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 船可以直接走向一格高的方块
     */
    public static final RuleAccessor<Boolean> CLIMBING_BOAT = register(
            RuleFactory.create("climbingBoat", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 可重复使用的锻造模板
     */
    public static final RuleAccessor<ReusableSmithingTemplate> REUSABLE_SMITHING_TEMPLATE = register(
            RuleFactory.create("reusableSmithingTemplate", ReusableSmithingTemplate.FALSE)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 开放/tp命令权限
     */
    public static final RuleAccessor<Boolean> OPEN_TP_PERMISSION = register(
            RuleFactory.create("openTpPermission", false)
                    .setCommand()
                    .build()
    );

    /**
     * 易碎深板岩
     */
    public static final RuleAccessor<Boolean> SOFT_DEEPSLATE = register(
            RuleFactory.create("softDeepslate", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 易碎黑曜石
     */
    public static final RuleAccessor<Boolean> SOFT_OBSIDIAN = register(
            RuleFactory.create("softObsidian", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 易碎矿石
     */
    public static final RuleAccessor<Boolean> SOFT_ORES = register(
            RuleFactory.create("softOres", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 更好的不死图腾
     */
    public static final RuleAccessor<BetterTotemOfUndying> BETTER_TOTEM_OF_UNDYING = register(
            RuleFactory.create("betterTotemOfUndying", BetterTotemOfUndying.VANILLA)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 假玩家动作命令
     */
    public static final RuleAccessor<String> COMMAND_PLAYER_ACTION = register(
            RuleFactory.create("commandPlayerAction", OPS)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 假玩家合成支持潜影盒
     */
    public static final RuleAccessor<Boolean> FAKE_PLAYER_SHULKER_BOX_ITEM_HANDLING = register(
            RuleFactory.create("fakePlayerShulkerBoxItemHandling", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 自定义猪灵交易时间
     */
    public static final RuleAccessor<Long> CUSTOM_PIGLIN_BARTERING_TIME = register(
            RuleFactory.create("customPiglinBarteringTime", -1L)
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
    public static final RuleAccessor<QuickSettingFakePlayerCraft> QUICK_SETTING_FAKE_PLAYER_CRAFT = register(
            RuleFactory.create("quickSettingFakePlayerCraft", QuickSettingFakePlayerCraft.FALSE)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 假玩家死亡不掉落
     */
    public static final RuleAccessor<Boolean> FAKE_PLAYER_KEEP_INVENTORY = register(
            RuleFactory.create("fakePlayerKeepInventory", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 假玩家死亡不掉落条件
     */
    public static final RuleAccessor<FakePlayerKeepInventoryCondition> FAKE_PLAYER_KEEP_INVENTORY_CONDITION = register(
            RuleFactory.create("fakePlayerKeepInventoryCondition", FakePlayerKeepInventoryCondition.UNCONDITIONAL)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 苦力怕命令
     */
    public static final RuleAccessor<String> COMMAND_CREEPER = register(
            RuleFactory.create("commandCreeper", FALSE)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 规则搜索命令
     */
    public static final RuleAccessor<String> COMMAND_RULE_SEARCH = register(
            RuleFactory.create("commandRuleSearch", OPS)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 增强闪电苦力怕
     */
    public static final RuleAccessor<Boolean> SUPER_CHARGED_CREEPER = register(
            RuleFactory.create("superChargedCreeper", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 玩家掉落头颅
     */
    public static final RuleAccessor<Boolean> PLAYER_DROP_HEAD = register(
            RuleFactory.create("playerDropHead", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 信标范围扩展
     */
    public static final RuleAccessor<Integer> BEACON_RANGE_EXPAND = register(
            RuleFactory.create("beaconRangeExpand", 0)
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
    public static final RuleAccessor<Boolean> BEACON_WORLD_HEIGHT = register(
            RuleFactory.create("beaconWorldHeight", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 可高亮方块坐标
     */
    public static final RuleAccessor<CanHighlightBlockPos> CAN_HIGHLIGHT_BLOCK_POS = register(
            RuleFactory.create("canHighlightBlockPos", CanHighlightBlockPos.DEFAULT)
                    .addCategories(RuleCategory.SURVIVAL)
                    .setClient()
                    .build()
    );

    /**
     * 玩家管理器命令
     */
    public static final RuleAccessor<String> COMMAND_PLAYER_MANAGER = register(
            RuleFactory.create("commandPlayerManager", OPS)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 方块掉落物直接进入物品栏
     */
    public static final RuleAccessor<BlockDropsDirectlyEnterInventory> BLOCK_DROPS_DIRECTLY_ENTER_INVENTORY = register(
            RuleFactory.create("blockDropsDirectlyEnterInventory", BlockDropsDirectlyEnterInventory.FALSE)
                    .addCategories(RuleCategory.SURVIVAL)
                    .setCustomRuleSwitch(enabled -> {
                                BlockDropsDirectlyEnterInventory value = CarpetOrgAdditionSettings.BLOCK_DROPS_DIRECTLY_ENTER_INVENTORY.value();
                                return switch (value) {
                                    case TRUE, FALSE -> value;
                                    case CUSTOM -> BlockDropsDirectlyEnterInventory.active(enabled);
                                };
                            },
                            () -> CarpetOrgAdditionSettings.BLOCK_DROPS_DIRECTLY_ENTER_INVENTORY.value().isCustom()
                    )
                    .build()
    );

    /**
     * 海龟蛋快速采集
     */
    public static final RuleAccessor<Boolean> TURTLE_EGG_FAST_MINE = register(
            RuleFactory.create("turtleEggFastMine", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 导航器
     */
    public static final RuleAccessor<String> COMMAND_NAVIGATE = register(
            RuleFactory.create("commandNavigate", TRUE)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 玩家死亡产生的掉落物不会自然消失
     */
    public static final RuleAccessor<Boolean> PLAYER_DROPS_NOT_DESPAWNING = register(
            RuleFactory.create("playerDropsNotDespawning", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 假玩家最大物品操作次数
     */
    public static final RuleAccessor<Integer> FAKE_PLAYER_MAX_ITEM_OPERATION_COUNT = register(
            RuleFactory.create("fakePlayerMaxItemOperationCount", 3)
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
    public static final RuleAccessor<Boolean> FAKE_PLAYER_SPAWN_MEMORY_LEAK_FIX = register(
            RuleFactory.create("fakePlayerSpawnMemoryLeakFix", false)
                    .addCategories(RuleCategory.BUGFIX)
                    .build()
    );

    /**
     * 快递命令
     */
    public static final RuleAccessor<String> COMMAND_MAIL = register(
            RuleFactory.create("commandMail", OPS)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 抑制方块破坏位置不匹配警告
     */
    public static final RuleAccessor<Boolean> SUPPRESSION_MISMATCH_IN_DESTROY_BLOCK_POS_WARN = register(
            RuleFactory.create("suppressionMismatchInDestroyBlockPosWarn", false)
                    .addCategories(RuleCategory.EXPERIMENTAL)
                    .build()
    );

    /**
     * 同步导航器航点
     */
    public static final RuleAccessor<Boolean> SYNC_NAVIGATE_WAYPOINT = register(
            RuleFactory.create("syncNavigateWaypoint", true)
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
    public static final RuleAccessor<Boolean> SHULKER_BOX_STACKABLE = register(
            RuleFactory.create("shulkerBoxStackable", false)
                    .addCategories(RuleCategory.EXPERIMENTAL)
                    .build()
    );

    /**
     * 最大服务器交互距离同步客户端
     */
    public static final RuleAccessor<Boolean> MAX_BLOCK_PLACE_DISTANCE_SYNC_CLIENT = register(
            RuleFactory.create("maxBlockPlaceDistanceSyncClient", true)
                    .setClient()
                    .build()
    );

    /**
     * 限制幻翼生成
     */
    public static final RuleAccessor<Boolean> LIMIT_PHANTOM_SPAWN = register(
            RuleFactory.create("limitPhantomSpawn", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 立即应用工具效果
     */
    public static final RuleAccessor<Boolean> APPLY_TOOL_EFFECTS_IMMEDIATELY = register(
            RuleFactory.create("applyToolEffectsImmediately", false)
                    .addCategories(RuleCategory.BUGFIX)
                    .setHidden()
                    .build()
    );

    /**
     * 强制补货
     */
    public static final RuleAccessor<Boolean> FORCE_RESTOCK = register(
            RuleFactory.create("forceRestock", false)
                    .setHidden()
                    .build()
    );

    /**
     * 自动同步玩家状态
     */
    public static final RuleAccessor<Boolean> AUTO_SYNC_PLAYER_STATUS = register(
            RuleFactory.create("autoSyncPlayerStatus", false)
                    .setHidden()
                    .build()
    );

    /**
     * 记录玩家命令
     */
    public static final RuleAccessor<Boolean> RECORD_PLAYER_COMMAND = register(
            RuleFactory.create("recordPlayerCommand", false)
                    .build()
    );

    /**
     * 保护类魔咒兼容
     */
    public static final RuleAccessor<Boolean> PROTECTION_ENCHANTMENT_COMPATIBLE = register(
            RuleFactory.create("protectionEnchantmentCompatible", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 伤害类魔咒兼容
     */
    public static final RuleAccessor<Boolean> DAMAGE_ENCHANTMENT_COMPATIBLE = register(
            RuleFactory.create("damageEnchantmentCompatible", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 每页最大行数
     */
    public static final RuleAccessor<Integer> MAX_LINES_PER_PAGE = register(
            RuleFactory.create("maxLinesPerPage", 10)
                    .addOptions(10, 15, 20, 25)
                    .addValidator(newValue -> newValue > 0, () -> ValidatorFeedbacks.greaterThan(0))
                    .setLenient()
                    .build()
    );

    /**
     * 不死图腾无敌时间
     */
    public static final RuleAccessor<Boolean> TOTEM_OF_UNDYING_INVINCIBLE_TIME = register(
            RuleFactory.create("totemOfUndyingInvincibleTime", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .setHidden()
                    .build()
    );

    /**
     * /player命令打开玩家物品栏
     */
    public static final RuleAccessor<String> PLAYER_COMMAND_OPEN_PLAYER_INVENTORY = register(
            RuleFactory.create("playerCommandOpenPlayerInventory", FALSE)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * /player命令假玩家传送
     */
    public static final RuleAccessor<String> PLAYER_COMMAND_TELEPORT_FAKE_PLAYER = register(
            RuleFactory.create("playerCommandTeleportFakePlayer", FALSE)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 村民虚空交易
     */
    public static final RuleAccessor<Boolean> VILLAGER_VOID_TRADING = register(
            RuleFactory.create("villagerVoidTrading", false)
                    .addCategories(RuleCategory.FEATURE)
                    .build()
    );

    /**
     * 经验球合并
     */
    public static final RuleAccessor<Boolean> EXPERIENCE_ORB_MERGE = register(
            RuleFactory.create("experienceOrbMerge", false)
                    .addCategories(RuleCategory.FEATURE)
                    .setHidden()
                    .build()
    );

    /**
     * 快捷潜影盒
     */
    public static final RuleAccessor<Boolean> QUICK_SHULKER = register(
            RuleFactory.create("quickShulker", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .setHidden()
                    .build()
    );

    /**
     * 禁用创造容器掉落
     */
    public static final RuleAccessor<Boolean> DISABLE_CREATIVE_CONTAINER_DROPS = register(
            RuleFactory.create("disableCreativeContainerDrops", false)
                    .addCategories(RuleCategory.CREATIVE)
                    .build()
    );

    /**
     * 显示假玩家召唤者
     */
    public static final RuleAccessor<Boolean> DISPLAY_PLAYER_SUMMONER = register(
            RuleFactory.create("displayPlayerSummoner", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 设置铁砧经验消耗上限
     */
    public static final RuleAccessor<Integer> SET_ANVIL_EXPERIENCE_CONSUMPTION_LIMIT = register(
            RuleFactory.create("setAnvilExperienceConsumptionLimit", -1)
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
    public static final RuleAccessor<Boolean> DISABLE_FURNACE_DROP_EXPERIENCE = register(
            RuleFactory.create("disableFurnaceDropExperience", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .setHidden()
                    .build()
    );

    /**
     * /player命令打开玩家物品栏选项
     */
    public static final RuleAccessor<OpenPlayerInventoryCommandOption> PLAYER_COMMAND_OPEN_PLAYER_INVENTORY_OPTION = register(
            RuleFactory.create("playerCommandOpenPlayerInventoryOption", OpenPlayerInventoryCommandOption.FAKE_PLAYER)
                    .setCommand()
                    .build()
    );

    /**
     * 玩家管理器强制添加注释
     */
    public static final RuleAccessor<Boolean> PLAYER_MANAGER_FORCE_COMMENT = register(
            RuleFactory.create("playerManagerForceComment", false)
                    .setCommand()
                    .build()
    );

    /**
     * 物品拾取范围扩展
     */
    @SuppressWarnings("Convert2MethodRef")
    public static final RuleAccessor<Integer> ITEM_PICKUP_RANGE_EXPAND = register(
            RuleFactory.create("itemPickupRangeExpand", 0)
                    .addCategories(RuleCategory.FEATURE)
                    .addValidator(integer -> integer >= 0, () -> ValidatorFeedbacks.greaterThanOrEqual(0))
                    .setHidden()
                    .addOptions(0, 3, 5)
                    .setLenient()
                    .setCustomRuleSwitch(
                            enabled -> enabled ? CarpetOrgAdditionSettings.ITEM_PICKUP_RANGE_EXPAND.value() : 0,
                            // 此处不能使用方法引用，因为方法引用会在创建时立即求值，但此时值还未初始化，执行到这里时会抛出空指针异常
                            // 直接交换成员顺序也能解决问题，但未来格式化或重构代码时可能无意间将成员顺序改回来
                            () -> CarpetOrgAdditionSettings.ITEM_PICKUP_RANGE_EXPAND_PLAYER_CONTROL.value()
                    )
                    .build()
    );

    /**
     * 物品拾取范围扩展玩家控制
     */
    public static final RuleAccessor<Boolean> ITEM_PICKUP_RANGE_EXPAND_PLAYER_CONTROL = register(
            RuleFactory.create("itemPickupRangeExpandPlayerControl", false)
                    .addCategories(RuleCategory.FEATURE)
                    .setHidden()
                    .build()
    );

    /**
     * 打开玩家物品栏GCA样式
     */
    public static final RuleAccessor<Boolean> PLAYER_COMMAND_OPEN_PLAYER_INVENTORY_GCA_STYLE = register(
            RuleFactory.create("playerCommandOpenPlayerInventoryGcaStyle", true)
                    .setCommand()
                    .build()
    );

    /**
     * 易碎下界合金
     */
    public static final RuleAccessor<Boolean> SOFT_NETHERITE = register(
            RuleFactory.create("softNetherite", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 禁用风弹效果
     */
    public static final RuleAccessor<Boolean> DISABLE_WIND_CHARGE_EFFECT = register(
            RuleFactory.create("disableWindChargeEffect", false)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * {@code /player}命令召唤玩家模型
     */
    public static final RuleAccessor<String> PLAYER_COMMAND_SUMMON_MANNEQUIN = register(
            RuleFactory.create("playerCommandSummonMannequin", FALSE)
                    .setCommand()
                    .addOptions(COMMAND_OPTIONS)
                    .build()
    );

    /**
     * 打开玩家物品栏
     */
    public static final RuleAccessor<OpenPlayerInventory> OPEN_PLAYER_INVENTORY = register(
            RuleFactory.create("openPlayerInventory", OpenPlayerInventory.FALSE)
                    .addCategories(RuleCategory.SURVIVAL)
                    .build()
    );

    /**
     * 关闭当前屏幕界面
     */
    public static final RuleAccessor<Boolean> PLAYER_COMMAND_CLOSE_SCREEN = register(
            RuleFactory.create("playerCommandCloseScreen", false)
                    .setCommand()
                    .build()
    );

    /**
     * 真正的和平模式
     */
    public static final RuleAccessor<Boolean> TRUE_PEACEFUL_MODE = register(
            RuleFactory.create("truePeacefulMode", false)
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
                    CarpetOrgAddition.LOGGER.error("{}: {} conflicts with another Carpet extension, disabling rule", CarpetOrgAdditionConstants.MOD_NAME, rule.name());
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
