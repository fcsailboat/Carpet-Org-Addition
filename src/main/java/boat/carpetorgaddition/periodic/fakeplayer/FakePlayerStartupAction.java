package boat.carpetorgaddition.periodic.fakeplayer;

import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public interface FakePlayerStartupAction extends Consumer<EntityPlayerMPFake> {
    String VALUE = "value";

    StartupActionType getType();

    Component getDisplayName(LocalizationKey key);

    default JsonObject toJson() {
        JsonObject json = new JsonObject();
        String type = this.getType().name().toLowerCase(Locale.ROOT);
        json.addProperty("type", type);
        return json;
    }

    static FakePlayerStartupAction fromJson(JsonObject json) {
        StartupActionType type = StartupActionType.valueOf(json.get("type").getAsString().toUpperCase(Locale.ROOT));
        String value = json.get(VALUE).getAsString();
        return switch (type) {
            case COMMAND -> CommandAction.of(value);
            case SIMPLE -> SimpleAction.valueOf(value.toUpperCase(Locale.ROOT));
        };
    }

    class CommandAction implements FakePlayerStartupAction {
        public static final FakePlayerStartupAction EMPTY = of("");
        private final String command;

        private CommandAction(String command) {
            this.command = command;
        }

        public static FakePlayerStartupAction of(String command) {
            return new CommandAction(command);
        }

        @Override
        public StartupActionType getType() {
            return StartupActionType.COMMAND;
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = FakePlayerStartupAction.super.toJson();
            json.addProperty(VALUE, this.command);
            return json;
        }

        @Override
        public Component getDisplayName(LocalizationKey key) {
            return key.then("run").translate(this.command);
        }

        @Override
        public void accept(EntityPlayerMPFake fakePlayer) {
            CommandUtils.execute(fakePlayer, this.command);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CommandAction that = (CommandAction) o;
            return Objects.equals(command, that.command);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(command);
        }

        public String getCommand() {
            return this.command;
        }
    }

    enum SimpleAction implements FakePlayerStartupAction {
        USE,
        ATTACK,
        KILL;

        @Override
        public StartupActionType getType() {
            return StartupActionType.SIMPLE;
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = FakePlayerStartupAction.super.toJson();
            json.addProperty(VALUE, this.toString());
            return json;
        }

        @Override
        public void accept(EntityPlayerMPFake fakePlayer) {
            this.function().accept(fakePlayer);
        }

        private Consumer<EntityPlayerMPFake> function() {
            return switch (this) {
                case USE -> fakePlayer -> FakePlayerUtils.click(fakePlayer, InteractionHand.OFF_HAND);
                case ATTACK -> fakePlayer -> FakePlayerUtils.click(fakePlayer, InteractionHand.MAIN_HAND);
                case KILL -> fakePlayer -> fakePlayer.kill(ServerUtils.getWorld(fakePlayer));
            };
        }

        @Override
        public Component getDisplayName(LocalizationKey key) {
            return key.then(this.toString()).translate();
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    enum StartupActionType {
        COMMAND(1),
        SIMPLE(0);
        /**
         * 用于排序动作，使用自定义的序数是为了防止意外的更改枚举字段顺序
         */
        private final int priority;

        StartupActionType(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return this.priority;
        }
    }
}
