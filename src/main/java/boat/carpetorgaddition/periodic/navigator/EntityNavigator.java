package boat.carpetorgaddition.periodic.navigator;

import boat.carpetorgaddition.command.NavigatorCommand;
import boat.carpetorgaddition.network.s2c.WaypointUpdateS2CPacket;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class EntityNavigator extends AbstractNavigator {
    /**
     * 此导航器追踪的实体
     */
    @NonNull
    private Entity entity;
    /**
     * 该导航器是否在玩家到达目的地后仍继续导航
     */
    private final boolean isContinue;
    /**
     * 上一个坐标
     */
    @NonNull
    private Vec3 prevPos;
    /**
     * 上一个维度
     */
    @NonNull
    private Level prevWorld;

    public EntityNavigator(ServerPlayer player, Entity entity, boolean isContinue) {
        super(player);
        this.entity = Objects.requireNonNull(entity);
        this.isContinue = isContinue;
        this.prevPos = ServerUtils.getEyePos(entity);
        this.prevWorld = ServerUtils.getWorld(entity);
    }

    @Override
    public void tick() {
        if (this.isFailure()) {
            // 如果目标实体死亡，就清除玩家的追踪器
            MessageUtils.sendMessageToHud(this.player, NavigatorCommand.HUD.then("target_death").translate());
            this.clear();
            return;
        }
        Level world = ServerUtils.getWorld(this.entity);
        Component text;
        if (ServerUtils.getWorld(this.player).equals(world)) {
            // 获取翻译后的文本信息
            Component in = IN.translate(entity.getName(), TextProvider.simpleBlockPos(entity.blockPosition()));
            int distance = MathUtils.getBlockIntegerDistance(player.blockPosition(), entity.blockPosition());
            // 添加上下箭头
            Vec3 eyePos = ServerUtils.getEyePos(this.entity);
            text = getHUDText(eyePos, in, distance);
        } else {
            Component dimension = TextProvider.dimension(ServerUtils.getWorld(this.entity));
            Component pos = TextProvider.simpleBlockPos(entity.blockPosition());
            text = IN.translate(entity.getName(), TextBuilder.combineAll(dimension, pos));
        }
        MessageUtils.sendMessageToHud(this.player, text);
        this.syncWaypoint(false);
        this.prevPos = ServerUtils.getEyePos(this.entity);
        this.prevWorld = ServerUtils.getWorld(this.entity);
    }

    @Override
    protected WaypointUpdateS2CPacket createPacket() {
        return new WaypointUpdateS2CPacket(this.entity);
    }

    @Override
    protected boolean updateRequired() {
        return !(this.prevPos.equals(ServerUtils.getEyePos(this.entity)) && this.prevWorld.equals(ServerUtils.getWorld(this.entity)));
    }

    /**
     * @return 此导航器是否需要停止
     */
    @Override
    protected boolean isArrive() {
        if (this.isContinue) {
            return false;
        }
        if (ServerUtils.getWorld(this.player).equals(ServerUtils.getWorld(this.entity))
            && MathUtils.getBlockDistance(player.blockPosition(), entity.blockPosition()) <= 8) {
            // 停止追踪
            MessageUtils.sendMessageToHud(this.player, REACH.translate());
            this.clear();
            return true;
        }
        return false;
    }

    /**
     * 目标实体是否死亡或被清除
     *
     * @return 是否需要停止追踪这个实体
     */
    private boolean isFailure() {
        return switch (this.entity) {
            case ServerPlayer corpse -> {
                if (corpse.isRemoved()) {
                    // 如果目标实体是玩家，并且玩家已被删除
                    // 就从服务器的玩家管理器中查找新的玩家实体对象，如果找到了，设置目标为新玩家，如果找不到，玩家的追踪器对象不变
                    // 只要这个玩家在线，就不需要清除这个追踪器，因为玩家可以复活
                    UUID uuid = corpse.getUUID();
                    Optional<ServerPlayer> optional = ServerUtils.getPlayer(this.server, uuid);
                    if (optional.isEmpty()) {
                        // 如果玩家已经下线，返回true
                        yield true;
                    }
                    this.entity = optional.get();
                }
                yield false;
            }
            case LivingEntity corpse -> corpse.isDeadOrDying() || isFailure(corpse);
            case Entity corpse -> isFailure(corpse);
        };
    }

    private boolean isFailure(Entity corpse) {
        Entity.RemovalReason removalReason = corpse.getRemovalReason();
        // 生物已死亡，或者生物被不可逆的清除
        if (removalReason != null && removalReason.shouldDestroy()) {
            return true;
        }
        // 目标实体被可逆的清除，就尝试在服务器重新找到目标实体
        // 如果找到，重新设置玩家的追踪实体对象
        // 否则，以实体消失的位置为目标继续导航，并在下一个游戏刻继续查找
        UUID uuid = corpse.getUUID();
        Optional<Entity> optional = ServerUtils.getEntity(this.server, uuid);
        optional.ifPresent(value -> this.entity = value);
        return false;
    }

    @Override
    public EntityNavigator copy(ServerPlayer player) {
        return new EntityNavigator(player, this.entity, this.isContinue);
    }
}
