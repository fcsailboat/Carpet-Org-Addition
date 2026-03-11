package boat.carpetorgaddition.dataupdate.nbt;

import boat.carpetorgaddition.util.ServerUtils;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtFormatException;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.fixes.References;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

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

    public CompoundTag update(CompoundTag nbt, int version, int vanillaVersion) {
        CompoundTag newNbt = this.updateDataFormat(nbt, version);
        return this.updateVanillaDataFormat(newNbt, vanillaVersion);
    }

    @MustBeInvokedByOverriders
    protected CompoundTag updateDataFormat(CompoundTag old, int version) {
        Optional<Integer> optional = old.getInt(DATA_VERSION);
        if (optional.isEmpty()) {
            throw new NbtFormatException("Missing nbt version information");
        }
        return old;
    }

    @MustBeInvokedByOverriders
    protected CompoundTag updateVanillaDataFormat(CompoundTag old, int version) {
        Optional<Integer> optional = old.getInt(MINECRAFT_DATA_VERSION);
        if (optional.isEmpty()) {
            throw new NbtFormatException("Missing minecraft nbt version information");
        }
        return old;
    }

    protected Tag updateItemStack(Tag nbt, int version) {
        Dynamic<Tag> input = new Dynamic<>(NbtOps.INSTANCE, nbt);
        Dynamic<Tag> result = this.fixerUpper.update(References.ITEM_STACK, input, version, CURRENT_MINECRAFT_DATA_VERSION);
        return result.getValue();
    }
}
