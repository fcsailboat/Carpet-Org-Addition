package boat.carpetorgaddition.periodic.fakeplayer;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.command.PlayerManagerCommand;
import boat.carpetorgaddition.dataupdate.json.DataUpdater;
import boat.carpetorgaddition.dataupdate.json.FakePlayerSerializerDataUpdater;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionSerializer;
import boat.carpetorgaddition.periodic.task.FakePlayerStartupActionTask;
import boat.carpetorgaddition.periodic.task.ServerTaskManager;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.FakePlayerSpawner;
import boat.carpetorgaddition.wheel.WorldFormat;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import carpet.fakes.ServerPlayerInterface;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

@NullMarked
public class FakePlayerSerializer implements Comparable<FakePlayerSerializer> {
    /**
     * 玩家名称
     */
    private String name;
    /**
     * 注释
     */
    private String comment = "";
    /**
     * 位置
     */
    private Vec3 playerPos;
    /**
     * 偏航角
     */
    private float yaw;
    /**
     * 俯仰角
     */
    private float pitch;
    /**
     * 维度
     */
    private String dimension;
    /**
     * 游戏模式
     */
    private GameType gameMode;
    /**
     * 是否飞行
     */
    private boolean flying;
    /**
     * 是否潜行
     */
    private boolean sneaking;
    /**
     * 是否自动登录
     */
    private boolean autologin = false;
    /**
     * 假玩家简单动作，例如左右键单击、跳跃、丢弃物品等
     */
    private EntityPlayerActionPackSerial simpleAction;
    /**
     * 假玩家自动动作，例如物品合成，自动交易等
     */
    private FakePlayerActionSerializer scriptAction;
    /**
     * 假玩家加入游戏时执行的动作
     */
    private final Map<FakePlayerStartupAction, Integer> startupActions = new HashMap<>();
    /**
     * 玩家所在的组
     */
    private final HashSet<String> groups = new HashSet<>();
    /**
     * 当前对象是否已经修改，即是否需要重新保存
     */
    private boolean isChanged = false;
    @Nullable
    private final File file;
    private final List<Listener> listeners = new ArrayList<>();
    /**
     * 当前数据文件的版本
     */
    private static final int CURRENT_VERSION = 4;

    /**
     * @apiNote 使用此构造方法会丢失玩家所在组，启动时动作等信息
     */
    public FakePlayerSerializer(EntityPlayerMPFake fakePlayer) {
        this.name = PlayerUtils.getName(fakePlayer);
        this.playerPos = ServerUtils.getFootPos(fakePlayer);
        this.yaw = fakePlayer.getYRot();
        this.pitch = fakePlayer.getXRot();
        this.dimension = ServerUtils.getIdAsString(ServerUtils.getWorld(fakePlayer));
        this.gameMode = fakePlayer.gameMode.getGameModeForPlayer();
        this.flying = fakePlayer.getAbilities().flying;
        this.sneaking = fakePlayer.isShiftKeyDown();
        this.simpleAction = new EntityPlayerActionPackSerial(((ServerPlayerInterface) fakePlayer).getActionPack());
        this.scriptAction = new FakePlayerActionSerializer(fakePlayer);
        this.file = new WorldFormat(ServerUtils.getServer(fakePlayer), PlayerSerializationManager.PLAYER_DATA).file(this.name, "json");
    }

    private FakePlayerSerializer(File file) throws IOException {
        JsonObject json = IOUtils.loadJson(file);
        String name = IOUtils.getFileNameWithoutExtension(file);
        this(json, name, file);
    }

    public FakePlayerSerializer(JsonObject oldJson, String name, @Nullable File file) {
        int version = DataUpdater.getVersion(oldJson);
        FakePlayerSerializerDataUpdater dataUpdater = FakePlayerSerializerDataUpdater.getInstance();
        // 需要重新保存吗？这可能会提高下一次读取文件的效率，但是会导致配置文件与低版本不兼容
        JsonObject json = dataUpdater.update(oldJson, version, CURRENT_VERSION);
        // 玩家名
        this.name = name;
        // 玩家位置
        JsonObject pos = json.get("pos").getAsJsonObject();
        this.playerPos = new Vec3(pos.get("x").getAsDouble(), pos.get("y").getAsDouble(), pos.get("z").getAsDouble());
        // 获取朝向
        JsonObject direction = json.get("direction").getAsJsonObject();
        this.yaw = direction.get("yaw").getAsFloat();
        this.pitch = direction.get("pitch").getAsFloat();
        // 维度
        this.dimension = json.get("dimension").getAsString();
        // 游戏模式
        this.gameMode = GameType.byName(json.get("gamemode").getAsString());
        // 是否飞行
        this.flying = json.get("flying").getAsBoolean();
        // 是否潜行
        this.sneaking = json.get("sneaking").getAsBoolean();
        // 是否自动登录
        this.autologin = IOUtils.getJsonElement(json, "autologin", false, Boolean.class);
        // 注释
        JsonElement element = json.get("annotation");
        this.comment = (element == null ? "" : element.getAsString());
        // 假玩家左右手动作
        if (json.has("simple_action")) {
            this.simpleAction = new EntityPlayerActionPackSerial(json.get("simple_action").getAsJsonObject());
        } else {
            this.simpleAction = EntityPlayerActionPackSerial.EMPTY;
        }
        // 假玩家动作，自动合成自动交易等
        if (json.has(PlayerSerializationManager.SCRIPT_ACTION)) {
            JsonObject scriptJson = json.get(PlayerSerializationManager.SCRIPT_ACTION).getAsJsonObject();
            this.scriptAction = new FakePlayerActionSerializer(scriptJson);
        } else {
            this.scriptAction = FakePlayerActionSerializer.NO_ACTION;
        }
        // 玩家组
        if (json.has("group")) {
            JsonArray array = json.getAsJsonArray("group");
            for (JsonElement group : array) {
                // Json null在正常情况下不会出现，这个检查是为了与早期测试时的Json文件兼容
                if (group.isJsonNull()) {
                    continue;
                }
                this.groups.add(group.getAsString());
            }
        }
        // 玩家启动动作
        JsonElement startup = json.get("startup_action");
        if (startup != null && startup.isJsonArray()) {
            JsonArray array = startup.getAsJsonArray();
            try {
                for (JsonElement jsonElement : array) {
                    JsonObject object = jsonElement.getAsJsonObject();
                    int delay = object.has("delay") ? object.get("delay").getAsInt() : 1;
                    JsonObject function = object.get("function").getAsJsonObject();
                    FakePlayerStartupAction action = FakePlayerStartupAction.fromJson(function);
                    this.addStartupFunction(action, delay);
                }
            } catch (RuntimeException e) {
                CarpetOrgAddition.LOGGER.warn("Unable to load startup action for {}", name);
            }
        }
        this.file = file;
    }

    public static FakePlayerSerializer loadFromFile(File file) throws IOException {
        return new FakePlayerSerializer(file);
    }

    public void save() {
        if (this.file == null) {
            throw new IllegalStateException();
        }
        try {
            IOUtils.write(this.file, this.toJson());
        } catch (IOException e) {
            // 译：未能成功保存玩家数据
            CarpetOrgAddition.LOGGER.warn("Failed to successfully save player data", e);
        }
        this.isChanged = false;
    }

    /**
     * @param message 是否显示登录消息
     */
    public boolean spawn(MinecraftServer server, boolean message) {
        // 生成假玩家
        return getSpawner(server).setSilence(!message).spawn();
    }

    public FakePlayerSpawner getSpawner(MinecraftServer server) {
        CommandSourceStack source = server.createCommandSourceStack();
        ServerTaskManager taskManager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        return FakePlayerSpawner.of(server, this.name)
                .setPosition(this.playerPos)
                .setYaw(this.yaw)
                .setPitch(this.pitch)
                .setWorld(ServerUtils.getWorldKey(this.dimension))
                .setGameMode(gameMode)
                .setFlying(this.flying)
                .setSneaking(this.sneaking)
                .setCallback(fakePlayer -> {
                    // 设置玩家动作
                    this.simpleAction.startAction(fakePlayer);
                    this.scriptAction.clearPlayer();
                    this.scriptAction.startAction(fakePlayer);
                    for (Map.Entry<FakePlayerStartupAction, Integer> entry : this.startupActions.entrySet()) {
                        FakePlayerStartupActionTask task = new FakePlayerStartupActionTask(source, fakePlayer, entry.getKey(), entry.getValue());
                        CommandUtils.handlingException(() -> taskManager.addTask(task), source);
                    }
                });
    }

    // 显示文本信息
    public Component info() {
        TextJoiner joiner = new TextJoiner();
        LocalizationKey key = PlayerManagerCommand.KEY.then("info");
        // 玩家名称
        joiner.newline(key.then("name").translate(this.name));
        // 玩家位置
        joiner.newline(key.then("pos").translate(
                MathUtils.formatToMaxTwoDecimals(this.playerPos.x()),
                MathUtils.formatToMaxTwoDecimals(this.playerPos.y()),
                MathUtils.formatToMaxTwoDecimals(this.playerPos.z())
        ));
        // 玩家朝向
        joiner.newline(key.then("direction").translate(
                MathUtils.formatToMaxTwoDecimals(this.yaw),
                MathUtils.formatToMaxTwoDecimals(this.pitch))
        );
        // 维度
        joiner.newline(key.then("dimension").translate(TextProvider.dimension(this.dimension)));
        // 游戏模式
        joiner.newline(key.then("gamemode").translate(this.gameMode.getLongDisplayName()));
        // 是否飞行
        joiner.newline(key.then("flying").translate(TextProvider.getBoolean(this.flying)));
        // 是否潜行
        joiner.newline(key.then("sneaking").translate(TextProvider.getBoolean(this.sneaking)));
        // 是否自动登录
        joiner.newline(key.then("autologin").translate(TextProvider.getBoolean(this.autologin)));
        if (!this.getGroups().isEmpty()) {
            String group = "group";
            if (this.getGroups().size() == 1) {
                joiner.newline(key.then(group).translate(this.getGroups().iterator().next()));
            } else {
                StringJoiner groups = new StringJoiner(", ", "[", "]");
                this.getGroups().forEach(groups::add);
                joiner.newline(key.then(group).translate(groups.toString()));
            }
        }
        boolean hasSimpleAction = this.simpleAction.hasAction();
        boolean hasScriptActions = this.scriptAction.hasAction();
        if (hasSimpleAction || hasScriptActions) {
            joiner.newline(key.then("action").translate());
            if (hasSimpleAction) {
                joiner.enter(() -> this.simpleAction.joinDisplayText(joiner, key));
            }
            if (hasScriptActions) {
                joiner.enter(this.scriptAction.getDisplayName());
            }
        }
        if (!this.startupActions.isEmpty()) {
            LocalizationKey startupKey = key.then("startup");
            joiner.newline(startupKey.translate());
            joiner.enter(() -> {
                List<Map.Entry<FakePlayerStartupAction, Integer>> list = new ArrayList<>(this.startupActions.entrySet());
                list.sort(Comparator.comparingInt(o -> o.getKey().getType().getPriority()));
                list.forEach(entry -> {
                    joiner.newline(entry.getKey().getDisplayName(startupKey));
                    int delay = entry.getValue();
                    if (delay > 1) {
                        joiner.enter(startupKey.then("delay").translate(delay));
                    }
                });
            });
        }
        if (!this.comment.isEmpty()) {
            // 添加注释
            joiner.newline(key.then("comment").translate(this.comment));
        }
        return joiner.join();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(DataUpdater.DATA_VERSION, CURRENT_VERSION);
        // 玩家位置
        JsonObject pos = new JsonObject();
        pos.addProperty("x", this.playerPos.x);
        pos.addProperty("y", this.playerPos.y);
        pos.addProperty("z", this.playerPos.z);
        json.add("pos", pos);
        // 玩家朝向
        JsonObject direction = new JsonObject();
        direction.addProperty("yaw", this.yaw);
        direction.addProperty("pitch", this.pitch);
        json.add("direction", direction);
        // 维度
        json.addProperty("dimension", this.dimension);
        // 游戏模式
        json.addProperty("gamemode", this.gameMode.getName());
        // 是否飞行
        json.addProperty("flying", this.flying);
        // 是否潜行
        json.addProperty("sneaking", this.sneaking);
        // 自动登录
        json.addProperty("autologin", this.autologin);
        // 注释
        json.addProperty("annotation", this.getComment());
        // 添加简单动作
        json.add("simple_action", this.simpleAction.toJson());
        // 添加玩家动作
        json.add(PlayerSerializationManager.SCRIPT_ACTION, this.scriptAction.toJson());
        // 添加启动时动作
        JsonArray startup = new JsonArray();
        for (Map.Entry<FakePlayerStartupAction, Integer> entry : this.startupActions.entrySet()) {
            JsonObject action = new JsonObject();
            action.add("function", entry.getKey().toJson());
            action.addProperty("delay", entry.getValue());
            startup.add(action);
        }
        json.add("startup_action", startup);
        // 添加玩家组
        JsonArray groups = new JsonArray();
        for (String group : this.groups) {
            groups.add(group);
        }
        json.add("group", groups);
        return json;
    }

    /**
     * 更新玩家数据
     */
    public void update(EntityPlayerMPFake fakePlayer) {
        this.name = PlayerUtils.getName(fakePlayer);
        this.playerPos = ServerUtils.getFootPos(fakePlayer);
        this.yaw = fakePlayer.getYRot();
        this.pitch = fakePlayer.getXRot();
        this.dimension = ServerUtils.getIdAsString(ServerUtils.getWorld(fakePlayer));
        this.gameMode = fakePlayer.gameMode.getGameModeForPlayer();
        this.flying = fakePlayer.getAbilities().flying;
        this.sneaking = fakePlayer.isShiftKeyDown();
        this.simpleAction = new EntityPlayerActionPackSerial(((ServerPlayerInterface) fakePlayer).getActionPack());
        this.scriptAction = new FakePlayerActionSerializer(fakePlayer);
        this.isChanged = true;
    }

    // 修改注释
    public void setComment(String comment) {
        this.comment = comment;
        this.isChanged = true;
    }

    // 设置自动登录
    public void setAutologin(boolean autologin) {
        this.autologin = autologin;
        this.isChanged = true;
    }

    /**
     * 将玩家添加到组
     */
    public boolean addToGroup(String group) {
        if (this.groups.add(group)) {
            this.listeners.forEach(listener -> listener.addGroupAfter(group));
            this.isChanged = true;
            return true;
        }
        return false;
    }

    /**
     * 将玩家从组中删除
     *
     * @return 是否删除成功
     */
    public boolean removeFromGroup(String group) {
        if (this.groups.remove(group)) {
            this.listeners.forEach(listener -> listener.removeGroupAfter(group));
            this.isChanged = true;
            return true;
        }
        return false;
    }

    // 获取玩家名
    public String getName() {
        return this.name;
    }

    // 获取显示名称
    public Component getDisplayName() {
        return TextBuilder.of(this.name).setHover(this.info()).build();
    }

    /**
     * {@code list}子命令中，每一行显示的内容
     */
    public Supplier<Component> line() {
        return () -> {
            String name = this.getName();
            String logonCommand = CommandProvider.playerManagerSpawn(name);
            String logoutCommand = CommandProvider.killFakePlayer(name);
            Component login = TextBuilder.of("[↑]")
                    .setCommand(logonCommand)
                    .setHover(LocalizationKeys.Button.LOGIN.translate())
                    .setColor(ChatFormatting.GREEN)
                    .build();
            Component logout = TextBuilder.of("[↓]")
                    .setCommand(logoutCommand)
                    .setHover(LocalizationKeys.Button.LOGOUT.translate())
                    .setColor(ChatFormatting.RED)
                    .build();
            Component info = TextBuilder.of("[?]")
                    .setHover(this.info())
                    .setColor(ChatFormatting.GRAY)
                    .build();
            TextJoiner joiner = new TextJoiner();
            joiner.append(login)
                    .space()
                    .append(logout)
                    .space()
                    .append(info)
                    .space()
                    .append(name);
            if (!this.comment.isEmpty()) {
                TextBuilder builder = TextBuilder.of("    // " + this.getComment());
                builder.setGrayItalic();
                joiner.append(builder.build());
            }
            return joiner.join();
        };
    }

    public String getComment() {
        return this.comment;
    }

    /**
     * @return 是否自动登录
     */
    public boolean isAutologin() {
        return this.autologin;
    }

    public boolean isChanged() {
        return this.isChanged;
    }

    @Nullable
    public File getFile() {
        return this.file;
    }

    @Unmodifiable
    public Set<String> getGroups() {
        return Collections.unmodifiableSet(this.groups);
    }

    public void addStartupFunction(FakePlayerStartupAction action, int delay) {
        if (delay == -1) {
            this.startupActions.remove(action);
        } else {
            this.startupActions.put(action, delay);
        }
        this.isChanged = true;
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    /**
     * @return 是否可以通过{@code list}的过滤器
     */
    public boolean match(String filter) {
        if (filter.isEmpty()) {
            return true;
        }
        String lowerCase = filter.toLowerCase(Locale.ROOT);
        if (this.name.toLowerCase(Locale.ROOT).contains(lowerCase)) {
            return true;
        }
        return this.comment.toLowerCase(Locale.ROOT).contains(lowerCase);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FakePlayerSerializer that = (FakePlayerSerializer) o;
        return Float.compare(yaw, that.yaw) == 0
               && Float.compare(pitch, that.pitch) == 0
               && flying == that.flying
               && sneaking == that.sneaking
               && autologin == that.autologin
               && Objects.equals(name, that.name)
               && Objects.equals(comment, that.comment)
               && Objects.equals(playerPos, that.playerPos)
               && Objects.equals(dimension, that.dimension)
               && gameMode == that.gameMode
               && Objects.equals(simpleAction, that.simpleAction)
               && Objects.equals(scriptAction, that.scriptAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, comment, playerPos, yaw, pitch, dimension, gameMode, flying, sneaking, autologin, simpleAction, scriptAction);
    }

    @Override
    public int compareTo(FakePlayerSerializer o) {
        return String.CASE_INSENSITIVE_ORDER.compare(this.name, o.name);
    }

    public boolean remove() {
        if (this.file == null) {
            return true;
        }
        return this.file.delete();
    }

    public List<String> listCommandStartupFunction() {
        return this.startupActions.keySet().stream()
                .filter(startup -> startup instanceof FakePlayerStartupAction.CommandAction)
                .map(startup -> (FakePlayerStartupAction.CommandAction) startup)
                .map(FakePlayerStartupAction.CommandAction::getCommand)
                .toList();

    }

    public interface Listener {
        void addGroupAfter(String group);

        void removeGroupAfter(String group);
    }
}
