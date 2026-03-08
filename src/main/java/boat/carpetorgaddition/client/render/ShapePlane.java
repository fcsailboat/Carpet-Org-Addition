package boat.carpetorgaddition.client.render;

import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.List;

public record ShapePlane(Vector3f vertex1, Vector3f vertex2, Vector3f vertex3, Vector3f vertex4) {
    public static List<ShapePlane> ofPlaces(AABB box) {
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        return List.of(
                // 南面
                new ShapePlane(new Vector3f(minX, minY, minZ), new Vector3f(maxX, minY, minZ), new Vector3f(maxX, maxY, minZ), new Vector3f(minX, maxY, minZ)),
                // 北面
                new ShapePlane(new Vector3f(minX, minY, maxZ), new Vector3f(minX, maxY, maxZ), new Vector3f(maxX, maxY, maxZ), new Vector3f(maxX, minY, maxZ)),
                // 东面
                new ShapePlane(new Vector3f(minX, minY, minZ), new Vector3f(minX, maxY, minZ), new Vector3f(minX, maxY, maxZ), new Vector3f(minX, minY, maxZ)),
                // 西面
                new ShapePlane(new Vector3f(maxX, minY, minZ), new Vector3f(maxX, minY, maxZ), new Vector3f(maxX, maxY, maxZ), new Vector3f(maxX, maxY, minZ)),
                // 底面
                new ShapePlane(new Vector3f(minX, minY, minZ), new Vector3f(maxX, minY, minZ), new Vector3f(maxX, minY, maxZ), new Vector3f(minX, minY, maxZ)),
                // 顶面
                new ShapePlane(new Vector3f(minX, maxY, minZ), new Vector3f(maxX, maxY, minZ), new Vector3f(maxX, maxY, maxZ), new Vector3f(minX, maxY, maxZ))
        );
    }
}
