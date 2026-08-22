package boat.carpetorgaddition.periodic.fakeplayer;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.dataupdate.json.DataUpdater;
import boat.carpetorgaddition.util.IOUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.WorldFormat;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FakePlayerResidents {
    private final WorldFormat worldFormat;
    /**
     * 所有在线的假玩家
     * <p>
     * 服务器关闭时，{@link PlayerList}会在保存玩家数据之前清空，因此使用自定义的集合存储玩家
     */
    private final Map<UUID, EntityPlayerMPFake> players = new HashMap<>();
    @Nullable
    private File file;
    /**
     * 服务器上一次运行时保存的数据
     */
    @Nullable
    private final HashSet<FakePlayerSerializer> previous;
    /**
     * 最多保留的历史版本文件数量
     */
    private static final int MAX_FILE_HISTORY_COUNT = 100;
    private static final String GRAVEYARD = "graveyard";
    private static final String RESIDENT = "resident";
    private static final int CURRENT_VERSION = 3;

    public FakePlayerResidents(MinecraftServer server) {
        this.worldFormat = new WorldFormat(server, PlayerSerializationManager.PLAYER_DATA, GRAVEYARD);
        List<FakePlayerSerializer> list = this.worldFormat.listFiles().stream()
                .filter(WorldFormat.JSON_EXTENSIONS)
                .filter(file -> file.getName().startsWith(RESIDENT + "_"))
                .max((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                .flatMap(file -> {
                    try {
                        JsonObject json = IOUtils.readJson(file);
                        return Optional.of(this.listSerializer(json));
                    } catch (IOException | RuntimeException e) {
                        CarpetOrgAddition.LOGGER.warn("Failed to load fake player resident data from file: {}", file.getAbsolutePath(), e);
                        return Optional.empty();
                    }
                })
                .orElse(List.of());
        this.previous = new HashSet<>(list);
    }

    public void put(EntityPlayerMPFake fakePlayer) {
        this.players.put(fakePlayer.getUUID(), fakePlayer);
    }

    public void remove(EntityPlayerMPFake fakePlayer) {
        this.players.remove(fakePlayer.getUUID());
    }

    public Set<FakePlayerSerializer> get(@Nullable String time) {
        if (time == null) {
            return Objects.requireNonNullElseGet(this.previous, Set::of);
        }
        File file = this.worldFormat.file(this.getFileName(time));
        if (file.isFile()) {
            try {
                JsonObject json = IOUtils.readJson(file);
                return new HashSet<>(this.listSerializer(json));
            } catch (IOException | RuntimeException e) {
                CarpetOrgAddition.LOGGER.warn("Failed to load fake player resident data from file: {}", file.getAbsolutePath(), e);
                return Set.of();
            }
        }
        return Set.of();
    }

    private List<FakePlayerSerializer> listSerializer(JsonObject json) {
        return json.getAsJsonObject("players")
                .entrySet()
                .stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().getAsJsonObject()))
                .map(entry -> new FakePlayerSerializer(entry.getValue(), entry.getKey(), null))
                .toList();
    }

    public List<String> listFileTime() {
        return this.listFiles().stream()
                .map(File::getName)
                .map(name -> name.substring((RESIDENT + "_").length()))
                .map(IOUtils::removeExtension)
                .toList();
    }

    public void onServerSave() {
        try {
            this.saveResidentFakePlayer();
        } catch (RuntimeException e) {
            CarpetOrgAddition.LOGGER.error("Unexpected error encountered while saving fake player resident data: ", e);
        }
    }

    private void saveResidentFakePlayer() {
        if (this.players.isEmpty()) {
            if (this.file != null) {
                IOUtils.removeFileIfExists(this.file);
                this.file = null;
            }
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty(DataUpdater.DATA_VERSION, CURRENT_VERSION);
        JsonObject players = new JsonObject();
        HashSet<FakePlayerSerializer> serializers = new HashSet<>(this.players.values().stream()
                .map(FakePlayerSerializer::new)
                .toList());
        if (serializers.equals(this.previous)) {
            // 文件曾经保存过，发生了状态回退
            if (this.file != null) {
                IOUtils.removeFileIfExists(this.file);
                this.file = null;
            }
            return;
        }
        serializers.forEach(serializer -> players.add(serializer.getName(), serializer.toJson()));
        json.add("players", players);
        String time = ServerUtils.getCurrentSystemTimeFormat();
        String fileName = getFileName(time);
        if (this.file == null) {
            this.file = this.worldFormat.file(fileName);
        } else {
            this.file = IOUtils.renameFile(this.file, fileName);
        }
        try {
            IOUtils.write(this.file, json);
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.error("Unexpected error encountered while saving player data: ", e);
        }
    }

    /**
     * 删除超出限制的文件
     */
    public void cleanupFiles() {
        List<File> list = this.listFiles().stream()
                // 文件名即为创建日期，直接按照文件名排序
                .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                .toList();
        int removeFileCount = list.size() - MAX_FILE_HISTORY_COUNT;
        if (removeFileCount > 0) {
            list.subList(0, removeFileCount).forEach(IOUtils::removeFileIfExists);
        }
    }

    private List<File> listFiles() {
        return this.worldFormat.listFiles().stream()
                .filter(WorldFormat.JSON_EXTENSIONS)
                .filter(file -> file.getName().startsWith(RESIDENT + "_"))
                .toList();
    }

    private String getFileName(String time) {
        return "%s_%s.json".formatted(RESIDENT, time);
    }
}
