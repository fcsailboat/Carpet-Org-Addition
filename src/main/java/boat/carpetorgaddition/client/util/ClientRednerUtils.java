package boat.carpetorgaddition.client.util;

import it.unimi.dsi.fastutil.floats.Float2FloatMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public class ClientRednerUtils {
    public static Float2FloatMap.Entry worldToScreenPoint(Vec3 worldPos, Matrix4fc modelView, Matrix4f projection) {
        Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        float offX = (float) (worldPos.x - camPos.x);
        float offY = (float) (worldPos.y - camPos.y);
        float offZ = (float) (worldPos.z - camPos.z);
        Vector4f origin = new Vector4f(offX, offY, offZ, 1.0F);
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
        int width = ClientUtils.getClient().getWindow().getWidth();
        int height = ClientUtils.getClient().getWindow().getHeight();
        return Float2FloatMap.entry((ndcX + 1) / 2 * width, (1 - ndcY) / 2 * height);
    }
}
