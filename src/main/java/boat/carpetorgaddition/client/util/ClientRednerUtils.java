package boat.carpetorgaddition.client.util;

import it.unimi.dsi.fastutil.floats.Float2FloatMap;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public class ClientRednerUtils {
    public static Float2FloatMap.Entry worldToScreenPoint(Vec3 worldPos, Matrix4fc modelView, Matrix4f projection) {
        Vec3 cameraPos = ClientUtils.getCamera().position();
        float offsetX = (float) (worldPos.x - cameraPos.x);
        float offsetY = (float) (worldPos.y - cameraPos.y);
        float offsetZ = (float) (worldPos.z - cameraPos.z);
        Vector4f origin = new Vector4f(offsetX, offsetY, offsetZ, 1.0F);
        origin.mul(modelView);
        origin.mul(projection);
        if (origin.w <= 0.0F) {
            return null;
        }
        float ndcX = origin.x / origin.w;
        float ndcY = origin.y / origin.w;
        float ndcZ = origin.z / origin.w;
        if (ndcX < -1 || ndcX > 1 || ndcY < -1 || ndcY > 1 || ndcZ < -1 || ndcZ > 1) {
            return null;
        }
        int width = ClientUtils.getWindowWidth();
        int height = ClientUtils.getWindowHeight();
        return Float2FloatMap.entry((ndcX + 1) / 2 * width, (1 - ndcY) / 2 * height);
    }
}
