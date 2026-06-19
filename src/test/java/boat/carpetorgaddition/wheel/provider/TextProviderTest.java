package boat.carpetorgaddition.wheel.provider;

import carpet.CarpetSettings;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TextProviderTest {
    @BeforeAll
    public static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CarpetSettings.language = "zh_cn";
    }

    @Test
    public void testTickToTime() {
        Assertions.assertEquals("10游戏刻", TextProvider.tickToTime(10L).getString());
        Assertions.assertEquals("1秒", TextProvider.tickToTime(20L).getString());
        Assertions.assertEquals("10秒", TextProvider.tickToTime(200L).getString());
        Assertions.assertEquals("1分钟", TextProvider.tickToTime(20L * 60L).getString());
        Assertions.assertEquals("1分10秒", TextProvider.tickToTime((20L * 60L) + (20L * 10L)).getString());
        Assertions.assertEquals("1小时", TextProvider.tickToTime(20L * 60L * 60L).getString());
        Assertions.assertEquals("1小时", TextProvider.tickToTime((20L * 60L * 60L) + 200L).getString());
        Assertions.assertEquals("1小时", TextProvider.tickToTime(72030L).getString());
        Assertions.assertEquals("1小时20分", TextProvider.tickToTime((20L * 60L * 60L) + (20L * 60L * 20L)).getString());
        Assertions.assertEquals("1小时20分", TextProvider.tickToTime((20L * 60L * 60L) + (20L * 60L * 20L) + (40L * 20L)).getString());
    }
}
