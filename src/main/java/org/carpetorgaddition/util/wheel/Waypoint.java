package org.carpetorgaddition.util.wheel;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.carpetorgaddition.util.constant.TextConstants;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class Waypoint {
    public static final String WAYPOINT = "waypoint";
    private BlockPos blockPos;
    private BlockPos anotherBlockPos;
    private final String dimension;
    private String illustrate;
    private final String creator;

    public final String name;

    public Waypoint(BlockPos blockPos, String name, String dimension, String playerName) {
        this.name = name;
        this.blockPos = blockPos;
        this.dimension = dimension;
        this.creator = playerName;
    }

    @SuppressWarnings("deprecation")
    public Waypoint(Location location, String name) {
        this.name = name;
        this.creator = location.getCreatorPlayerName();
        switch (location.getLocType()) {
            case OVERWORLD -> {
                this.dimension = WorldUtils.OVERWORLD;
                this.blockPos = new BlockPos(location.getOverworld_x(), location.getOverworld_y(), location.getOverworld_z());
            }
            case OVERWORLD_AND_THE_NETHER -> {
                this.dimension = WorldUtils.OVERWORLD;
                this.blockPos = new BlockPos(location.getOverworld_x(), location.getOverworld_y(), location.getOverworld_z());
                this.anotherBlockPos = new BlockPos(location.getThe_nether_x(), location.getThe_nether_y(), location.getThe_nether_z());
            }
            case THE_NETHER -> {
                this.dimension = WorldUtils.THE_NETHER;
                this.blockPos = new BlockPos(location.getThe_nether_x(), location.getThe_nether_y(), location.getThe_nether_z());
            }
            case THE_NETHER_AND_OVERWORLD -> {
                this.dimension = WorldUtils.THE_NETHER;
                this.blockPos = new BlockPos(location.getThe_nether_x(), location.getThe_nether_y(), location.getThe_nether_z());
                this.anotherBlockPos = new BlockPos(location.getOverworld_x(), location.getOverworld_y(), location.getOverworld_z());
            }
            case THE_END -> {
                this.dimension = WorldUtils.THE_END;
                this.blockPos = new BlockPos(location.getThe_end_x(), location.getThe_end_y(), location.getThe_end_z());
            }
            // 不应该会执行到这里
            default -> throw new IllegalStateException("未知的枚举类型:" + location.getLocType());
        }
        if (location.getIllustrate() == null || location.getIllustrate().isEmpty()) {
            // 旧的路径点没有说明文本
            return;
        }
        this.illustrate = location.getIllustrate();
    }

    // 将旧的路径点替换为新的
    @SuppressWarnings({"deprecation", "ResultOfMethodCallIgnored"})
    public static void replaceWaypoint(MinecraftServer server) {
        // 将旧的路径点替换为新的并移动到新位置
        File file = Objects.requireNonNull(server).getSavePath(WorldSavePath.ROOT).resolve("locations").toFile();
        // 创建一个文件用来标记是否已经完成移动
        File flagFile = new File(file, "MOVED");
        if (flagFile.exists()) {
            // 如果这个文件存在，说明路径点在之前已经替换过了，方法之间结束
            return;
        }
        // 文件夹必须存在（如果file.isDirectory()成立，那file.exists()一定也成立）
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    try {
                        // 读取旧的路径点，生成新的路径点json
                        Location location = Location.loadLoc(file, f.getName());
                        Waypoint waypoint = new Waypoint(location, f.getName());
                        waypoint.save(server);
                    } catch (IOException e) {
                        CarpetOrgAddition.LOGGER.warn("路径点[{}]移动失败", IOUtils.removeExtension(f.getName()));
                    }
                }
            }
            try {
                // 替换完后创建一个空白文件用来表示已经完成移动
                flagFile.createNewFile();
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.warn("标记文件创建失败", e);
            }
        }
    }

    // 将路径点写入本地文件
    public void save(MinecraftServer server) throws IOException {
        JsonObject json = new JsonObject();
        // 路径点的坐标
        json.addProperty("x", this.blockPos.getX());
        json.addProperty("y", this.blockPos.getY());
        json.addProperty("z", this.blockPos.getZ());
        // 路径点所在维度
        json.addProperty("dimension", this.dimension);
        // 路径点的创建者
        json.addProperty("creator", this.creator);
        if (this.illustrate != null) {
            // 路径点的说明文本
            json.addProperty("illustrate", this.illustrate);
        }
        if (this.anotherBlockPos != null) {
            // 路径点的另一个路径点坐标
            json.addProperty("another_x", this.anotherBlockPos.getX());
            json.addProperty("another_y", this.anotherBlockPos.getY());
            json.addProperty("another_z", this.anotherBlockPos.getZ());
        }
        WorldFormat worldFormat = new WorldFormat(server, WAYPOINT);
        File file = worldFormat.file(this.name);
        IOUtils.saveJson(file, json);
    }

    // 从本地文件加载一个路径点对象
    public static Optional<Waypoint> load(MinecraftServer server, String name) throws IOException {
        WorldFormat worldFormat = new WorldFormat(server, WAYPOINT);
        File file = worldFormat.getFile(name);
        JsonObject json = IOUtils.loadJson(file);
        BlockPos blockPos;
        // 路径点的三个坐标
        if (IOUtils.jsonHasElement(json, "x", "y", "z")) {
            blockPos = new BlockPos(json.get("x").getAsInt(), json.get("y").getAsInt(), json.get("z").getAsInt());
        } else {
            // 如果文件中读取就路径点中没有这三个坐标，直接返回空
            return Optional.empty();
        }
        // 路径点的维度
        String dimension = IOUtils.jsonHasElement(json, "dimension")
                ? json.get("dimension").getAsString() : WorldUtils.OVERWORLD;
        // 路径点的创建者
        String creator = IOUtils.jsonHasElement(json, "creator")
                ? json.get("creator").getAsString() : "#none";
        Waypoint waypoint = new Waypoint(blockPos, name, dimension, creator);
        // 添加路径点的另一个坐标
        if (IOUtils.jsonHasElement(json, "another_x", "another_z", "another_z")) {
            waypoint.setAnotherBlockPos(new BlockPos(json.get("another_x").getAsInt(),
                    json.get("another_y").getAsInt(), json.get("another_z").getAsInt()));
        }
        // 添加路径点的说明文本
        if (IOUtils.jsonHasElement(json, "illustrate")) {
            waypoint.setIllustrate(json.get("illustrate").getAsString());
        }
        return Optional.of(waypoint);
    }

    // 显示路径点
    public void show(ServerCommandSource source) {
        MutableText text = switch (this.dimension) {
            case WorldUtils.OVERWORLD -> this.anotherBlockPos == null
                    ? TextUtils.translate("carpet.commands.locations.show.overworld",
                    this.formatName(), TextConstants.blockPos(this.blockPos, Formatting.GREEN))
                    : TextUtils.translate("carpet.commands.locations.show.overworld_and_the_nether",
                    this.formatName(), TextConstants.blockPos(this.blockPos, Formatting.GREEN),
                    TextConstants.blockPos(this.anotherBlockPos, Formatting.RED));
            case WorldUtils.THE_NETHER -> this.anotherBlockPos == null
                    ? TextUtils.translate("carpet.commands.locations.show.the_nether",
                    this.formatName(), TextConstants.blockPos(this.blockPos, Formatting.RED))
                    : TextUtils.translate("carpet.commands.locations.show.the_nether_and_overworld",
                    this.formatName(), TextConstants.blockPos(this.blockPos, Formatting.RED),
                    TextConstants.blockPos(this.anotherBlockPos, Formatting.GREEN));
            case WorldUtils.THE_END -> TextUtils.translate("carpet.commands.locations.show.the_end",
                    this.formatName(), TextConstants.blockPos(this.blockPos, Formatting.DARK_PURPLE));
            default -> TextUtils.translate("carpet.commands.locations.show.custom_dimension",
                    this.formatName(), this.dimension, TextConstants.blockPos(this.blockPos, Formatting.GREEN));
        };
        MessageUtils.sendMessage(source, text);
    }

    // 将路径点名称改为带有方括号和悬停样式的文本组件对象
    private Text formatName() {
        String name = "[" + this.name.split("\\.")[0] + "]";
        if (this.illustrate == null) {
            return TextUtils.createText(name);
        }
        return TextUtils.hoverText(name, this.illustrate);
    }

    public void setAnotherBlockPos(BlockPos anotherBlockPos) {
        this.anotherBlockPos = anotherBlockPos;
    }

    public void setIllustrate(String illustrate) {
        if (illustrate == null || illustrate.isEmpty()) {
            this.illustrate = null;
            return;
        }
        this.illustrate = illustrate;
    }

    public void setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public BlockPos getAnotherBlockPos() {
        return anotherBlockPos;
    }

    public String getDimension() {
        return dimension;
    }

    public String getName() {
        return name;
    }

    // 是否可以添加对向坐标
    public boolean canAddAnother() {
        return this.dimension.equals(WorldUtils.OVERWORLD) || this.dimension.equals(WorldUtils.THE_NETHER);
    }
}
