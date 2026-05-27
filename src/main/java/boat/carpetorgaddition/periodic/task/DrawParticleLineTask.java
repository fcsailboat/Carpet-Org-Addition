package boat.carpetorgaddition.periodic.task;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("unused")
public class DrawParticleLineTask extends ServerTask {
    private static final double MAX_DRAW_DISTANCE = Math.pow(128, 2);
    private final ServerLevel world;
    private final ParticleOptions particleEffect;
    private final double distance;
    // 粒子线的起点
    private final Vec3 from;
    // 粒子线延伸的方向
    private final Vec3 vector;
    private Vec3 origin = new Vec3(0.0, 0.0, 0.0);

    public DrawParticleLineTask(CommandSourceStack source, ServerLevel world, ParticleOptions particleEffect, Vec3 from, Vec3 to) {
        super(source);
        this.world = world;
        this.particleEffect = particleEffect;
        this.from = from;
        this.distance = from.distanceToSqr(to);
        this.vector = to.subtract(this.from).normalize();
    }

    @Override
    public void tick() {
        // 每一个游戏刻内需要绘制的距离
        double tickDistance = Math.sqrt(distance) / 20;
        tickDistance = tickDistance * Mth.clamp(1, tickDistance / 15, 6);
        double sum = 0;
        // 每次绘制0.5格，直到总距离达到每一个游戏刻内需要绘制的距离
        while (sum < tickDistance) {
            this.spawnParticles();
            this.origin = this.origin.add(this.vector.scale(0.5));
            sum += 0.5;
        }
    }

    // 生成粒子效果
    private void spawnParticles() {
        this.world.sendParticles(this.particleEffect,
                this.from.x + this.origin.x,
                this.from.y + this.origin.y,
                this.from.z + this.origin.z,
                5, 0, 0, 0, 1);
    }

    @Override
    public boolean stopped() {
        return this.distance <= this.origin.lengthSqr() || this.origin.lengthSqr() >= MAX_DRAW_DISTANCE;
    }
}
