package boat.carpetorgaddition.wheel;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.dataupdate.json.DataUpdater;
import boat.carpetorgaddition.util.IOUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.players.NameAndId;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * <p>
 * 使用{@code /finder item 物品ID from offline_player}命令查找离线玩家的物品时，
 * 玩家数据是以UUID的形式保存的，但是召唤假玩家需要的是玩家名，因此就需要根据玩家UUID
 * 找到对应的玩家名。
 * </p>
 * <p>
 * 在游戏根目录下，有一个{@code usercache.json}文件，虽然可以通过它来获取对应的玩家名称，
 * 但是，这个文件只会保存1000名玩家，长时间没有上线的玩家可能会被覆盖，因此需要一个类来记录
 * 所有的映射关系，保证不常上线的玩家名也能获取。
 * </p>
 * <p>
 * Mojang提供了根据玩家UUID获取玩家名称的API，但是这对服务器中的非正版玩家无能为力，
 * 并且如果找不到名称的玩家狠多，如果每个都去查询，Mojang也不会同时处理这么多的请求。
 * </p>
 * <p>
 * 本类用于记录每个上线过的玩家UUID与玩家名称的对应关系，它在所有世界中共享，但是并不能彻底解决以上问题：
 * <ul>
 * <li>只能记录安装本Mod之后上线的玩家，不能记录之前的。</li>
 * <li>玩家可能更改他们的名称。</li>
 * </ul>
 * </p>
 * <p>
 * 综上所述，本类不是{@code usercache.json}的替代，而是该文件中找不到玩家名时的备用方案。
 * </p>
 *
 * @see <a href="https://zh.minecraft.wiki/w/玩家档案缓存存储格式">玩家档案缓存存储格式</a>
 */
public class GameProfileCache {
    /**
     * 一个只有一个线程并且阻塞队列为空的线程池，用于根据玩家UUID查询玩家名称
     */
    public static final ThreadPoolExecutor QUERY_PLAYER_NAME_THREAD_POOL = new ThreadPoolExecutor(
            0,
            1,
            60,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            GameProfileCache::createDaemonThread,
            new ThreadPoolExecutor.AbortPolicy()
    );
    private static final String CONFIG_NAME_NAME = "profile";
    private static final int CURRENT_VERSION = 3;
    private final File config = IOUtils.configFile(CONFIG_NAME_NAME + ".json");
    private final Table table = new Table();
    /**
     * 集合可能被多个线程同时访问
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    /**
     * 自上次保存以来，数据是否发生了变化
     */
    private boolean changed = false;
    /**
     * Mojang提供的根据玩家UUID查询玩家名的API
     */
    public static final String MOJANG_API = "https://api.minecraftservices.com/minecraft/profile/lookup/%s";
    private static volatile GameProfileCache INSTANCE;

    public static GameProfileCache getInstance() {
        if (INSTANCE == null) {
            synchronized (GameProfileCache.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GameProfileCache();
                }
            }
        }
        return INSTANCE;
    }

    private GameProfileCache() {
        this.migration();
        this.load();
        this.mergeUsercache();
    }

    /**
     * 通过Mojang API查询玩家名称
     */
    public static String queryPlayerNameFromMojangApi(UUID uuid) throws IOException {
        String result = openMojangApiConnection(uuid);
        // 解析json字符串
        JsonObject json = IOUtils.GSON.fromJson(result, JsonObject.class);
        if (json.has("name")) {
            return json.get("name").getAsString();
        }
        throw new IllegalStateException("Player name not returned");
    }

    /**
     * 连接到{@code Mojang API}
     */
    private static String openMojangApiConnection(UUID uuid) throws IOException {
        URL url;
        try {
            URI uri = new URI(MOJANG_API.formatted(uuid.toString()));
            url = uri.toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalStateException("Invalid URL constructed from UUID: " + uuid, e);
        }
        // 连接到Mojang API
        URLConnection connection = url.openConnection();
        InputStream input = connection.getInputStream();
        try (input) {
            return IOUtils.readInputStreamAsString(input);
        }
    }

    /**
     * 创建守护线程
     */
    private static Thread createDaemonThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        // 设置守护线程
        thread.setDaemon(true);
        thread.setName("Query-Player-Name");
        return thread;
    }

    /**
     * 根据UUID获取对应的玩家名
     */
    public Optional<String> get(UUID uuid) {
        try {
            this.lock.readLock().lock();
            return this.table.get(uuid);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public Optional<GameProfile> getGameProfile(UUID uuid) {
        Optional<String> optional = this.get(uuid);
        return optional.map(name -> new GameProfile(uuid, name));
    }

    public Optional<NameAndId> getPlayerConfigEntry(UUID uuid) {
        return this.getGameProfile(uuid).map(NameAndId::new);
    }

    /**
     * 根据玩家名称获取玩家UUID<br>
     * 如果玩家名称与缓存中的某个玩家名大小写完全匹配，则返回这个名称大小写完全相同的玩家档案<br>
     * 如果没有完全匹配的，但是有仅大小写不同的玩家，则返回其中一个玩家档案<br>
     * 否则返回空的{@code Optional}
     *
     * @param name 玩家的名称
     */
    public Optional<GameProfile> getGameProfile(@NonNull String name) {
        try {
            this.lock.readLock().lock();
            Optional<Map.Entry<UUID, String>> optional = this.table.get(name);
            return optional.map(value -> new GameProfile(value.getKey(), value.getValue()));
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public void put(GameProfile gameProfile) {
        this.put(gameProfile.id(), gameProfile.name());
    }

    public void put(NameAndId entry) {
        this.put(entry.id(), entry.name());
    }

    /**
     * 将玩家UUID与玩家名称的对应关系添加至集合
     */
    public void put(UUID uuid, String name) {
        try {
            this.lock.writeLock().lock();
            this.table.put(uuid, name);
            this.changed = true;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * 将{@code usercache.json}合并到{@code profile.json}
     */
    private void mergeUsercache() {
        try {
            JsonArray array = IOUtils.loadJson(IOUtils.USERCACHE_JSON, JsonArray.class);
            Set<Map.Entry<String, String>> set = array.asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .map(json -> Map.entry(json.get("name").getAsString(), json.get("uuid").getAsString()))
                    .collect(Collectors.toSet());
            for (Map.Entry<String, String> entry : set) {
                String name = entry.getKey();
                UUID uuid = UUID.fromString(entry.getValue());
                this.put(uuid, name);
            }
        } catch (RuntimeException | IOException e) {
            CarpetOrgAddition.LOGGER.warn("Unable to merge usercahce.json into profile.json", e);
        }
    }

    private void migration() {
        if (this.config.isFile()) {
            return;
        }
        // 迁移配置文件
        File file = IOUtils.configFile("uuid_name_mapping.txt");
        if (file.isFile()) {
            try {
                BufferedReader reader = IOUtils.toReader(file);
                try (reader) {
                    migration(reader);
                    changed = true;
                }
                // 在弃用旧文件之前保存文件，避免后续可能因服务器未正常关闭而无法触发保存
                save();
                // 弃用旧的文件
                IOUtils.deprecatedFile(file);
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.error("Unable to migrate uuid_name_mapping.txt to profile.json", e);
            }
        }
    }

    private void migration(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String[] split = line.split("=");
            if (split.length != 2) {
                continue;
            }
            UUID uuid;
            try {
                uuid = UUID.fromString(split[0].strip());
            } catch (IllegalArgumentException e) {
                continue;
            }
            String name = split[1].strip();
            if (name.isEmpty()) {
                continue;
            }
            try {
                this.lock.writeLock().lock();
                this.table.put(uuid, name);
            } finally {
                this.lock.writeLock().unlock();
            }
        }
    }

    private void load() {
        if (this.config.isFile()) {
            try {
                JsonObject json = IOUtils.loadJson(this.config);
                JsonArray array = json.getAsJsonArray("usercache");
                for (JsonElement element : array) {
                    UUID uuid;
                    String name;
                    try {
                        JsonObject entry = element.getAsJsonObject();
                        uuid = UUID.fromString(entry.get("uuid").getAsString());
                        name = entry.get("name").getAsString();
                    } catch (RuntimeException e) {
                        continue;
                    }
                    try {
                        this.lock.writeLock().lock();
                        this.table.put(uuid, name);
                    } finally {
                        this.lock.writeLock().unlock();
                    }
                }
            } catch (NullPointerException | JsonParseException | IOException e) {
                CarpetOrgAddition.LOGGER.error("Unable to read the mapping table between player UUID and name from the file", e);
            }
        }
    }

    /**
     * 将玩家UUID与名称的映射表写入本地文件
     */
    public void save() {
        if (this.changed) {
            JsonObject json = new JsonObject();
            json.addProperty(DataUpdater.DATA_VERSION, CURRENT_VERSION);
            JsonArray array = new JsonArray();
            try {
                this.lock.readLock().lock();
                Set<Map.Entry<UUID, String>> entries = this.table.entrySet();
                for (Map.Entry<UUID, String> entry : entries) {
                    JsonObject profile = new JsonObject();
                    profile.addProperty("uuid", entry.getKey().toString());
                    profile.addProperty("name", entry.getValue());
                    array.add(profile);
                }
            } finally {
                this.lock.readLock().unlock();
            }
            json.addProperty("count", array.size());
            json.add("usercache", array);
            try {
                IOUtils.write(this.config, json);
                this.changed = false;
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.error("Unable to write the mapping table between player UUID and name to the file", e);
            }
        }
        if (CarpetOrgAdditionConstants.isEnableHiddenFunction()) {
            this.saveToCsv();
        }
    }

    /**
     * 将玩家名和UUID映射再保存一份到csv表格文件
     *
     * @apiNote 该文件不是配置文件，设置文件为只读是为了提醒玩家不去修改，修改该文件不会影响游戏读取真正的配置文件
     */
    private void saveToCsv() {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Name,UUID");
        for (Map.Entry<UUID, String> entry : this.table.entrySet()) {
            joiner.add("%s,%s".formatted(entry.getValue(), entry.getKey()));
        }
        String csv = joiner.toString();
        saveToCsv(CONFIG_NAME_NAME + ".csv", csv, StandardCharsets.UTF_8);
        // 使用GBK编码再保存一份
        // Windows下直接用Excel打开中UTF-8编码的csv文件会导致中文部分乱码
        saveToCsv(CONFIG_NAME_NAME + "(GBK).csv", csv, Charset.forName("GBK"));
    }

    private void saveToCsv(String fileName, String csv, Charset charset) {
        File file = IOUtils.configFile(fileName);
        if (file.isFile()) {
            // 写入文件前取消文件只读
            if (!(file.canWrite() || file.setWritable(true))) {
                CarpetOrgAddition.LOGGER.warn("Failed to set {} as writable", fileName);
                return;
            }
        }
        try {
            IOUtils.write(file, csv, charset);
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.error("Unable to write the mapping table between player UUID and name to the {}", fileName, e);
        }
        // 设置文件为只读
        if (!file.setReadOnly()) {
            CarpetOrgAddition.LOGGER.warn("Failed to set {} as read-only", fileName);
        }
    }

    public static class Table {
        private final HashMap<String, HashSet<String>> usernames = new HashMap<>();
        private final BiMap<UUID, String> map = HashBiMap.create();

        /**
         * 根据玩家名称获取玩家UUID，如果缓存中存在大小写变体，返回名称最接近。
         */
        @VisibleForTesting
        public Optional<Map.Entry<UUID, String>> get(String name) {
            if (name == null) {
                throw new IllegalArgumentException("Null as a parameter");
            }
            Set<String> set = this.usernames.get(name.toLowerCase(Locale.ROOT));
            if (set == null || set.isEmpty()) {
                return Optional.empty();
            }
            // 优先获取大小写最接近的
            String actualName = set.stream().min(createComparator(name)).orElseThrow();
            UUID uuid = this.map.inverse().get(actualName);
            if (uuid == null) {
                return Optional.empty();
            }
            return Optional.of(Map.entry(uuid, actualName));
        }

        @VisibleForTesting
        public Optional<String> get(UUID uuid) {
            return Optional.ofNullable(this.map.get(uuid));
        }

        @VisibleForTesting
        public void put(UUID uuid, String name) {
            String lowerCase = name.toLowerCase(Locale.ROOT);
            Set<String> set = this.usernames.get(lowerCase);
            if (set == null) {
                HashSet<String> names = new HashSet<>();
                names.add(name);
                this.usernames.put(lowerCase, names);
            } else {
                set.add(name);
            }
            this.map.forcePut(uuid, name);
        }

        /**
         * 更接近目标名称书写形式的字符串排在前面
         */
        private Comparator<String> createComparator(String name) {
            return (o1, o2) -> {
                if (o1.equals(o2)) {
                    return 0;
                }
                if (o1.equalsIgnoreCase(o2)) {
                    for (int i = 0; i < name.length(); i++) {
                        if (o1.charAt(i) == o2.charAt(i)) {
                            continue;
                        }
                        if (o1.charAt(i) == name.charAt(i)) {
                            return -1;
                        }
                        return 1;
                    }
                }
                throw new IllegalArgumentException();
            };
        }

        private void remove(String name) {
            String lowerCase = name.toLowerCase(Locale.ROOT);
            Set<String> set = this.usernames.get(lowerCase);
            if (set == null) {
                return;
            }
            if (set.remove(name)) {
                if (set.isEmpty()) {
                    this.usernames.remove(lowerCase);
                }
                this.map.inverse().remove(name);
            }
        }

        @SuppressWarnings("unused")
        private void remove(UUID uuid) {
            String name = this.map.get(uuid);
            if (name == null) {
                return;
            }
            this.remove(name);
        }

        private Set<Map.Entry<UUID, String>> entrySet() {
            return this.map.entrySet();
        }
    }
}
