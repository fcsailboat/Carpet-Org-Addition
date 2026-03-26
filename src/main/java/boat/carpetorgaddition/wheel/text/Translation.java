package boat.carpetorgaddition.wheel.text;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.util.IOUtils;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Translation {
    private static final String TRANSLATION_PATH = "assets/carpet-org-addition/lang/%s.json";
    private static final Translation TRANSLATION = new Translation();
    private static final String ROLLBACK_LANG = "en_us";
    /**
     * {@code Carpet Org Addition}的所有翻译，键表示语言的类型，值是嵌套的一个Map集合，表示翻译的本地化键名和值<br>
     */
    private final Map<String, Map<String, String>> translations = new ConcurrentHashMap<>();

    private Translation() {
    }

    public static Translation getInstance() {
        return TRANSLATION;
    }

    /**
     * @apiNote 在单人游戏中，方法可能会被{@code Render thread}和{@code Server thread}同时访问
     */
    public Map<String, String> getTranslation() {
        // 每种语言只从文件读取一次
        // CarpetOrgAdditionConstants.getCarpetLanguage()不是线程安全的，可能存在可见性问题，但不考虑这种情况
        String lang = CarpetOrgAdditionConstants.getCarpetLanguage();
        return this.translations.computeIfAbsent(lang, this::loadTranslation);
    }

    private Map<String, String> loadTranslation(String lang) {
        try {
            Map<String, String> map = readTranslation(lang);
            if (map.isEmpty()) {
                if (ROLLBACK_LANG.equals(lang)) {
                    return Map.of();
                }
                return this.translations.computeIfAbsent(ROLLBACK_LANG, this::loadTranslation);
            }
            return map;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to successfully read language file", e);
        }
    }

    // 从文件读取翻译
    private Map<String, String> readTranslation(String lang) throws IOException {
        ClassLoader loader = Translation.class.getClassLoader();
        String path = TRANSLATION_PATH.formatted(lang);
        InputStream input = loader.getResourceAsStream(path);
        if (input == null) {
            return Map.of();
        }
        try (input) {
            return readFromInputStream(input);
        }
    }

    private Map<String, String> readFromInputStream(InputStream input) throws IOException {
        String result = IOUtils.readInputStreamAsString(input);
        JsonObject json = IOUtils.stringAsJson(result);
        return json.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getAsString()));
    }

    /**
     * 根据本地化键名获取{@code Carpet Org Addition}的翻译，原版和其他模组的翻译不会从这里获取到
     *
     * @param key 本地化键名
     * @return 如果翻译来自本模组，返回对应的翻译，如果翻译键本身错误，或者翻译键来自原版或其他模组，返回{@code null}
     */
    @Nullable
    public static String getTranslateValue(String key) {
        Map<String, String> translate = getInstance().getTranslation();
        return translate.get(key);
    }
}
