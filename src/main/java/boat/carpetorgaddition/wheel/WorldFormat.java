package boat.carpetorgaddition.wheel;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.exception.FileOperationException;
import boat.carpetorgaddition.util.IOUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
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
    /**
     * 文件所在文件夹
     */
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
        Path path = server.getWorldPath(LevelResource.ROOT).resolve(CarpetOrgAdditionConstants.COMPACT_MOD_NAME_LOWER_CASE);
        if (directory == null) {
            if (directories.length != 0) {
                throw new IllegalArgumentException();
            }
        } else {
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

    private WorldFormat(WorldFormat worldFormat, String child) {
        this.directory = new File(worldFormat.directory, child);
        if (this.directory.isDirectory() || this.directory.mkdirs()) {
            return;
        }
        throw new FileOperationException("Unable to create directory: " + this.directory);
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
     * @param fileName  文件对象的文件名
     * @param extension 如果文件名没有扩展名，则自动添加当前参数为扩展名
     */
    public File file(String fileName, String extension) {
        String end = extension.startsWith(".") ? extension : "." + extension;
        if (fileName.endsWith(end)) {
            return new File(this.directory, fileName);
        }
        return new File(this.directory, fileName + end);
    }

    /**
     * 创建一个当前目录下的文件夹对象，只创建文件对象，不创建文件
     */
    public File directory(String directory) {
        return new File(this.directory, directory);
    }

    /**
     * @return 包含该目录所有文件的不可变的List集合
     * @apiNote Java貌似没有对中文的拼音排序做很好的支持，因此，中文的排序依然是无序的
     */
    @Unmodifiable
    public List<File> listFiles() {
        File[] files = this.directory.listFiles();
        if (files == null) {
            return List.of();
        }
        // 一些操作系统下文件排序可能不是按字母排序
        return Stream.of(files).sorted(Comparator.comparing(file -> file.getName().toLowerCase())).toList();
    }

    @Unmodifiable
    public List<File> listFiles(Predicate<File> filter) {
        File[] files = this.directory.listFiles();
        if (files == null) {
            return List.of();
        }
        // 一些操作系统下文件排序可能不是按字母排序
        return Stream.of(files).filter(filter).sorted(Comparator.comparing(file -> file.getName().toLowerCase())).toList();
    }

    /**
     * 创建指向当前目录下子目录的新对象
     */
    public WorldFormat resolve(String child) {
        return new WorldFormat(this, child);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return this.directory.equals(((WorldFormat) o).directory);
    }

    @Override
    public int hashCode() {
        return this.directory.hashCode();
    }

    @Override
    public String toString() {
        return this.directory.toString();
    }
}
