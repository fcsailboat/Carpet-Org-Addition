package boat.carpetorgaddition.util;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.exception.FileOperationException;
import com.google.gson.*;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class IOUtils {
    public static final String JSON_EXTENSION = ".json";
    public static final String NBT_EXTENSION = ".nbt";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /**
     * 模组配置文件路径
     */
    public static final Path CONFIGURE_DIRECTORY = CarpetOrgAdditionConstants.getConfigDirectory().resolve(CarpetOrgAdditionConstants.COMPACT_MOD_NAME_LOWER_CASE);
    /**
     * 不能包含在文件名中的字符
     */
    public static final String INVALID_FILENAME_CHARS = "\\/:*?\"<>|";
    /**
     * Windows保留文件名
     */
    public static final String[] WINDOWS_RESERVED_NAME = {
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    };
    /**
     * {@code usercache.json}文件
     */
    public static final File USERCACHE_JSON = CarpetOrgAdditionConstants.getGameDirectory().resolve("usercache.json").toFile();

    private IOUtils() {
    }

    /**
     * 创建一个UTF-8编码的字符输入流对象
     */
    public static BufferedReader toReader(File file) throws IOException {
        return Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
    }

    /**
     * 创建一个UTF-8编码的字符输出流对象
     */
    public static BufferedWriter toWriter(File file) throws IOException {
        return toWriter(file, StandardCharsets.UTF_8);
    }

    /**
     * 创建一个指定编码的字符输出流对象
     */
    public static BufferedWriter toWriter(File file, Charset charset) throws IOException {
        // 如果父级目录不存在则创建
        // 如果是根目录或父级目录存在，则无需创建父级目录
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new IOException("Failed to create directory: " + parent);
            }
        }
        return Files.newBufferedWriter(file.toPath(), charset);
    }

    /**
     * 将一个json对象保存到文件
     */
    @NullMarked
    public static void write(File file, JsonObject json) throws IOException {
        String jsonString = GSON.toJson(json, JsonObject.class);
        write(file, jsonString);
    }

    /**
     * 将一段字符串文本保存到文件
     */
    @NullMarked
    public static void write(File file, String content) throws IOException {
        write(file, content, StandardCharsets.UTF_8);
    }

    @NullMarked
    public static void write(File file, String content, Charset charset) throws IOException {
        File parent = file.getParentFile();
        if (parent == null) {
            throw new IOException("File parent directory is null: " + file.getAbsolutePath());
        }
        // 创建父级目录
        Files.createDirectories(parent.toPath());
        File tempFile = Files.createTempFile(parent.toPath(), removeExtension(file) + "-", ".tmp").toFile();
        boolean hasOriginalFile = file.exists();
        File backupFile = Paths.get(file.getParent(), file.getName() + ".bak").toFile();
        try {
            // 将数据保存到临时文件
            try (BufferedWriter writer = toWriter(tempFile, charset)) {
                writer.write(content);
            }
            // 备份旧的文件
            if (hasOriginalFile) {
                Files.deleteIfExists(backupFile.toPath());
                Files.move(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // 重命名失败，回退文件
                if (backupFile.exists()) {
                    Files.move(backupFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                throw e;
            }
            // 删除备份
            Files.deleteIfExists(backupFile.toPath());
        } catch (IOException e) {
            if (hasOriginalFile && backupFile.exists()) {
                try {
                    // 保存失败，恢复文件
                    Files.move(backupFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException recoveryError) {
                    e.addSuppressed(recoveryError);
                }
            }
            // 保存失败，回退文件
            Files.deleteIfExists(tempFile.toPath());
            throw e;
        }
    }

    /**
     * 从文件加载一个json对象
     */
    public static JsonObject loadJson(File file) throws IOException {
        return loadJson(file, JsonObject.class);
    }

    public static <T> T loadJson(File file, Class<T> type) throws IOException {
        BufferedReader reader = toReader(file);
        try (reader) {
            return GSON.fromJson(reader, type);
        }
    }

    public static JsonObject stringAsJson(String jsonString) {
        return GSON.fromJson(jsonString, JsonObject.class);
    }

    public static String jsonAsString(JsonObject json) {
        return GSON.toJson(json, JsonObject.class);
    }

    /**
     * 复制一个文件
     *
     * @param original 源文件，该文件必须存在且不能是文件夹
     * @param copy     复制的目标位置
     */
    public static void copyFile(File original, File copy) {
        if (original.isFile()) {
            try {
                BufferedInputStream input = new BufferedInputStream(new FileInputStream(original));
                BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(copy));
                try (input; output) {
                    byte[] bytes = new byte[1024 * 8]; // 1024 * 8 = 8192;
                    int len;
                    while ((len = input.read(bytes)) != -1) {
                        output.write(bytes, 0, len);
                    }
                }
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.warn("Unable to copy file", e);
            }
        } else {
            CarpetOrgAddition.LOGGER.warn("Copy a non-existent file: {}", original.getAbsolutePath());
        }
    }

    /**
     * 备份一个文件
     *
     * @param file 要备份的文件
     */
    public static void backupFile(File file) {
        File backup = new File(file.getParent(), "backup_" + System.currentTimeMillis() + "_" + file.getName());
        if (backup.exists()) {
            // 不太可能出现重名备份文件
            throw new IllegalStateException("The backup file already exists");
        }
        copyFile(file, backup);
    }

    /**
     * json对象中是否包含指定元素
     *
     * @param elements 一个字符串数组，数组中只要有一个元素不存在于json中方法就返回false
     */
    public static boolean jsonHasElement(JsonObject json, String... elements) {
        for (String element : elements) {
            if (json.has(element)) {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * 检查文件名称是否有效
     */
    public static boolean isValidFileName(String fileName) {
        // 不允许以“.”开头，因为这样的文件名可能被视为隐藏文件
        if (fileName.startsWith(".")) {
            return true;
        }
        // 不能是Windows保留文件名
        int index = fileName.indexOf(".");
        String substring = (index == -1 ? fileName : fileName.substring(0, index)).toUpperCase(Locale.ROOT);
        for (String str : WINDOWS_RESERVED_NAME) {
            if (Objects.equals(str, substring)) {
                return true;
            }
        }
        // 文件名包含无效字符
        for (int i = 0; i < IOUtils.INVALID_FILENAME_CHARS.length(); i++) {
            char c = IOUtils.INVALID_FILENAME_CHARS.charAt(i);
            if (fileName.contains(String.valueOf(c))) {
                return true;
            }
        }
        return false;
    }

    public static File configFile(String fileName) {
        return CONFIGURE_DIRECTORY.resolve(fileName).toFile();
    }

    /**
     * 根据键获取json中对应的值，如果不存在，返回默认值
     *
     * @param defaultValue 如果为获取到值，返回默认值
     * @param type         返回值的类型
     */
    @Contract(value = "_,_,!null,_ -> !null")
    public static <T> T getJsonElement(JsonObject json, String key, T defaultValue, Class<T> type) {
        JsonElement element = json.get(key);
        if (element == null) {
            return defaultValue;
        }
        // 布尔值
        if (type == boolean.class || type == Boolean.class) {
            return type.cast(element.getAsBoolean());
        }
        // 整数
        if (type == byte.class || type == Byte.class) {
            return type.cast(element.getAsByte());
        }
        if (type == short.class || type == Short.class) {
            return type.cast(element.getAsShort());
        }
        if (type == int.class || type == Integer.class) {
            return type.cast(element.getAsInt());
        }
        if (type == long.class || type == Long.class) {
            return type.cast(element.getAsLong());
        }
        // 浮点数
        if (type == float.class || type == Float.class) {
            return type.cast(element.getAsFloat());
        }
        if (type == double.class || type == Double.class) {
            return type.cast(element.getAsDouble());
        }
        // 字符串
        if (type == String.class) {
            return type.cast(element.getAsString());
        }
        // jsonObject
        if (JsonObject.class.isAssignableFrom(type)) {
            return type.cast(element.getAsJsonObject());
        }
        // JsonArray
        if (JsonArray.class.isAssignableFrom(type)) {
            return type.cast(element.getAsJsonArray());
        }
        throw new IllegalArgumentException();
    }

    public static <T> Optional<T> getJsonElement(JsonObject json, String key, Class<T> type) {
        return Optional.ofNullable(getJsonElement(json, key, null, type));
    }

    /**
     * 获取不带扩展名的文件名
     */
    public static String getFileNameWithoutExtension(File file) {
        String name = file.getName();
        int i = name.lastIndexOf(".");
        return i == -1 ? name : name.substring(0, i);
    }

    /**
     * 删除文件扩展名
     */
    @Deprecated(forRemoval = true)
    public static String removeJsonExtension(String fileName) {
        if (fileName.endsWith(JSON_EXTENSION)) {
            return fileName.substring(0, fileName.lastIndexOf("."));
        }
        return fileName;
    }

    public static String removeExtension(File file) {
        return removeExtension(file.getName());
    }

    public static String removeExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        return index == -1 ? fileName : fileName.substring(0, index);
    }

    public static String removeExtension(String fileName, String extension) {
        String fineNameLowerCase = fileName.toLowerCase(Locale.ROOT);
        String extensionNameLowerCase = (extension.startsWith(".") ? extension : "." + extension).toLowerCase(Locale.ROOT);
        if (fineNameLowerCase.endsWith(extensionNameLowerCase)) {
            return removeExtension(fileName);
        }
        return fileName;
    }

    /**
     * 重命名文件
     */
    public static File renameFile(File file, String name) {
        Path source = file.toPath();
        Path target = new File(file.getParent(), name).toPath();
        try {
            return Files.move(source, target).toFile();
        } catch (IOException e) {
            throw new FileOperationException("Unable to rename file %s".formatted(file.toString()), e);
        }
    }

    /**
     * 如果文件存在，则删除文件
     */
    public static void removeFileIfExists(File file) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            throw new FileOperationException("Unable to delete file %s".formatted(file.toString()), e);
        }
    }

    /**
     * 删除一个文件
     */
    @Deprecated(forRemoval = true)
    public static void removeFile(File file) {
        if (file.delete()) {
            return;
        }
        throw new FileOperationException("Unable to delete file %s".formatted(file.toString()));
    }

    /**
     * 将一个文件标记为弃用
     */
    public static void deprecatedFile(File file) throws IOException {
        if (file.renameTo(new File(file.getParent(), "deprecated_" + System.currentTimeMillis() + "_" + file.getName()))) {
            return;
        }
        throw new IOException("Unable to rename file %s".formatted(file.toString()));
    }

    /**
     * 记录一个IO错误，用来处理编译时异常
     */
    public static void loggerError(IOException e) {
        CarpetOrgAddition.LOGGER.error("IO error occurred:", e);
    }

    /**
     * @return 文件夹中是否包含与指定文件完全相同的文件
     */
    public static boolean containsIdenticalFile(File directory, File file) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files == null) {
                return false;
            }
            for (File current : files) {
                if (equalsForSameNamedFiles(file, current)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 比较两个文件的名称和内容是否完全相同
     */
    public static boolean equalsForSameNamedFiles(File file1, File file2) {
        if (Objects.equals(file1.getName(), file2.getName())) {
            return equals(file1, file2);
        }
        return false;
    }

    /**
     * 比较两个文件是否完全相同
     */
    public static boolean equals(File file1, File file2) {
        if (file1.length() != file2.length()) {
            return false;
        }
        return equals(file1.toPath(), file2.toPath());
    }

    public static boolean equals(Path path1, Path path2) {
        try {
            return Files.mismatch(path1, path2) == -1L;
        } catch (IOException e) {
            return false;
        }
    }

    public static String readInputStream(InputStream input) throws IOException {
        StringBuilder builder = new StringBuilder();
        byte[] bytes = new byte[1024];
        int len;
        while ((len = input.read(bytes)) != -1) {
            builder.append(new String(bytes, 0, len, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }
}
