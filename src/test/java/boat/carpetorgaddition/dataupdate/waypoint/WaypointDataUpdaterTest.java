package boat.carpetorgaddition.dataupdate.waypoint;

import boat.carpetorgaddition.dataupdate.json.DataUpdater;
import boat.carpetorgaddition.dataupdate.json.WaypointDataUpdater;
import boat.carpetorgaddition.util.IOUtils;
import com.google.gson.JsonObject;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class WaypointDataUpdaterTest {
    @Disabled
    @Test
    public void testDataUpdate() {
        @Language("JSON") String oldWaypoint = """
                {
                  "x": 1,
                  "y": 2,
                  "z": 3,
                  "dimension": "minecraft:overworld",
                  "creator": "Steve",
                  "illustrate": "测试",
                  "another_x": 8,
                  "another_y": 16,
                  "another_z": 24
                }
                """;
        JsonObject json = IOUtils.GSON.fromJson(oldWaypoint, JsonObject.class);
        WaypointDataUpdater dataUpdater = WaypointDataUpdater.getInstance();
        JsonObject update = dataUpdater.update(json, DataUpdater.getVersion(json), 1);
        // 路径点坐标已被合并
        Assertions.assertNull(update.get("x"));
        Assertions.assertNull(update.get("y"));
        Assertions.assertNull(update.get("z"));
        Assertions.assertNotNull(update.get("pos"));
        // 维度和创建者保持不变
        Assertions.assertNotNull(update.get("dimension"));
        Assertions.assertNotNull(update.get("creator"));
        // illustrate已被重命名为comment
        Assertions.assertNull(update.get("illustrate"));
        Assertions.assertNotNull(update.get("comment"));
        // 路径点的另一个坐标已被合并
        Assertions.assertNull(update.get("another_x"));
        Assertions.assertNull(update.get("another_y"));
        Assertions.assertNull(update.get("another_z"));
        @Language("JSON") String newWaypoint = """
                {
                  "pos": {
                      "x": 1,
                      "y": 2,
                      "z": 3
                  },
                  "dimension": "minecraft:overworld",
                  "creator": "Steve",
                  "comment": "测试",
                  "another_pos": {
                      "x": 8,
                      "y": 16,
                      "z": 24
                  },
                  "data_version": 2
                }
                """;
        JsonObject newJson = IOUtils.GSON.fromJson(newWaypoint, JsonObject.class);
        Assertions.assertEquals(update, newJson);
    }
}
