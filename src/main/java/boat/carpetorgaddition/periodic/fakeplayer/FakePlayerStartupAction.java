package boat.carpetorgaddition.periodic.fakeplayer;

import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public interface FakePlayerStartupAction extends Consumer<EntityPlayerMPFake> {
    Object getKey();

    StartupActionType getType();

    Component getDisplayName(LocalizationKey key);

    default JsonObject toJson() {
        JsonObject json = new JsonObject();
        String type = this.getType().name().toLowerCase(Locale.ROOT);
        json.addProperty("type", type);
        return json;
    }

    static Optional<FakePlayerStartupAction> fromJson(JsonObject json) {
        StartupActionType type = StartupActionType.valueOf(json.get("type").getAsString().toUpperCase(Locale.ROOT));
        try {
            return Optional.of(switch (type) {
                case COMMAND -> CommandAction.of(json.get("command").getAsString());
                case SIMPLE -> SimpleAction.valueOf(json.get("action").getAsString().toUpperCase(Locale.ROOT));
            });
        } catch (RuntimeException e) {
            return Optional.empty();
        }
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
            json.addProperty("command", this.command);
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
        public Object getKey() {
            return this.getClass();
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
            json.addProperty("action", this.toString());
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
        public Object getKey() {
            return this;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    enum StartupActionType {
        COMMAND,
        SIMPLE
    }
}
