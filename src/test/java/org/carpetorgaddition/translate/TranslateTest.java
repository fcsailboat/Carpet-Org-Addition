package org.carpetorgaddition.translate;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.carpetorgaddition.util.wheel.Counter;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslateTest {
    private final List<String> zh;
    private final List<String> en;
    private final List<String> zhValue;
    private final List<String> enValue;
    private final ArrayList<String> notRuleKey = new ArrayList<>();

    public TranslateTest() throws FileNotFoundException {
        BufferedReader enReader = new BufferedReader(new FileReader("src/main/resources/assets/carpet-org-addition/lang/en_us.json"));
        BufferedReader zhReader = new BufferedReader(new FileReader("src/main/resources/assets/carpet-org-addition/lang/zh_cn.json"));
        Gson gson = new Gson();
        JsonObject jsonEn = gson.fromJson(enReader, JsonObject.class);
        JsonObject jsonZh = gson.fromJson(zhReader, JsonObject.class);
        // 排除所有规则相关的翻译键
        Predicate<String> excludeRuleName = s -> !s.matches("carpet\\.rule\\..+\\.name");
        this.zh = jsonZh.entrySet().stream().map(Map.Entry::getKey).filter(excludeRuleName).toList();
        this.en = jsonEn.entrySet().stream().map(Map.Entry::getKey).filter(excludeRuleName).toList();
        // 排除所有规则相关的翻译值
        Predicate<Map.Entry<String, JsonElement>> excludeRule = s -> !s.getKey().matches("carpet\\.rule\\..+\\.name") && !s.getKey().startsWith("carpet.rule.");
        this.zhValue = jsonZh.entrySet().stream().filter(excludeRule).map(Map.Entry::getValue).map(JsonElement::getAsString).toList();
        this.enValue = jsonEn.entrySet().stream().filter(excludeRule).map(Map.Entry::getValue).map(JsonElement::getAsString).toList();
        for (String key : zh) {
            if (key.startsWith("carpet.rule.")) {
                continue;
            }
            notRuleKey.add(key);
        }
        // 删除carpet.category.ORG，因为它在源码中没有被直接使用
        notRuleKey.remove("carpet.category.ORG");
        notRuleKey.remove("carpet.category.Customized");
    }

    /**
     * 比较中英文翻译的键是否完全相同
     */
    @Test
    public void compareTranslationKey() {
        // 中英文翻译的差异
        boolean identical = true;
        StringJoiner error = new StringJoiner("\n", "中英文翻译键不匹配：\n", "");
        // 包含在中文翻译中，但在英文翻译中没有
        for (String key : zh) {
            if (en.contains(key)) {
                continue;
            }
            error.add("en_us缺失：" + key);
            identical = false;
        }
        // 包含在英文翻译中，但在中文翻译中没有
        for (String key : en) {
            if (zh.contains(key)) {
                continue;
            }
            error.add("zh_cn缺失：" + key);
            identical = false;
        }
        Assert.assertTrue(error.toString(), identical);
    }

    /**
     * 检查是否有翻译键未被使用
     */
    @Test
    public void translationUsedBy() throws IOException {
        File rootPath = new File("src/main/java/org/carpetorgaddition");
        Counter<String> counter = new Counter<>();
        notRuleKey.forEach(key -> counter.set(key, 0));
        translationUsedBy(rootPath, counter);
        // 所有未被使用的翻译键
        List<String> list = counter.stream().filter(s -> counter.getCount(s) == 0).toList();
        StringJoiner errorReport = new StringJoiner("\n", "包含未使用的翻译键：\n", "");
        for (String key : list) {
            errorReport.add(key);
        }
        Assert.assertTrue(errorReport.toString(), list.isEmpty());
    }

    // 递归遍历所有源代码文件
    private void translationUsedBy(File root, Counter<String> counter) throws IOException {
        // 检查当前文件是否是文件夹
        File[] files = root.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile()) {
                // 检查当前文件是否是Java文件
                if (file.getName().endsWith(".java")) {
                    readFile(file, counter);
                }
            } else if (file.isDirectory()) {
                // 递归遍历所有Java文件
                translationUsedBy(file, counter);
            }
        }
    }

    private void readFile(File java, Counter<String> counter) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(java));
        String line;
        while ((line = reader.readLine()) != null) {
            for (String key : notRuleKey) {
                int index = 0;
                while ((line.indexOf("\"" + key + "\"", index)) != -1) {
                    index++;
                    counter.add(key);
                }
            }
        }
    }

    /**
     * 检查中英文翻译值占位符的个数是否一致
     */
    @Test
    public void compareTranslationValue() {
        int size = zhValue.size();
        boolean perfectMatch = true;
        StringJoiner error = new StringJoiner("\n", "中英文翻译值不匹配：\n", "");
        for (int index = 0; index < size; index++) {
            String zhTranslate = zhValue.get(index);
            String enTranslate = enValue.get(index);
            if (count(zhTranslate) == count(enValue.get(index))) {
                continue;
            }
            error.add(zhTranslate + " --- " + enTranslate);
            perfectMatch = false;
        }
        Assert.assertTrue(error.toString(), perfectMatch);
    }

    private int count(String key) {
        Matcher matcher = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)").matcher(key);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
