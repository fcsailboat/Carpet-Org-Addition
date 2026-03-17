package boat.carpetorgaddition.network.s2c;

import boat.carpetorgaddition.network.NetworkPacketRegister;
import boat.carpetorgaddition.util.ServerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

/**
 * 导航点更新数据包
 *
 */
public final class WaypointUpdateS2CPacket implements CustomPacketPayload {
    private final Vec3 target;
    private final Identifier worldId;
    private final int entityId;
    public static final Type<WaypointUpdateS2CPacket> ID = NetworkPacketRegister.ofType("waypoint_update");
    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointUpdateS2CPacket> CODEC = new StreamCodec<>() {
        @Override
        public WaypointUpdateS2CPacket decode(RegistryFriendlyByteBuf buf) {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            Vec3 target = new Vec3(x, y, z);
            return new WaypointUpdateS2CPacket(target, buf.readIdentifier(), buf.readInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, WaypointUpdateS2CPacket value) {
            Vec3 target = value.getTarget();
            buf.writeDouble(target.x());
            buf.writeDouble(target.y());
            buf.writeDouble(target.z());
            buf.writeIdentifier(value.worldId);
            buf.writeInt(value.entityId);
        }
    };

    /**
     * @param target  导航点的目标
     * @param worldId 导航点所在维度
     */
    public WaypointUpdateS2CPacket(Vec3 target, Identifier worldId, int entityId) {
        this.target = target;
        this.worldId = worldId;
        this.entityId = entityId;
    }

    public WaypointUpdateS2CPacket(Entity entity) {
        Level world = ServerUtils.getWorld(entity);
        Identifier id = ServerUtils.getWorldKey(world).identifier();
        this(ServerUtils.getEyePos(entity), id, entity.getId());
    }

    public WaypointUpdateS2CPacket(BlockPos blockPos, Level world) {
        this(blockPos.getCenter(), ServerUtils.getId(world), -1);
    }

    public Vec3 getTarget() {
        return target;
    }

    public ResourceKey<Level> getWorldKey() {
        return ServerUtils.getWorldKey(this.worldId);
    }

    @SuppressWarnings("unused")
    public int getEntityId() {
        return entityId;
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}