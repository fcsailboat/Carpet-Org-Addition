package boat.carpetorgaddition.network.handler;

import boat.carpetorgaddition.logger.Loggers;
import boat.carpetorgaddition.network.c2s.LibrarianCommodityQueryC2SPacket;
import boat.carpetorgaddition.network.s2c.LibrarianCommodityResponseS2CPacket;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.misc.LibrarianCommodityEntry;
import carpet.patches.EntityPlayerMPFake;
import com.google.common.collect.MapMaker;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NullMarked;

import java.util.*;

@NullMarked
public class LibrarianCommodityPacketHandler implements ServerPlayNetworking.PlayPayloadHandler<LibrarianCommodityQueryC2SPacket> {
    private final Map<ResourceKey<Level>, Map<BlockPos, Villager>> caches = new HashMap<>();
    public static final Map<ServerPlayer, GlobalPos> PREVIOUS_QUERY_BLOCK_POS = new HashMap<>();

    @Override
    public void receive(LibrarianCommodityQueryC2SPacket payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (player instanceof EntityPlayerMPFake || player instanceof FakePlayer) {
            return;
        }
        if (Loggers.LIBRARIAN.isSubscribed(player)) {
            GlobalPos globalPos = payload.globalPos();
            PREVIOUS_QUERY_BLOCK_POS.put(player, globalPos);
            ResourceKey<Level> key = globalPos.dimension();
            BlockPos blockPos = globalPos.pos();
            MinecraftServer server = ServerUtils.getServer(player);
            ServerLevel world = ServerUtils.getWorld(server, key);
            PoiManager poiManager = world.getPoiManager();
            Optional<Holder<PoiType>> optional = poiManager.getType(blockPos);
            if (optional.isEmpty()) {
                return;
            }
            if (optional.get().is(PoiTypes.LIBRARIAN)) {
                Map<BlockPos, Villager> cache = this.caches.computeIfAbsent(key, _ -> new MapMaker().weakValues().makeMap());
                if (this.getLibrarianCommodity(cache, player, globalPos)) {
                    return;
                }
                this.refreshCache(cache, world, globalPos);
                this.getLibrarianCommodity(cache, player, globalPos);
            }
        }
    }

    private void refreshCache(Map<BlockPos, Villager> cache, ServerLevel world, GlobalPos globalPos) {
        cache.clear();
        BlockPos blockPos = globalPos.pos();
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

    private boolean getLibrarianCommodity(Map<BlockPos, Villager> cache, ServerPlayer player, GlobalPos globalPos) {
        Villager villager = cache.get(globalPos.pos());
        if (villager == null || villager.isRemoved()) {
            return false;
        }
        if (villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) {
            Optional<GlobalPos> optional = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
            if (optional.isEmpty()) {
                return false;
            }
            if (globalPos.equals(optional.get())) {
                this.sendNetworkPacket(player, globalPos.pos(), villager);
                return true;
            }
        }
        return false;
    }

    private void sendNetworkPacket(ServerPlayer player, BlockPos blockPos, Villager villager) {
        List<Map.Entry<Integer, ItemStack>> offers = new ArrayList<>();
        for (MerchantOffer offer : villager.getOffers()) {
            if (offer.getResult().is(Items.ENCHANTED_BOOK)) {
                offers.add(Map.entry(offer.getCostA().getCount(), offer.getResult()));
            }
        }
        LibrarianCommodityEntry entry = new LibrarianCommodityEntry(blockPos, offers);
        PlayerUtils.sendNetworkPacket(player, new LibrarianCommodityResponseS2CPacket(entry));
    }

    public static void cleanupStaleEntries() {
        PREVIOUS_QUERY_BLOCK_POS.entrySet().removeIf(entry -> entry.getKey().isRemoved());
    }
}
