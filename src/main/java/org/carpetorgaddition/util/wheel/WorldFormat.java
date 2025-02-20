package org.carpetorgaddition.util.wheel;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @see <a href="https://zh.minecraft.wiki/w/Java%E7%89%88%E4%B8%96%E7%95%8C%E6%A0%BC%E5%BC%8F">世界格式</a>
 */
public class WorldFormat {
    /**
     * 文件是否为{@code json}扩展名
     */
    public static final Predicate<File> JSON_EXTENSIONS = file -> file.getName().endsWith(IOUtils.JSON_EXTENSION);

    private final File directory;

    /**
     * 尝试创建一个存档目录下的文件夹
     *
     * @param server      游戏当前运行的服务器，用来获取操作系统下服务器存档的路径
     * @param directory   一个字符串，表示第二级子目录，有这个参数的原因是为了防止构造忘记传入第二级目录参数，该参数可以为null，
     *                    表示没有第二级目录，此时不应该为第三个参数传入值
     * @param directories 一个字符串数组，数组中第一个元素表示第三级子目录，第二个元素表示第四级子目录，以此类推
     * @apiNote 第一级目录是carpetorgaddition文件夹
     */
    public WorldFormat(MinecraftServer server, @Nullable String directory, String... directories) {
        // 获取服务器存档保存文件的路径
        Path path = server.getSavePath(WorldSavePath.ROOT).resolve(CarpetOrgAddition.MOD_NAME_LOWER_CASE);
        if (directory != null) {
            path = path.resolve(directory);
        }
        // 拼接路径
        for (String name : directories) {
            path = path.resolve(name);
        }
        // 将路径转为文件对象
        this.directory = path.toFile();
        // 文件夹必须存在或者成功创建
        if (this.directory.isDirectory() || this.directory.mkdirs()) {
            return;
        }
        // 如果这个文件夹不存在并且没有创建成功，将信息写入日志
        CarpetOrgAddition.LOGGER.error("{}文件夹创建失败", this.directory);
    }

    /**
     * 创建一个当前目录下的文件对象，只创建文件对象，不创建文件
     *
     * @param fileName 文件对象的文件名，必须是带扩展名的
     */
    public File file(String fileName) {
        if (fileName.contains(".")) {
            return new File(this.directory, fileName);
        }
        throw new IllegalArgumentException();
    }

    /**
     * 创建一个当前目录下的文件对象，只创建文件对象，不创建文件
     *
     * @param fileName 文件名，如果没有扩展名，则自动添加json作为扩展名
     */
    public File jsonFile(String fileName) {
        return new File(this.directory, suppJsonFileName(fileName));
    }

    /**
     * 获取一个当前目录下的指定名称的文件对象
     *
     * @param fileName 文件的名称，如果没有扩展名，则自动添加一个.json作为扩展名
     */
    public File getJsonFile(String fileName) {
        fileName = suppJsonFileName(fileName);
        return new File(this.directory, fileName);
    }

    // 补全文件扩展名
    private static String suppJsonFileName(String fileName) {
        if (fileName.split("\\.").length == 1) {
            return fileName + IOUtils.JSON_EXTENSION;
        }
        return fileName;
    }

    /**
     * @return 包含目录下所有文件的Set集合
     * @deprecated 因为是Set集合，所以集合内的元素是无序的，并且，该集合可变，可以任意添加或修改元素
     */
    @Deprecated(forRemoval = true)
    public HashSet<File> listFiles() {
        File[] files = this.directory.listFiles();
        if (files == null) {
            // 返回空集合
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(files));
    }

    /**
     * @return 包含该目录所有文件的不可变的List集合
     * @apiNote Java貌似没有对中文的拼音排序做很好的支持，因此，中文的排序依然是无序的
     */
    public List<File> toImmutableFileList() {
        File[] files = this.directory.listFiles();
        if (files == null) {
            return List.of();
        }
        // 一些操作系统下文件排序可能不是按字母排序
        return Stream.of(files).sorted(Comparator.comparing(file -> file.getName().toLowerCase())).toList();
    }

    public List<File> toImmutableFileList(Predicate<File> filter) {
        File[] files = this.directory.listFiles();
        if (files == null) {
            return List.of();
        }
        // 一些操作系统下文件排序可能不是按字母排序
        return Stream.of(files).filter(filter).sorted(Comparator.comparing(file -> file.getName().toLowerCase())).toList();
    }

    // 检查该目录下的文件是否存在
    public boolean fileExists(String fileName) {
        fileName = suppJsonFileName(fileName);
        File file = this.jsonFile(fileName);
        return file.exists();
    }

    @Override
    public String toString() {
        return this.directory.toString();
    }
}
