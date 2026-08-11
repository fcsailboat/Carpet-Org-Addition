package boat.carpetorgaddition.periodic.fakeplayer;

import net.minecraft.world.phys.Vec3;

/**
 * {@link FakePlayerPathfinder}的空实现，这是一个单例
 */
public final class DummyPathfinder implements FakePlayerPathfinder {
    public static final DummyPathfinder EMPTY = new DummyPathfinder();

    private DummyPathfinder() {
    }

    @Override
    public void tick() {
    }

    @Override
    public double length() {
        return 0;
    }

    @Override
    public void pathfinding() {
    }

    @Override
    public Vec3 getCurrentNode() {
        return Vec3.ZERO;
    }

    @Override
    public boolean arrivedAtAnyNode() {
        return false;
    }

    @Override
    public boolean backToBeforeNode() {
        return true;
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public void pause(int time) {
    }

    @Override
    public void stop() {
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onStop() {
    }

    @Override
    public boolean isInvalid() {
        return true;
    }

    @Override
    public boolean isInaccessible() {
        return true;
    }

    @Override
    public boolean isMoving() {
        return false;
    }
}
