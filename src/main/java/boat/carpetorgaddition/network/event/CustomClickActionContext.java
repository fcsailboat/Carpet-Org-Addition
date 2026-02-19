package boat.carpetorgaddition.network.event;

import boat.carpetorgaddition.wheel.nbt.NbtReader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;

public class CustomClickActionContext {
    private final MinecraftServer server;
    /**
     * 发送自定义单击动作的玩家
     */
    private final ServerPlayer player;
    private final CommandSourceStack serverCommandSource;
    /**
     * 自定义动作携带的负载
     */
    private final NbtReader reader;
    /**
     * 当前自定义动作是通过什么发送的
     */
    private final ActionSource actionSource;
    public static final ScopedValue<ServerPlayer> CURRENT_PLAYER = ScopedValue.newInstance();
    public static final ScopedValue<ActionSource> ACTION_SOURCE = ScopedValue.newInstance();

    public CustomClickActionContext(MinecraftServer server, ServerPlayer player, NbtReader reader) {
        this.server = server;
        this.player = player;
        this.serverCommandSource = player.createCommandSourceStack();
        this.reader = reader;
        if (reader == null) {
            this.actionSource = ActionSource.UNKNOWN;
        } else {
            Optional<ActionSource> optional = reader.getActionSourceNullable("action_source");
            this.actionSource = optional.orElseGet(() -> ACTION_SOURCE.isBound() ? ACTION_SOURCE.get() : ActionSource.UNKNOWN);
        }
    }

    public CustomClickActionContext(MinecraftServer server, ServerPlayer player, Tag tag) {
        NbtReader reader = tag instanceof CompoundTag nbt ? new NbtReader(server, nbt) : null;
        this(server, player, reader);
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    public NbtReader getReader() {
        return Objects.requireNonNull(this.reader, "Nbt Reader is null");
    }

    public CommandSourceStack getSource() {
        return this.serverCommandSource;
    }

    public ActionSource getActionSource() {
        return this.actionSource;
    }
}
