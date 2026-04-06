package boat.carpetorgaddition.client;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.KeyMapping;

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
        String massage = switch (name) {
            case "qscfthmko099" -> "How did we get here?";
            case "half_kite" -> "Happy birthday!";
            case "MR_LAGANXIANG" -> "You're back!!!";
            case "qweryyuoskv" -> "The bomb been planted!";
            case "zhaixianyu" -> "Why don't you ever take advice?";
            default -> null;
        };
        if (massage != null) {
            CarpetOrgAddition.LOGGER.info(massage);
        }
    }
}
