package boat.carpetorgaddition.periodic.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Supplier;

public interface FakePlayerPathfinder {
    FakePlayerPathfinder EMPTY = DummyPathfinder.EMPTY;

    /**
     * @param fakePlayer 当前假玩家的提供者，用于确保玩家在切换维度后还能正常运行
     */
    static FakePlayerPathfinder of(Supplier<EntityPlayerMPFake> fakePlayer, Supplier<Optional<BlockPos>> supplier) {
        return new GeneralPathfinder(fakePlayer, supplier);
    }

    /**
     * 每个游戏刻都调用
     */
    void tick();

    /**
     * @return 路径节点的数据
     */
    double length();

    /**
     * 更新寻路路径
     */
    void pathfinding();

    /**
     * @return 当前节点
     */
    Vec3 getCurrentNode();

    /**
     * @return 玩家是否到达了任意一个节点
     */
    boolean arrivedAtAnyNode();

    /**
     * @return 是否回到了任意一个节点
     */
    boolean backToBeforeNode();

    /**
     * @return 是否到达了目标位置
     */
    boolean isFinished();

    /**
     * 暂停寻路
     *
     * @param time 暂停寻路的时间
     */
    void pause(int time);

    /**
     * 停止寻路
     */
    void stop();

    /**
     * 当寻路开始时调用
     */
    void onStart();

    /**
     * 当寻路停止时调用
     */
    void onStop();

    /**
     * @return 目标位置是否是无效的
     */
    boolean isInvalid();

    /**
     * @return 目标位置是否是不可到达的
     */
    boolean isInaccessible();

    /**
     * @return 玩家是否正在移动
     */
    boolean isMoving();
}
