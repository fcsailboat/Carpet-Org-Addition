package boat.carpetorgaddition.dataupdate;

import boat.carpetorgaddition.dataupdate.json.CarpetConfDataUpdater;
import boat.carpetorgaddition.rule.RuleConfig;
import boat.carpetorgaddition.util.IOUtils;
import com.google.gson.JsonObject;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Disabled
public class CarpetConfDataUpdaterTest {
    @Test
    public void testOpenInventoryOptionDataUpdate() {
        @Language("JSON") String oldJson = """
                {
                  "data_version": 1,
                  "rules": {
                    "playerCommandOpenPlayerInventory": "%s"
                  }
                }
                """;
        List<String> list = List.of("fake_player", "online_player", "all_player", "non_whitelist");
        for (String element : list) {
            CarpetConfDataUpdater dataUpdater = CarpetConfDataUpdater.getInstance();
            JsonObject newJson = dataUpdater.update(IOUtils.GSON.fromJson(oldJson.formatted(element), JsonObject.class), 1, 2);
            System.out.println(IOUtils.GSON.toJson(newJson));
            Map<String, String> map = newJson.getAsJsonObject(RuleConfig.RULES)
                    .entrySet()
                    .stream()
                    .map(entry -> Map.entry(entry.getKey(), entry.getValue().getAsString()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Assertions.assertEquals("true", map.get("playerCommandOpenPlayerInventory"));
            Assertions.assertEquals(element, map.get("playerCommandOpenPlayerInventoryOption"));
        }
    }

    @Test
    public void testOpenInventorySwitchDataUpdate() {
        @Language("JSON") String oldJson = """
                {
                  "data_version": 1,
                  "rules": {
                    "playerCommandOpenPlayerInventory": "false"
                  }
                }
                """;
        CarpetConfDataUpdater dataUpdater = CarpetConfDataUpdater.getInstance();
        JsonObject newJson = dataUpdater.update(IOUtils.GSON.fromJson(oldJson, JsonObject.class), 1, 2);
        System.out.println(IOUtils.GSON.toJson(newJson));
        Map<String, String> map = newJson.getAsJsonObject(RuleConfig.RULES)
                .entrySet()
                .stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().getAsString()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Assertions.assertNull(map.get("playerCommandOpenPlayerInventory"));
        Assertions.assertNull(map.get("playerCommandOpenPlayerInventoryOption"));
    }

    @Test
    public void testRuleNameDataUpdate() {
        @Language("JSON") String oldJson = """
                {
                  "data_version": 1,
                  "rules": {
                    "CCEUpdateSuppression": "false",
                    "beaconWorldHeight": "true",
                    "betterTotemOfUndying": "true",
                    "blockDropsDirectlyEnterInventory": "false",
                    "canActivatesObserver": "true",
                    "commandFinder": "true",
                    "commandKillMe": "true",
                    "commandLocations": "true",
                    "commandPlayerAction": "true",
                    "commandPlayerManager": "true",
                    "commandRuleSearch": "true",
                    "commandSpectator": "true",
                    "commandXpTransfer": "true",
                    "disableBatCanSpawn": "true",
                    "fakePlayerCraftPickItemFromShulkerBox": "true",
                    "fakePlayerKeepInventory": "true",
                    "fakePlayerSpawnMemoryLeakFix": "true",
                    "fakePlayerSpawnNoKnockback": "true",
                    "finderCommandMaxFeedbackCount": "15",
                    "fireworkRocketUseCooldown": "true",
                    "flyingUseOnBlockFirework": "true",
                    "limitPhantomSpawn": "true",
                    "maxBlockPlaceDistance": "32.0",
                    "maxBlockPlaceDistanceReferToEntity": "true",
                    "maxBlockPlaceDistanceSyncClient": "false",
                    "openCarpetPermissions": "true",
                    "openSeedPermissions": "true",
                    "openShulkerBoxForcibly": "true",
                    "openVillagerInventory": "true",
                    "peacefulCreeper": "true",
                    "playerCommandOpenPlayerInventory": "all_player",
                    "playerCommandTeleportFakePlayer": "true",
                    "playerDropHead": "true",
                    "quickSettingFakePlayerCraft": "true",
                    "quickShulker": "true",
                    "recordPlayerCommand": "true",
                    "riptideIgnoreWeather": "true",
                    "shulkerBoxStackable": "true",
                    "softDeepslate": "true",
                    "softObsidian": "true",
                    "softOres": "true",
                    "staringEndermanNotAngry": "true",
                    "totemOfUndyingInvincibleTime": "true",
                    "turtleEggFastMine": "true",
                    "villagerHeal": "true",
                    "openTpPermissions": "ops",
                    "openGameRulePermissions": "false"
                  }
                }
                """;
        CarpetConfDataUpdater dataUpdater = CarpetConfDataUpdater.getInstance();
        JsonObject newJson = dataUpdater.update(IOUtils.GSON.fromJson(oldJson, JsonObject.class), 1, 2);
        System.out.println(IOUtils.GSON.toJson(newJson));
        Map<String, String> rules = newJson.getAsJsonObject(RuleConfig.RULES)
                .entrySet()
                .stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().getAsString()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, String> map = Map.ofEntries(
                Map.entry("openSeedPermissions", "openSeedPermission"),
                Map.entry("openCarpetPermissions", "openCarpetPermission"),
                Map.entry("openGameRulePermissions", "openGameRulePermission"),
                Map.entry("openTpPermissions", "openTpPermission")
        );
        for (Map.Entry<String, String> entry : map.entrySet()) {
            Assertions.assertFalse(rules.containsKey(entry.getKey()), entry::getKey);
            Assertions.assertTrue(rules.containsKey(entry.getValue()), entry.getValue());
        }
        Assertions.assertEquals("inventory", rules.get("betterTotemOfUndying"));
    }
}
