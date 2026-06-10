package boat.carpetorgaddition.client;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.KeyMapping;

import java.util.List;

public class CarpetOrgAdditionClient implements ClientModInitializer {
    /**
     * 清除高亮路径点的按键绑定
     */
    public static final KeyMapping CLEAR_WAYPOINT = new KeyMapping(LocalizationKeys.Keyboard.WAYPOINT.then("clear").toString(), InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC);

    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        CarpetOrgAdditionClientRegister.register();
        this.logWittyComment();
    }

    private void logWittyComment() {
        String name = ClientUtils.getSession().getName();
        List<String> messages = switch (name) {
            case "qscfthmko099" -> List.of("How did we get here?");
            case "half_kite" -> List.of("Happy birthday!");
            case "MR_LAGANXIANG" -> List.of(
                    // 矿艺：Minecraft的文言译名为“矿艺大典”
                    // https://lzh.minecraft.wiki/w/%E7%A4%A6%E8%97%9D%E5%A4%A7%E5%85%B8
                    "半世萍踪各西东，矿艺成坏两匆匆。",
                    "故纸浮沉同晓梦，异乡风雨误归鸿。",
                    // 南溟：南边的大海，但在这里指澳大利亚
                    "南溟苦旅霜欺鬓，小劫随身笑倚风。",
                    "倦看聚散寻常事，归来独卧雨声中。"
            );
            // 这并非出自《CS:GO》，而是他学Java时写的一个数字炸弹，语法瑕疵是有意为之
            case "qweryyuoskv" -> List.of("The bomb been planted!");
            case "zhaixianyu" -> List.of("Why don't you ever take advice?");
            default -> List.of();
        };
        for (String message : messages) {
            CarpetOrgAddition.LOGGER.info(message);
        }
    }
}
