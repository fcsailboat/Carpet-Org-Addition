package boat.carpetorgaddition.wheel;

import boat.carpetorgaddition.command.LocationsCommand;
import boat.carpetorgaddition.dataupdate.json.DataUpdater;
import boat.carpetorgaddition.dataupdate.json.WaypointDataUpdater;
import boat.carpetorgaddition.util.IOUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys.Dimension;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Waypoint {
    public static final String WAYPOINT = "waypoint";
    private static final int CURRENT_VERSION = 3;
    private BlockPos blockPos;
    @Nullable
    private BlockPos anotherBlockPos;
    private final Level world;
    @NotNull
    private MetaComment comment = new MetaComment();
    private final String creator;
    private final MinecraftServer server;
    private final String name;

    public Waypoint(BlockPos blockPos, String name, Level world, String creator, MinecraftServer server) {
        this.name = name;
        this.blockPos = blockPos;
        this.world = world;
        this.creator = creator;
        this.server = server;
    }

    public Waypoint(BlockPos blockPos, String name, ServerPlayer player) {
        this(blockPos, name, ServerUtils.getWorld(player), ServerUtils.getPlayerName(player), ServerUtils.getServer(player));
    }

    // 将路径点写入本地文件
    public void save() throws IOException {
        JsonObject json = new JsonObject();
        // 数据版本
        json.addProperty(DataUpdater.DATA_VERSION, CURRENT_VERSION);
        // 路径点的坐标
        JsonObject pos = new JsonObject();
        pos.addProperty("x", this.blockPos.getX());
        pos.addProperty("y", this.blockPos.getY());
        pos.addProperty("z", this.blockPos.getZ());
        json.add("pos", pos);
        // 路径点所在维度
        json.addProperty("dimension", this.getWorldAsString());
        // 路径点的创建者
        json.addProperty("creator", this.creator);
        // 路径点的注释
        json.addProperty("comment", this.comment.getComment());
        // 路径点的另一个路径点坐标
        JsonObject anotherPos = new JsonObject();
        if (this.anotherBlockPos != null) {
            anotherPos.addProperty("x", this.anotherBlockPos.getX());
            anotherPos.addProperty("y", this.anotherBlockPos.getY());
            anotherPos.addProperty("z", this.anotherBlockPos.getZ());
        }
        json.add("another_pos", anotherPos);
        WorldFormat worldFormat = new WorldFormat(this.server, WAYPOINT);
        File file = worldFormat.file(this.name + IOUtils.JSON_EXTENSION);
        IOUtils.write(file, json);
    }

    // 从本地文件加载一个路径点对象
    public static Waypoint load(MinecraftServer server, String name) throws IOException {
        WorldFormat worldFormat = new WorldFormat(server, WAYPOINT);
        File file = worldFormat.file(name, IOUtils.JSON_EXTENSION);
        JsonObject json = IOUtils.loadJson(file);
        WaypointDataUpdater dataUpdater = WaypointDataUpdater.getInstance();
        json = dataUpdater.update(json, DataUpdater.getVersion(json));
        // 路径点的位置
        JsonObject pos = json.get("pos").getAsJsonObject();
        int x = pos.get("x").getAsInt();
        int y = pos.get("y").getAsInt();
        int z = pos.get("z").getAsInt();
        BlockPos blockPos = new BlockPos(x, y, z);
        // 路径点的维度
        String dimension = IOUtils.jsonHasElement(json, "dimension") ? json.get("dimension").getAsString() : ServerUtils.OVERWORLD;
        // 路径点的创建者
        String creator = IOUtils.jsonHasElement(json, "creator") ? json.get("creator").getAsString() : "#none";
        ServerLevel world = ServerUtils.getWorld(server, dimension);
        Waypoint waypoint = new Waypoint(blockPos, name, world, creator, server);
        // 添加路径点的另一个坐标
        if (json.has("another_pos")) {
            JsonObject anotherPos = json.get("another_pos").getAsJsonObject();
            if (IOUtils.jsonHasElement(anotherPos, "x", "y", "z")) {
                int anotherX = anotherPos.get("x").getAsInt();
                int anotherY = anotherPos.get("y").getAsInt();
                int anotherZ = anotherPos.get("z").getAsInt();
                waypoint.setAnotherBlockPos(new BlockPos(anotherX, anotherY, anotherZ));
            }
        }
        // 添加路径点的说明文本
        waypoint.setComment(json.get("comment").getAsString());
        return waypoint;
    }

    // 显示路径点
    public Component line() {
        LocalizationKey where = LocationsCommand.KEY.then("where");
        String worldId = this.getWorldAsString();
        if (this.anotherBlockPos == null) {
            Map.Entry<Component, ChatFormatting> entry = switch (worldId) {
                case ServerUtils.OVERWORLD -> Map.entry(Dimension.OVERWORLD.translate(), ChatFormatting.GREEN);
                case ServerUtils.THE_NETHER -> Map.entry(Dimension.THE_NETHER.translate(), ChatFormatting.RED);
                case ServerUtils.THE_END -> Map.entry(Dimension.THE_END.translate(), ChatFormatting.DARK_PURPLE);
                default -> Map.entry(TextBuilder.create(worldId), ChatFormatting.GREEN);
            };
            return where.translate(this.formatName(), entry.getKey(), TextProvider.blockPos(this.blockPos, entry.getValue()));
        } else {
            LocalizationKey cross = where.then("cross");
            return switch (worldId) {
                case ServerUtils.OVERWORLD -> cross.translate(
                        this.formatName(),
                        Dimension.OVERWORLD.translate(),
                        TextProvider.blockPos(this.blockPos, ChatFormatting.GREEN),
                        Dimension.THE_NETHER.translate(),
                        TextProvider.blockPos(this.anotherBlockPos, ChatFormatting.RED)
                );
                case ServerUtils.THE_NETHER -> cross.translate(
                        this.formatName(),
                        Dimension.THE_NETHER.translate(),
                        TextProvider.blockPos(this.blockPos, ChatFormatting.RED),
                        Dimension.OVERWORLD.translate(),
                        TextProvider.blockPos(this.anotherBlockPos, ChatFormatting.GREEN)
                );
                case ServerUtils.THE_END -> where.translate(
                        this.formatName(),
                        Dimension.THE_END.translate(),
                        TextProvider.blockPos(this.blockPos, ChatFormatting.DARK_PURPLE)
                );
                default -> where.translate(
                        this.formatName(),
                        TextBuilder.create(worldId),
                        TextProvider.blockPos(this.blockPos, ChatFormatting.GREEN)
                );
            };
        }
    }

    // 将路径点名称改为带有方括号和悬停样式的文本组件对象
    private Component formatName() {
        TextBuilder builder = TextBuilder.of("[" + this.name.split("\\.")[0] + "]");
        if (this.comment.isEmpty()) {
            return builder.build();
        }
        builder.setHover(this.comment.getText());
        return builder.build();
    }

    public void setAnotherBlockPos(@Nullable BlockPos anotherBlockPos) {
        this.anotherBlockPos = anotherBlockPos;
    }

    public void setComment(String comment) {
        this.comment = comment == null || comment.isEmpty() ? new MetaComment() : new MetaComment(comment);
    }

    public void setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public @Nullable BlockPos getAnotherBlockPos() {
        return this.anotherBlockPos;
    }

    public Level getWorld() {
        return this.world;
    }

    public String getWorldAsString() {
        return ServerUtils.getWorldIdAsString(this.world);
    }

    public String getName() {
        return name;
    }

    // 是否可以添加对向坐标
    public boolean canAddAnother() {
        return this.world.dimension() == Level.OVERWORLD || this.world.dimension() == Level.NETHER;
    }
}
