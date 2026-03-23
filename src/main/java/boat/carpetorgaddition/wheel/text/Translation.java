package boat.carpetorgaddition.wheel.text;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.util.IOUtils;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Translation {
    private static final String TRANSLATION_PATH = "assets/carpet-org-addition/lang/%s.json";
    private static final Translation TRANSLATION = new Translation();
    /**
     * {@code Carpet Org Addition}的所有翻译，键表示语言，值是嵌套的一个Map集合，分别表示翻译的键和值
     */
    private final ConcurrentHashMap<String, Map<String, String>> translations = new ConcurrentHashMap<>();

    private Translation() {
    }

    public static Translation getInstance() {
        return TRANSLATION;
    }

    public Map<String, String> getTranslation() {
        // 每种语言只从文件读取一次
        String lang = CarpetOrgAdditionConstants.getCarpetLanguage();
        return this.translations.computeIfAbsent(lang, this::loadTranslation);
    }

    private Map<String, String> loadTranslation(String lang) {
        try {
            Map<String, String> map = readTranslation(lang);
            if (map.isEmpty()) {
                Map<String, String> english = this.translations.get("en_us");
                if (english == null) {
                    english = this.readTranslation("en_us");
                }
                return english;
            }
            return map;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to success fully read language file", e);
        }
    }

    // 从文件读取翻译
    private Map<String, String> readTranslation(String lang) throws IOException {
        ClassLoader loader = Translation.class.getClassLoader();
        String path = TRANSLATION_PATH.formatted(lang);
        InputStream input = loader.getResourceAsStream(path);
        try (input) {
            return readFromInputStream(input);
        }
    }

    private Map<String, String> readFromInputStream(InputStream input) throws IOException {
        if (input == null) {
            return Map.of();
        }
        byte[] bytes = new byte[1024];
        StringBuilder builder = new StringBuilder();
        int len;
        while ((len = input.read(bytes)) != -1) {
            builder.append(new String(bytes, 0, len, StandardCharsets.UTF_8));
        }
        String result = builder.toString();
        JsonObject json = IOUtils.stringAsJson(result);
        return json.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getAsString()));
    }

    /**
     * 根据键获取{@code Carpet Org Addition}的翻译，原版和其他模组的翻译不会从这里获取到
     *
     * @param key 翻译键
     * @return 如果翻译来自本模组，返回对应的翻译，如果翻译键本身错误，或着翻译键来自原版或其他模组，返回{@code null}
     */
    @Nullable
    public static String getTranslateValue(String key) {
        Map<String, String> translate = getInstance().getTranslation();
        return translate.get(key);
    }
}
