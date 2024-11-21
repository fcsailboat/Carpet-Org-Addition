package org.carpetorgaddition.network.s2c;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.GlobalPos;
import org.carpetorgaddition.network.PacketUtils;
import org.jetbrains.annotations.Nullable;

// 村民信息同步数据包
public record VillagerPoiSyncS2CPacket(VillagerInfo info) implements CustomPayload {
    public static final Id<VillagerPoiSyncS2CPacket> ID = PacketUtils.createId("villager_poi_sync");
    public static PacketCodec<RegistryByteBuf, VillagerPoiSyncS2CPacket> CODEC = new PacketCodec<>() {
        @Override
        public VillagerPoiSyncS2CPacket decode(RegistryByteBuf buf) {
            VillagerInfo villagerInfo = new VillagerInfo(buf.readInt());
            villagerInfo.setBedPos(PacketUtils.readNullable(buf, PacketByteBuf::readGlobalPos));
            villagerInfo.setJobSitePos(PacketUtils.readNullable(buf, PacketByteBuf::readGlobalPos));
            villagerInfo.setPotentialJobSite(PacketUtils.readNullable(buf, PacketByteBuf::readGlobalPos));
            return new VillagerPoiSyncS2CPacket(villagerInfo);
        }

        @Override
        public void encode(RegistryByteBuf buf, VillagerPoiSyncS2CPacket value) {
            VillagerInfo villagerInfo = value.info;
            buf.writeInt(villagerInfo.geVillagerId());
            PacketUtils.writeNullable(villagerInfo.getBedPos(), buf, () -> buf.writeGlobalPos(villagerInfo.getBedPos()));
            PacketUtils.writeNullable(villagerInfo.getJobSitePos(), buf, () -> buf.writeGlobalPos(villagerInfo.getJobSitePos()));
            PacketUtils.writeNullable(villagerInfo.getPotentialJobSite(), buf, () -> buf.writeGlobalPos(villagerInfo.getPotentialJobSite()));
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class VillagerInfo {
        private final int villagerId;
        private @Nullable GlobalPos bedPos;
        private @Nullable GlobalPos potentialJobSite;
        private @Nullable GlobalPos jobSitePos;

        public VillagerInfo(int villagerId) {
            this.villagerId = villagerId;
        }

        public int geVillagerId() {
            return villagerId;
        }

        public @Nullable GlobalPos getBedPos() {
            return bedPos;
        }

        public @Nullable GlobalPos getJobSitePos() {
            return jobSitePos;
        }

        public @Nullable GlobalPos getPotentialJobSite() {
            return this.potentialJobSite;
        }

        public void setBedPos(@Nullable GlobalPos bedPos) {
            this.bedPos = bedPos;
        }

        public void setJobSitePos(@Nullable GlobalPos jobSitePos) {
            this.jobSitePos = jobSitePos;
        }

        public void setPotentialJobSite(@Nullable GlobalPos potentialJobSite) {
            this.potentialJobSite = potentialJobSite;
        }

        public void setGlobalPos(MemoryModuleType<GlobalPos> moduleType, @Nullable GlobalPos globalPos) {
            if (moduleType == MemoryModuleType.HOME) {
                this.bedPos = globalPos;
            } else if (moduleType == MemoryModuleType.JOB_SITE) {
                this.jobSitePos = globalPos;
            } else if (moduleType == MemoryModuleType.POTENTIAL_JOB_SITE) {
                this.potentialJobSite = globalPos;
            }
        }
    }
}
