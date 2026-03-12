package boat.carpetorgaddition.dataupdate.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;

public final class CarpetConfDataUpdater extends DataUpdater {
    /**
     * 所有规则，截至{@code 2025年6月19日}
     */
    @Unmodifiable
    public static final List<String> OLD_VERSION_RULES = List.of(
            "commandItemShadowing",
            "setBedrockHardness",
            "bindingCurseInvalidation",
            "disableOpenOrWaterDetection",
            "creativeImmuneKill",
            "flyingUseOnBlockFirework",
            "staringEndermanNotAngry",
            "farmlandPreventStepping",
            "maxBlockPlaceDistance",
            "simpleUpdateSkipper",
            "channelingIgnoreWeather",
            "notDamageEnderPearl",
            "disableDamageImmunity",
            "disableBatCanSpawn",
            "turtleEggFastHatch",
            "openShulkerBoxForcibly",
            "villagerInfiniteTrade",
            "fireworkRocketUseCooldown",
            "riptideIgnoreWeather",
            "pickaxeMinedBedrock",
            "villagerHeal",
            "fakePlayerHeal",
            "maxBlockPlaceDistanceReferToEntity",
            "knockbackStick",
            "disableRespawnBlocksExplode",
            "CCEUpdateSuppression",
            "openSeedPermissions",
            "openCarpetPermissions",
            "openGameRulePermissions",
            "openVillagerInventory",
            "peacefulCreeper",
            "commandXpTransfer",
            "commandSpectator",
            "commandFinder",
            "commandKillMe",
            "commandLocations",
            "healthNotFullCanEat",
            "canMineSpawner",
            "fakePlayerSpawnNoKnockback",
            "canActivatesObserver",
            "disableWaterFreezes",
            "fakePlayerCraftKeepItem",
            "commandParticleLine",
            "disableMobPeacefulDespawn",
            "climbingBoat",
            "reusableSmithingTemplate",
            "openTpPermissions",
            "softDeepslate",
            "softObsidian",
            "softOres",
            "betterTotemOfUndying",
            "commandPlayerAction",
            "fakePlayerCraftPickItemFromShulkerBox",
            "customPiglinBarteringTime",
            "quickSettingFakePlayerCraft",
            "fakePlayerKeepInventory",
            "commandCreeper",
            "commandRuleSearch",
            "superChargedCreeper",
            "playerDropHead",
            "beaconRangeExpand",
            "beaconWorldHeight",
            "canHighlightBlockPos",
            "commandPlayerManager",
            "blockDropsDirectlyEnterInventory",
            "turtleEggFastMine",
            "commandNavigate",
            "playerDropsNotDespawning",
            "fakePlayerMaxCraftCount",
            "fakePlayerSpawnMemoryLeakFix",
            "commandMail",
            "suppressionMismatchInDestroyBlockPosWarn",
            "syncNavigateWaypoint",
            "shulkerBoxStackable",
            "maxBlockPlaceDistanceSyncClient",
            "limitPhantomSpawn",
            "applyToolEffectsImmediately",
            "forceRestock",
            "autoSyncPlayerStatus",
            "recordPlayerCommand",
            "protectionEnchantmentCompatible",
            "damageEnchantmentCompatible",
            "finderCommandMaxFeedbackCount",
            "totemOfUndyingInvincibleTime",
            "playerCommandOpenPlayerInventory",
            "playerCommandTeleportFakePlayer",
            "experienceOrbMerge",
            "quickShulker",
            "disableCreativeContainerDrops"
    );

    private static final CarpetConfDataUpdater INSTANCE = new CarpetConfDataUpdater();

    private CarpetConfDataUpdater() {
    }

    public static CarpetConfDataUpdater getInstance() {
        return INSTANCE;
    }

    @Override
    protected JsonObject update(JsonObject oldJson, int version) {
        return switch (version) {
            case 0, 1 -> {
                JsonObject newJson = new JsonObject();
                newJson.addProperty(DataUpdater.DATA_VERSION, 2);
                JsonObject newRules = new JsonObject();
                for (Map.Entry<String, JsonElement> entry : oldJson.getAsJsonObject("rules").entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue().getAsString();
                    switch (key) {
                        case "playerCommandOpenPlayerInventory" -> {
                            if (!"false".equals(value)) {
                                newRules.addProperty("playerCommandOpenPlayerInventoryOption", value);
                                newRules.addProperty("playerCommandOpenPlayerInventory", "true");
                            }
                        }
                        case "betterTotemOfUndying" -> {
                            String newValue = switch (value) {
                                case "true" -> "inventory";
                                case "false" -> "vanilla";
                                case "shulker_box" -> "inventory_with_shulker_box";
                                default -> null;
                            };
                            if (newValue != null) {
                                newRules.addProperty(key, newValue);
                            }
                        }
                        default -> {
                            String newKey = switch (key) {
                                case "openSeedPermissions" -> "openSeedPermission";
                                case "openCarpetPermissions" -> "openCarpetPermission";
                                case "openGameRulePermissions" -> "openGameRulePermission";
                                case "openTpPermissions" -> "openTpPermission";
                                case "fakePlayerCraftKeepItem" -> "fakePlayerActionKeepItem";
                                case "fakePlayerCraftPickItemFromShulkerBox" -> "fakePlayerShulkerBoxItemHandling";
                                case "fakePlayerMaxCraftCount" -> "fakePlayerMaxItemOperationCount";
                                case "finderCommandMaxFeedbackCount" -> "maxLinesPerPage";
                                default -> key;
                            };
                            newRules.addProperty(newKey, value);
                        }
                    }
                }
                newJson.add("rules", newRules);
                yield this.update(newJson, 2);
            }
            case 2 -> {
                JsonObject newJson = new JsonObject();
                newJson.addProperty(DataUpdater.DATA_VERSION, 3);
                JsonObject newRules = new JsonObject();
                for (Map.Entry<String, JsonElement> entry : oldJson.getAsJsonObject("rules").entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue().getAsString();
                    if ("openShulkerBoxForcibly".equals(key)) {
                        newRules.addProperty("forceOpenContainer", Boolean.parseBoolean(value) ? "shulker_box" : "false");
                    } else {
                        newRules.addProperty(key, value);
                    }
                }
                newJson.add("rules", newRules);
                yield this.update(newJson, 3);
            }
            case 3 -> {
                JsonObject newJson = new JsonObject();
                newJson.addProperty(DataUpdater.DATA_VERSION, 4);
                JsonObject newRules = new JsonObject();
                for (Map.Entry<String, JsonElement> entry : oldJson.getAsJsonObject("rules").entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue().getAsString();
                    switch (key) {
                        case "channelingIgnoreWeather" ->
                                newRules.addProperty("channelingIgnoreConditions", "true".equals(value) ? "ignore_weather_and_sky" : value);
                        case "riptideIgnoreWeather" -> newRules.addProperty("riptideIgnoreConditions", value);
                        default -> newRules.addProperty(key, value);
                    }
                }
                newJson.add("rules", newRules);
                yield this.update(newJson, 4);
            }
            default -> oldJson;
        };
    }
}
