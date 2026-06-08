package boat.carpetorgaddition.wheel.misc;

import boat.carpetorgaddition.util.ServerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LibrarianVillagerPoiCache {
    private final MinecraftServer server;
    private final Map<ResourceKey<Level>, Map<BlockPos, Villager>> caches = new HashMap<>();

    public LibrarianVillagerPoiCache(MinecraftServer server) {
        this.server = server;
    }

    /**
     * 根据兴趣点获取村民
     */
    public Optional<Villager> getVillager(ServerLevel world, BlockPos blockPos) {
        ResourceKey<Level> worldKey = ServerUtils.getWorldKey(world);
        PoiManager poiManager = world.getPoiManager();
        // 获取目标位置的兴趣点，如果没有兴趣点，则无需查找绑定该工作方块的村民
        Optional<Holder<PoiType>> optional = poiManager.getType(blockPos).filter(type -> type.is(PoiTypes.LIBRARIAN));
        if (optional.isEmpty()) {
            return Optional.empty();
        }
        Map<BlockPos, Villager> cache = this.caches.computeIfAbsent(worldKey, _ -> new HashMap<>());
        Villager villager = cache.get(blockPos);
        if (villager != null) {
            VillagerData villagerData = villager.getVillagerData();
            // 村民必须是图书管理员
            if (villagerData.profession().is(VillagerProfession.LIBRARIAN)) {
                // 村民的工作方块位置必须与缓存位置相同
                boolean valid = villager.getBrain()
                        .getMemory(MemoryModuleType.JOB_SITE)
                        .filter(pos -> pos.equals(new GlobalPos(worldKey, blockPos)))
                        .isPresent();
                if (valid) {
                    return Optional.of(villager);
                }
            }
        }
        this.refreshCache(cache, world, blockPos);
        return Optional.ofNullable(cache.get(blockPos));
    }

    private void refreshCache(Map<BlockPos, Villager> cache, Level world, BlockPos blockPos) {
        cache.clear();
        double range = 128.0;
        AABB box = new AABB(
                blockPos.getX() - range,
                ServerUtils.getMinArchitectureAltitude(world) - 64.0,
                blockPos.getZ() - range,
                blockPos.getX() + range,
                ServerUtils.getMaxArchitectureAltitude(world) + 64.0,
                blockPos.getZ() + range
        );
        List<Villager> list = world.getEntitiesOfClass(Villager.class, box);
        for (Villager villager : list) {
            villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).ifPresent(pos -> cache.put(pos.pos(), villager));
        }
    }
}
