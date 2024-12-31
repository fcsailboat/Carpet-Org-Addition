package org.docs.rule;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.carpetorgaddition.rule.Hidden;
import org.carpetorgaddition.rule.Removed;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RuleDocument {
    private final LinkedHashSet<String> rules = new LinkedHashSet<>();
    private final JsonObject json;

    public static void main(String[] args) throws IOException, NoSuchFieldException, ClassNotFoundException {
        // 生成文档前备份旧的文件
        String time = DateTimeFormatter.ofPattern("yyMMddHHmmss").format(LocalDateTime.now());
        FileInputStream fileInputStream = new FileInputStream("docs/rules.md");
        Files.copy(fileInputStream, Path.of("docs/backups/rules/" + time + ".md"));
        RuleDocument ruleDocument = new RuleDocument();
        BufferedWriter writer = new BufferedWriter(new FileWriter("docs/rules.md"));
        writer.write("## 所有规则");
        writer.newLine();
        writer.newLine();
        writer.write("**提示：可以使用`Ctrl+F`快速查找自己想要的规则**");
        writer.newLine();
        writer.newLine();
        for (String rule : ruleDocument.rules) {
            RuleInformation ruleInfo = ruleDocument.readClass(rule);
            if (ruleInfo == null) {
                continue;
            }
            writer.write(ruleInfo.toString());
            writer.newLine();
        }
        writer.close();
    }

    RuleDocument() throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/assets/carpet-org-addition/lang/zh_cn.json"));
        Gson gson = new Gson();
        this.json = gson.fromJson(reader, JsonObject.class);
        Set<Map.Entry<String, JsonElement>> entries = this.json.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
            List<String> list = Arrays.stream(entry.getKey().split("\\.")).toList();
            if (isRule(list)) {
                this.rules.add(list.get(2));
            }
        }
    }

    // 当前翻译键是否是规则的翻译键
    private boolean isRule(List<String> list) {
        if ("carpet".equals(list.get(0)) && "rule".equals(list.get(1))) {
            return !"message".equals(list.get(2)) && !"validate".equals(list.get(2));
        }
        return false;
    }

    // 读取字节码信息
    @Nullable
    RuleInformation readClass(String rule) throws ClassNotFoundException, NoSuchFieldException {
        Class<?> clazz = Class.forName("org.carpetorgaddition.CarpetOrgAdditionSettings");
        Field field = clazz.getField(rule);
        if (field.isAnnotationPresent(Removed.class) || field.isAnnotationPresent(Hidden.class)) {
            return null;
        }
        return new RuleInformation(field, readRuleName(rule), readRuleDesc(rule), readRuleExtra(rule));
    }

    // 读取规则名称
    private String readRuleName(String rule) {
        return json.get("carpet.rule." + rule + ".name").getAsString();
    }

    // 读取规则描述
    private String readRuleDesc(String rule) {
        return json.get("carpet.rule." + rule + ".desc").getAsString();
    }

    // 读取规则扩展描述
    private String[] readRuleExtra(String rule) {
        int number = 0;
        ArrayList<String> list = new ArrayList<>();
        while (true) {
            String extra = "carpet.rule." + rule + ".extra." + number;
            if (this.json.has(extra)) {
                list.add(this.json.get(extra).getAsString());
                number++;
            } else {
                break;
            }
        }
        return list.toArray(new String[0]);
    }
}
