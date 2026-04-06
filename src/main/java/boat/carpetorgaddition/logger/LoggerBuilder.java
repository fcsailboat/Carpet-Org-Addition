package boat.carpetorgaddition.logger;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.exception.CarpetLoggerRegisterException;
import carpet.logging.HUDLogger;
import carpet.logging.Logger;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class LoggerBuilder {
    private static final String[] NO_OPTIONS = new String[0];
    private final String name;
    @Nullable
    private final String defaultValue;
    private final String[] options;
    private boolean strict = false;
    private LoggerType type = LoggerType.STANDARD;
    private boolean hidden = false;
    private Consumer<ServerPlayer> subscribeCallback = CarpetOrgAddition::pass;
    private Consumer<ServerPlayer> unsubscribeCallback = CarpetOrgAddition::pass;

    private LoggerBuilder(String name, @Nullable String defaultValue, String[] options) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.options = options;
    }

    public static LoggerBuilder of(String name, String defaultValue, List<String> options) {
        return new LoggerBuilder(name, defaultValue, options.toArray(String[]::new));
    }

    public static LoggerBuilder of(String name) {
        return new LoggerBuilder(name, null, NO_OPTIONS);
    }

    public LoggerBuilder setStrict(boolean strict) {
        this.strict = strict;
        return this;
    }

    public LoggerBuilder setHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public LoggerBuilder setType(LoggerType type) {
        this.type = type;
        return this;
    }

    public LoggerBuilder setSubscribeCallback(Consumer<ServerPlayer> subscribeCallback) {
        this.subscribeCallback = subscribeCallback;
        return this;
    }

    public LoggerBuilder setUnsubscribeCallback(Consumer<ServerPlayer> unsubscribeCallback) {
        this.unsubscribeCallback = unsubscribeCallback;
        return this;
    }

    public LoggerAccessor build() {
        Field field = this.buildDummyBooleanField();
        Logger logger = switch (this.type) {
            case STANDARD, FUNCTION -> new Logger(field, this.name, this.defaultValue, this.options, this.strict);
            case HUD -> new HUDLogger(field, this.name, this.defaultValue, this.options, this.strict);
        };
        return new LoggerAccessor(logger, this.name, this.hidden, this.subscribeCallback, this.unsubscribeCallback);
    }

    /**
     * 动态生成和加载一个类，并获取其中的成员变量
     *
     * @apiNote 实际上，这个字段并没有什么实际用途，它仅用于与{@link Logger}类的构造方法和成员变量兼容。
     * {@code Carpet Org Addition}不会使用该对象参与实现任何逻辑
     */
    private Field buildDummyBooleanField() {
        String className = CarpetOrgAdditionConstants.COMPACT_MOD_NAME + "Dummy" + this.toUpperCamelCase(this.name);
        ClassDesc dummy = ClassDesc.of(className);
        byte[] bytes = ClassFile.of().build(dummy, classBuilder -> classBuilder
                .withField(
                        this.name,
                        ConstantDescs.CD_boolean,
                        fieldBuilder -> fieldBuilder.withFlags(AccessFlag.PUBLIC, AccessFlag.STATIC)
                )
        );
        Class<?> clazz = LoggerClassLoader.INSTANCE.defineClass(className, bytes);
        Field field;
        try {
            field = clazz.getField(this.name);
        } catch (NoSuchFieldException e) {
            throw new CarpetLoggerRegisterException("Unable to create %s logger".formatted(this.name), e);
        }
        field.setAccessible(true);
        return field;
    }

    private String toUpperCamelCase(String name) {
        String[] split = name.split("[_ ]]");
        StringBuilder builder = new StringBuilder();
        for (String str : split) {
            builder.append(str.substring(0, 1).toUpperCase(Locale.ROOT)).append(str.substring(1));
        }
        return builder.toString();
    }

    public static class LoggerClassLoader extends ClassLoader {
        private static final LoggerClassLoader INSTANCE = new LoggerClassLoader();

        private LoggerClassLoader() {
        }

        public Class<?> defineClass(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }
}
