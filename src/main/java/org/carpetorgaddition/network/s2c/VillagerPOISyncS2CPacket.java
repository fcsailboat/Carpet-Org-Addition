package org.carpetorgaddition.network.s2c;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.GlobalPos;
import org.carpetorgaddition.CarpetOrgAddition;
import org.jetbrains.annotations.Nullable;

// 村民信息同步数据包
public record VillagerPOISyncS2CPacket(VillagerInfo info) implements CustomPayload {
    private static final Identifier VILLAGER_POI_SYNC = Identifier.of(CarpetOrgAddition.MOD_ID, "villager_poi_sync");
    public static final Id<VillagerPOISyncS2CPacket> ID = new Id<>(VILLAGER_POI_SYNC);
    public static PacketCodec<RegistryByteBuf, VillagerPOISyncS2CPacket> CODEC = new PacketCodec<>() {
        @Override
        public void encode(RegistryByteBuf buf, VillagerPOISyncS2CPacket value) {
            VillagerInfo villagerInfo = value.info;
            buf.writeInt(villagerInfo.geVillagerId());
            if (villagerInfo.getBedPos() == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                buf.writeGlobalPos(villagerInfo.getBedPos());
            }
            if (villagerInfo.getJobSitePos() == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                buf.writeGlobalPos(villagerInfo.getJobSitePos());
            }
            if (villagerInfo.getPotentialJobSite() == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                buf.writeGlobalPos(villagerInfo.getPotentialJobSite());
            }
        }

        @Override
        public VillagerPOISyncS2CPacket decode(RegistryByteBuf buf) {
            VillagerInfo villagerInfo = new VillagerInfo(buf.readInt());
            if (buf.readBoolean()) {
                villagerInfo.setBedPos(buf.readGlobalPos());
            }
            if (buf.readBoolean()) {
                villagerInfo.setJobSitePos(buf.readGlobalPos());
            }
            if (buf.readBoolean()) {
                villagerInfo.setPotentialJobSite(buf.readGlobalPos());
            }
            return new VillagerPOISyncS2CPacket(villagerInfo);
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
