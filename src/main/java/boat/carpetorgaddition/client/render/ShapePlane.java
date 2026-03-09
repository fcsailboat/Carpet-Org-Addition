package boat.carpetorgaddition.client.render;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record ShapePlane(Vec3 vertex1, Vec3 vertex2, Vec3 vertex3, Vec3 vertex4) {
    public static List<ShapePlane> ofPlaces(AABB box) {
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        return List.of(
                // 南面
                new ShapePlane(new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ), new Vec3(maxX, maxY, minZ), new Vec3(minX, maxY, minZ)),
                // 北面
                new ShapePlane(new Vec3(minX, minY, maxZ), new Vec3(minX, maxY, maxZ), new Vec3(maxX, maxY, maxZ), new Vec3(maxX, minY, maxZ)),
                // 东面
                new ShapePlane(new Vec3(minX, minY, minZ), new Vec3(minX, maxY, minZ), new Vec3(minX, maxY, maxZ), new Vec3(minX, minY, maxZ)),
                // 西面
                new ShapePlane(new Vec3(maxX, minY, minZ), new Vec3(maxX, minY, maxZ), new Vec3(maxX, maxY, maxZ), new Vec3(maxX, maxY, minZ)),
                // 底面
                new ShapePlane(new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ), new Vec3(maxX, minY, maxZ), new Vec3(minX, minY, maxZ)),
                // 顶面
                new ShapePlane(new Vec3(minX, maxY, minZ), new Vec3(maxX, maxY, minZ), new Vec3(maxX, maxY, maxZ), new Vec3(minX, maxY, maxZ))
        );
    }
}
