package org.carpetorgaddition.periodic.task;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class DrawParticleLineTask extends ServerTask {
    private static final double MAX_DRAW_DISTANCE = Math.pow(128, 2);
    private final ServerWorld world;
    private final ParticleEffect particleEffect;
    private final double distance;
    // 粒子线的起点
    private final Vec3d from;
    // 粒子线延伸的方向
    private final Vec3d vector;
    private Vec3d origin = new Vec3d(0.0, 0.0, 0.0);

    public DrawParticleLineTask(ServerWorld world, ParticleEffect particleEffect, Vec3d from, Vec3d to) {
        this.world = world;
        this.particleEffect = particleEffect;
        this.from = from;
        this.distance = from.squaredDistanceTo(to);
        this.vector = to.subtract(this.from).normalize();
    }

    @Override
    public void tick() {
        // 每一个游戏刻内需要绘制的距离
        double tickDistance = Math.sqrt(distance) / 20;
        tickDistance = tickDistance * MathHelper.clamp(1, tickDistance / 15, 6);
        double sum = 0;
        // 每次绘制0.5格，直到总距离达到每一个游戏刻内需要绘制的距离
        while (sum < tickDistance) {
            this.spawnParticles();
            this.origin = this.origin.add(this.vector.multiply(0.5));
            sum += 0.5;
        }
    }

    // 生成粒子效果
    private void spawnParticles() {
        this.world.spawnParticles(this.particleEffect,
                this.from.x + this.origin.x,
                this.from.y + this.origin.y,
                this.from.z + this.origin.z,
                5, 0, 0, 0, 1);
    }

    @Override
    public boolean stopped() {
        return this.distance <= this.origin.lengthSquared() || this.origin.lengthSquared() >= MAX_DRAW_DISTANCE;
    }

    @Override
    public String getLogName() {
        return "绘制粒子线";
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return this.from.hashCode();
    }
}
