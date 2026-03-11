package boat.carpetorgaddition.dataupdate.nbt;

import boat.carpetorgaddition.util.ServerUtils;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.fixes.References;

import java.util.Optional;

public abstract class NbtDataUpdater {
    protected final MinecraftServer server;
    protected final DataFixer fixerUpper;
    /**
     * {@code Carpet Org Addition}的数据版本
     */
    public static final String DATA_VERSION = "data_version";
    /**
     * {@code Minecraft}的数据版本
     */
    public static final String MINECRAFT_DATA_VERSION = "minecraft_data_version";
    /**
     * 当前{@code Minecraft}的NBT数据版本
     */
    public static final int CURRENT_MINECRAFT_DATA_VERSION = ServerUtils.getMinecraftDataVersion();

    public NbtDataUpdater(MinecraftServer server) {
        this.server = server;
        this.fixerUpper = this.server.getFixerUpper();
    }

    public final CompoundTag update(CompoundTag oldNbt, int version, int targetVersion) {
        CompoundTag newNbt = this.updateDataFormat(oldNbt, version);
        int minecraftVersion = this.getMinecraftNbtVersion(oldNbt);
        CompoundTag result = this.updateMinecraftDataFormat(newNbt, minecraftVersion);
        if (NbtDataUpdater.getVersion(result) != targetVersion) {
            throw new IllegalStateException("Nbt has not been updated to the target version");
        }
        return result;
    }

    protected abstract CompoundTag updateDataFormat(CompoundTag old, int version);

    protected abstract CompoundTag updateMinecraftDataFormat(CompoundTag old, int version);

    protected Tag updateItemStack(Tag nbt, int version) {
        Dynamic<Tag> input = new Dynamic<>(NbtOps.INSTANCE, nbt);
        Dynamic<Tag> result = this.fixerUpper.update(References.ITEM_STACK, input, version, CURRENT_MINECRAFT_DATA_VERSION);
        return result.getValue();
    }

    public static int getVersion(CompoundTag nbt) {
        return nbt.getIntOr("data_version", 0);
    }

    protected int getMinecraftNbtVersion(CompoundTag nbt) {
        Optional<Integer> optional = nbt.getInt(NbtDataUpdater.MINECRAFT_DATA_VERSION).or(() -> nbt.getInt("NbtDataVersion"));
        return optional.orElse(-1);
    }
}
