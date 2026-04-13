package boat.carpetorgaddition.wheel;

import boat.carpetorgaddition.util.IOUtils;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WorldFormatTest {
    @Test
    public void testJsonElement() {
        JsonObject json = new JsonObject();
        json.addProperty("aaa", "bbb");
        json.addProperty("Number", 100);
        json.addProperty("Number1", 100);
        json.addProperty("Number2", 100.0);
        json.addProperty("Number3", 100F);
        json.addProperty("Boolean", true);
        Assertions.assertEquals("bbb", IOUtils.getJsonElement(json, "aaa", "BBB", String.class));
        Assertions.assertEquals(100, IOUtils.getJsonElement(json, "Number1", 0, Integer.class));
        Assertions.assertEquals(100.0, IOUtils.getJsonElement(json, "Number2", 0.0, Double.class));
        Assertions.assertEquals(100F, IOUtils.getJsonElement(json, "Number3", 0F, Float.class));
        Assertions.assertEquals(true, IOUtils.getJsonElement(json, "Boolean", false, Boolean.class));
        Assertions.assertEquals("BBB", IOUtils.getJsonElement(json, "_aaa", "BBB", String.class));
        Assertions.assertEquals(0, IOUtils.getJsonElement(json, "_Number1", 0, Integer.class));
        Assertions.assertEquals(0, IOUtils.getJsonElement(json, "_Number2", 0, Integer.class));
        Assertions.assertEquals(0, IOUtils.getJsonElement(json, "_Number3", 0, Integer.class));
        Assertions.assertEquals(false, IOUtils.getJsonElement(json, "_Boolean", false, Boolean.class));
        Assertions.assertThrows(IllegalArgumentException.class, () -> IOUtils.getJsonElement(json, "Number", 0, Number.class));
        Assertions.assertNull(IOUtils.getJsonElement(json, "key", null, Object.class));
    }
}
