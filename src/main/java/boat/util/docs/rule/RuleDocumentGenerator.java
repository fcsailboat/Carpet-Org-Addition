package boat.util.docs.rule;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.RuleContext;
import boat.carpetorgaddition.util.IOUtils;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RuleDocumentGenerator {
    private final Set<RuleContext<?>> rules;
    private final JsonObject json;
    private static final boolean CUSTOM_CONFIG_PATH = false;
    private static final Path ROOT = Path.of("..");

    private RuleDocumentGenerator() throws IOException {
        this.json = IOUtils.loadJson(ROOT.resolve(Path.of("src/main/resources/assets/carpet-org-addition/lang/zh_cn.json")).toFile());
        this.rules = CarpetOrgAdditionSettings.listRules();
    }

    public static RuleDocumentGenerator of() {
        if (CUSTOM_CONFIG_PATH) {
            CarpetOrgAdditionConstants.setCarpetLanguage("zh_cn");
            CarpetOrgAdditionConstants.setConfigDirectory(Path.of("run/config"));
            CarpetOrgAdditionConstants.setGameDirectory(Path.of("run"));
        }
        try {
            return new RuleDocumentGenerator();
        } catch (IOException e) {
            throw new RuntimeException("Rule document generator initialization failed", e);
        }
    }

    /**
     * 生成规则文档
     */
    public void generate() {
        try {
            // 生成文档前备份旧的文件
            String time = DateTimeFormatter.ofPattern("yyMMddHHmmss").format(LocalDateTime.now());
            FileInputStream input = new FileInputStream(ROOT.resolve("docs/rules.md").toFile());
            Files.copy(input, ROOT.resolve(Path.of("docs/backups/rules/" + time + ".md")));
            BufferedWriter writer = new BufferedWriter(new FileWriter(ROOT.resolve("docs/rules.md").toFile(), StandardCharsets.UTF_8));
            try (writer) {
                this.head(writer);
                this.body(writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to generate rule document", e);
        }
    }

    private void head(BufferedWriter writer) throws IOException {
        writer.write("## 所有规则");
        writer.newLine();
        writer.newLine();
        writer.write("**提示：可以使用`Ctrl+F`快速查找自己想要的规则**");
        writer.newLine();
        writer.newLine();
    }

    private void body(BufferedWriter writer) throws IOException {
        List<RuleInformation> list = this.rules.stream()
                .filter(context -> !context.isHidden())
                .filter(context -> !context.isRemove())
                .map(this::parse)
                .toList();
        for (RuleInformation ruleInfo : list) {
            writer.write(ruleInfo.toString());
            writer.newLine();
        }
    }

    // 读取字节码信息
    private RuleInformation parse(RuleContext<?> context) {
        String rule = context.getName();
        return new RuleInformation(context, readRuleName(rule), readRuleDesc(rule), readRuleExtra(rule));
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
