package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.util.IOUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.WorldFormat;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelResource;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

public class RuntimeCommand extends AbstractServerCommand {
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("runtime");
    public static final LocalizationKey MEMORY = KEY.then("memory");

    public RuntimeCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .then(Commands.literal("memory")
                        .then(Commands.literal("jvm")
                                .executes(this::showJvmMemory))
                        .then(Commands.literal("physical")
                                .executes(this::showPhysicalMemory))
                        .then(Commands.literal("gc")
                                .executes(this::triggerGc)))
                .then(Commands.literal("server")
                        .then(Commands.literal("openfolder")
                                .executes(this::openFolder)))
                .then(Commands.literal("test")
                        .then(Commands.literal("io")
                                .then(Commands.argument("content", StringArgumentType.greedyString())
                                        .executes(this::writeContent)))));
    }

    /**
     * 显示jvm内存使用情况
     *
     * @return 已使用的内存大小
     */
    private int showJvmMemory(CommandContext<CommandSourceStack> context) {
        Runtime runtime = Runtime.getRuntime();
        // 已使用内存
        long usedSize = runtime.totalMemory() - runtime.freeMemory();
        Component used = displayMemory(usedSize);
        Component total = displayMemory(runtime.totalMemory());
        Component max = displayMemory(runtime.maxMemory());
        MessageUtils.sendEmptyMessage(context);
        LocalizationKey key = MEMORY.then("jvm");
        MessageUtils.sendMessage(context, key.translate());
        MessageUtils.sendMessage(context, key.then("used").translate(used));
        MessageUtils.sendMessage(context, key.then("total").translate(total));
        MessageUtils.sendMessage(context, key.then("max").translate(max));
        return (int) usedSize;
    }

    /**
     * 显示物理内存使用情况
     *
     * @return 已使用的物理内存大小
     */
    private int showPhysicalMemory(CommandContext<CommandSourceStack> context) {
        SystemInfo info = new SystemInfo();
        GlobalMemory memory = info.getHardware().getMemory();
        long usedSize = memory.getTotal() - memory.getAvailable();
        Component total = displayMemory(memory.getTotal());
        Component used = displayMemory(usedSize);
        MessageUtils.sendEmptyMessage(context);
        LocalizationKey key = MEMORY.then("physical");
        MessageUtils.sendMessage(context, key.translate());
        MessageUtils.sendMessage(context, key.then("total").translate(total));
        MessageUtils.sendMessage(context, key.then("used").translate(used));
        return (int) usedSize;
    }

    /**
     * 尝试触发一次gc
     */
    private int triggerGc(CommandContext<CommandSourceStack> context) {
        Runtime runtime = Runtime.getRuntime();
        long l = runtime.freeMemory();
        runtime.gc();
        long size = runtime.freeMemory() - l;
        Component free = displayMemory(size);
        LocalizationKey key = KEY.then("gc");
        MessageUtils.sendMessage(context, key.translate(free));
        Component prompt = TextBuilder.of(key.then("prompt").translate()).setGrayItalic().build();
        MessageUtils.sendMessage(context.getSource(), prompt);
        return (int) size;
    }

    /**
     * 打开世界文件夹
     */
    private int openFolder(CommandContext<CommandSourceStack> context) {
        Util.getPlatform().openPath(context.getSource().getServer().getWorldPath(LevelResource.ROOT));
        return 1;
    }

    private Component displayMemory(long size) {
        DecimalFormat format = new DecimalFormat("#.00");
        String mb = format.format(size / 1024.0 / 1024.0);
        TextBuilder builder = TextBuilder.of("%sMB".formatted(mb))
                .setHover("%sB".formatted(size))
                .setColor(ChatFormatting.GRAY);
        return builder.build();
    }

    private int writeContent(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        WorldFormat worldFormat = new WorldFormat(server, "debug", "io");
        File file = worldFormat.file("FileWriteTest.txt");
        String content = StringArgumentType.getString(context, "content");
        try {
            IOUtils.write(file, content);
        } catch (IOException e) {
            IOUtils.loggerError(e);
        }
        return content.length();
    }

    @Override
    public String getDefaultName() {
        return "runtime";
    }

    @Override
    public boolean shouldRegister() {
        return CarpetOrgAddition.isDebugMode();
    }
}
