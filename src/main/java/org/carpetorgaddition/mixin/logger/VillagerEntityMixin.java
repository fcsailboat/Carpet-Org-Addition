package org.carpetorgaddition.mixin.logger;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.GlobalPos;
import org.carpetorgaddition.logger.LoggerNames;
import org.carpetorgaddition.logger.LoggerRegister;
import org.carpetorgaddition.logger.NetworkPacketLogger;
import org.carpetorgaddition.network.s2c.VillagerPOISyncS2CPacket;
import org.carpetorgaddition.network.s2c.VillagerPOISyncS2CPacket.VillagerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {
    @Unique
    private final VillagerEntity thisVillager = (VillagerEntity) (Object) this;

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        if (LoggerRegister.villager && thisVillager.getWorld().getTime() % 20 == 0 && thisVillager.getServer() != null) {
            NetworkPacketLogger logger = (NetworkPacketLogger) LoggerNames.getLogger(LoggerNames.VILLAGER);
            logger.sendPacket(string -> {
                VillagerInfo villagerInfo = new VillagerInfo(thisVillager.getId());
                for (MemoryModuleType<GlobalPos> type : getList(string)) {
                    thisVillager.getBrain()
                            .getOptionalRegisteredMemory(type)
                            .ifPresent(globalPos -> villagerInfo.setGlobalPos(type, globalPos));
                }
                return new VillagerPOISyncS2CPacket(villagerInfo);
            });
        }
    }

    // 获取要同步的兴趣点类型
    @Unique
    private Set<MemoryModuleType<GlobalPos>> getList(String option) {
        if (option == null || "all".equals(option)) {
            return Set.of(MemoryModuleType.HOME, MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE);
        }
        List<String> options = Arrays.stream(option.split("[,，]")).toList();
        HashSet<MemoryModuleType<GlobalPos>> set = new HashSet<>();
        for (String str : options) {
            switch (str) {
                case "bed" -> set.add(MemoryModuleType.HOME);
                case "jobSitePos" -> set.add(MemoryModuleType.JOB_SITE);
                case "potentialJobSite" -> set.add(MemoryModuleType.POTENTIAL_JOB_SITE);
            }
        }
        return set;
    }
}
