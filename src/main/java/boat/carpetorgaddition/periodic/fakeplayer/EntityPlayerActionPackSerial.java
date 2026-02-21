package boat.carpetorgaddition.periodic.fakeplayer;

import boat.carpetorgaddition.mixin.accessor.carpet.EntityPlayerActionPackAccessor;
import boat.carpetorgaddition.mixin.accessor.carpet.EntityPlayerActionPackAccessor.ActionAccessor;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.helpers.EntityPlayerActionPack.ActionType;
import carpet.patches.EntityPlayerMPFake;
import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class EntityPlayerActionPackSerial {
    @Unmodifiable
    private final Map<ActionType, EntityPlayerActionPack.@NonNull Action> actionMap;
    public static final EntityPlayerActionPackSerial EMPTY = new EntityPlayerActionPackSerial();
    private static final BiMap<ActionType, String> ACTION_KEYS = EnumHashBiMap.create(ActionType.class);

    static {
        Map<ActionType, String> map = Arrays.stream(ActionType.values())
                // 不使用type.name().toLowerCase(Locale.ROOT)，防止Carpet重构影响加载和序列化
                .map(type -> Map.entry(type, switch (type) {
                    case USE -> "use";
                    case ATTACK -> "attack";
                    case JUMP -> "jump";
                    case DROP_ITEM -> "drop_item";
                    case DROP_STACK -> "drop_stack";
                    case SWAP_HANDS -> "swap_hands";
                }))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        ACTION_KEYS.putAll(map);
    }

    private EntityPlayerActionPackSerial() {
        this.actionMap = Map.of();
    }

    public EntityPlayerActionPackSerial(EntityPlayerActionPack actionPack) {
        this.actionMap = ((EntityPlayerActionPackAccessor) actionPack).getActions()
                .entrySet()
                .stream()
                .filter(entry -> !entry.getValue().done)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 从json中反序列化一个对象
     */
    public EntityPlayerActionPackSerial(JsonObject json) {
        EnumMap<ActionType, EntityPlayerActionPack.Action> actions = new EnumMap<>(ActionType.class);
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            ActionType type = ACTION_KEYS.inverse().get(key);
            if (type == null) {
                continue;
            }
            JsonObject data = entry.getValue().getAsJsonObject();
            if (data.get("continuous").getAsBoolean()) {
                // 左键长按
                actions.put(type, EntityPlayerActionPack.Action.continuous());
            } else {
                // 间隔左键
                int interval = data.get("interval").getAsInt();
                actions.put(type, EntityPlayerActionPack.Action.interval(interval));
            }
        }
        this.actionMap = actions;
    }

    /**
     * 设置假玩家动作
     */
    public void startAction(EntityPlayerMPFake fakePlayer) {
        if (this.actionMap.isEmpty()) {
            return;
        }
        EntityPlayerActionPack action = ((ServerPlayerInterface) fakePlayer).getActionPack();
        this.actionMap.forEach(action::start);
    }

    /**
     * （玩家）是否有动作
     */
    public boolean hasAction() {
        return !this.actionMap.isEmpty();
    }

    /**
     * 将动作转换为文本
     */
    public void joinDisplayText(TextJoiner joiner, LocalizationKey key) {
        for (Map.Entry<ActionType, EntityPlayerActionPack.Action> entry : this.actionMap.entrySet()) {
            ActionType type = entry.getKey();
            joiner.newline(key.then(ACTION_KEYS.get(type)).translate());
            joiner.enter(() -> joinDisplayText(key, entry.getValue(), joiner));
        }
    }

    private static void joinDisplayText(LocalizationKey key, EntityPlayerActionPack.Action action, TextJoiner joiner) {
        if (((ActionAccessor) action).isContinuous()) {
            // 长按
            joiner.newline(key.then("continuous").translate());
        } else {
            // 单击
            joiner.newline(key.then("interval").translate(action.interval));
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        for (Map.Entry<ActionType, EntityPlayerActionPack.Action> entry : this.actionMap.entrySet()) {
            ActionType type = entry.getKey();
            String key = ACTION_KEYS.get(type);
            JsonObject data = new JsonObject();
            EntityPlayerActionPack.Action action = entry.getValue();
            data.addProperty("interval", action.interval);
            data.addProperty("continuous", ((ActionAccessor) action).isContinuous());
            json.add(key, data);
        }
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EntityPlayerActionPackSerial that = (EntityPlayerActionPackSerial) o;
        if (this.actionMap.isEmpty() && that.actionMap.isEmpty()) {
            return true;
        }
        if (this.actionMap.size() == that.actionMap.size()) {
            for (Map.Entry<ActionType, EntityPlayerActionPack.Action> entry : this.actionMap.entrySet()) {
                ActionType key = entry.getKey();
                EntityPlayerActionPack.Action value = entry.getValue();
                EntityPlayerActionPack.Action action = that.actionMap.get(key);
                if (action == null) {
                    return false;
                }
                if (action == value) {
                    continue;
                }
                if (action.interval != value.interval) {
                    return false;
                }
                if (((ActionAccessor) action).isContinuous() != ((ActionAccessor) value).isContinuous()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.actionMap.isEmpty()) {
            return 0;
        }
        return this.actionMap.entrySet().stream().mapToInt(entry -> {
            ActionType key = entry.getKey();
            EntityPlayerActionPack.Action value = entry.getValue();
            ActionAccessor accessor = (ActionAccessor) value;
            return Objects.hash(key.hashCode(), value.interval, Boolean.hashCode(accessor.isContinuous()));
        }).sum();
    }
}
