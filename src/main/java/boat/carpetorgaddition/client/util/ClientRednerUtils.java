package boat.carpetorgaddition.client.util;

import it.unimi.dsi.fastutil.floats.Float2FloatMap;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public class ClientRednerUtils {
    public static Float2FloatMap.Entry worldToScreenPoint(Vec3 worldPos, Matrix4fc modelView, Matrix4f projection) {
        Vec3 cameraPos = ClientUtils.getCamera().position();
        double offsetX = (worldPos.x - cameraPos.x);
        double offsetY = (worldPos.y - cameraPos.y);
        double offsetZ = (worldPos.z - cameraPos.z);
        double distance = worldPos.distanceTo(cameraPos);
        if (distance > 1000.0) {
            double scale = 1000.0 / distance;
            offsetX = offsetX * scale;
            offsetY = offsetY * scale;
            offsetZ = offsetZ * scale;
        }
        Vector4f origin = new Vector4f((float) offsetX, (float) offsetY, (float) offsetZ, 1.0F);
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
