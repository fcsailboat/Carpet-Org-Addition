package boat.carpetorgaddition.network.s2c;

import boat.carpetorgaddition.network.NetworkPacketRegister;
import boat.carpetorgaddition.util.MathUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public final class FakePlayerPathfinderS2CPacket implements CustomPacketPayload {
    public static final Type<FakePlayerPathfinderS2CPacket> ID = NetworkPacketRegister.ofType("fake_player_pathfinder");
    public static final StreamCodec<RegistryFriendlyByteBuf, FakePlayerPathfinderS2CPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf output, FakePlayerPathfinderS2CPacket value) {
            output.writeInt(value.getEntityId());
            output.writeCollection(value.getVec3List(), VEC3D_CODEC);
        }

        @Override
        public FakePlayerPathfinderS2CPacket decode(RegistryFriendlyByteBuf input) {
            int entityId = input.readInt();
            List<Vec3> list = input.readCollection(ArrayList::new, VEC3D_CODEC);
            return new FakePlayerPathfinderS2CPacket(entityId, list);
        }
    };

    private static final StreamCodec<FriendlyByteBuf, Vec3> VEC3D_CODEC = new StreamCodec<>() {
        @Override
        public Vec3 decode(FriendlyByteBuf buf) {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            return new Vec3(x, y, z);
        }

        @Override
        public void encode(FriendlyByteBuf buf, Vec3 value) {
            buf.writeDouble(value.x());
            buf.writeDouble(value.y());
            buf.writeDouble(value.z());
        }
    };
    private final int entityId;
    private final List<Vec3> list;

    private FakePlayerPathfinderS2CPacket(int entityId, List<Vec3> list) {
        this.entityId = entityId;
        this.list = list;
    }

    public static FakePlayerPathfinderS2CPacket of(int entityId, List<Vec3> list) {
        if (list.isEmpty()) {
            return new FakePlayerPathfinderS2CPacket(entityId, List.of());
        }
        ArrayList<Vec3> compressed = new ArrayList<>();
        int start = 0;
        compressed.add(list.getFirst());
        for (int i = 1; i < list.size() - 1; i++) {
            Vec3 first = list.get(start);
            Vec3 second = list.get(i);
            Vec3 third = list.get(i + 1);
            if (MathUtils.collinear(first, second, third) && MathUtils.isInRange(first, third, second)) {
                continue;
            }
            compressed.add(second);
            start = i;
        }
        compressed.add(list.getLast());
        return new FakePlayerPathfinderS2CPacket(entityId, compressed);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public int getEntityId() {
        return this.entityId;
    }

    public List<Vec3> getVec3List() {
        return this.list;
    }
}
