package boat.util.docs.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.RuleContext;
import boat.carpetorgaddition.rule.value.CommandPermissionLevel;
import carpet.api.settings.RuleCategory;
import carpet.api.settings.RuleHelper;

import java.util.StringJoiner;

public class RuleInformation {
    private final String translatedName;
    private final String docs;
    private final String[] extra;
    private final RuleContext<?> context;

    public RuleInformation(RuleContext<?> context, String translatedName, String docs, String[] extra) {
        this.context = context;
        this.translatedName = translatedName;
        this.docs = docs;
        this.extra = extra;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // 规则名称
        String lineSeparator = System.lineSeparator();
        sb.append("### ").append(this.translatedName).append("(").append(this.context.getName()).append(")").append(lineSeparator);
        sb.append(lineSeparator);
        // 规则描述
        sb.append(this.docs).append(this.hasExtra() ? "<br>" + lineSeparator : lineSeparator);
        // 规则扩展描述
        if (this.hasExtra()) {
            for (String extra : this.extra) {
                sb.append("_").append(extra).append("_<br>").append(lineSeparator);
            }
            sb.append(lineSeparator);
        } else {
            sb.append(lineSeparator);
        }
        // 参数类型
        sb.append("- 类型：`").append(this.getArgumentType()).append("`").append(lineSeparator);
        // 参数默认值
        sb.append("- 默认值：`").append(this.getDefaultValue()).append("`").append(lineSeparator);
        // 参考选项
        if (this.context.getSuggestions().isEmpty()) {
            if (isBoolean()) {
                sb.append("- 参考选项：`true`，`false`").append(lineSeparator);
            }
        } else {
            StringJoiner sj = new StringJoiner("，", "- 参考选项：", lineSeparator);
            for (String option : this.context.getSuggestions()) {
                sj.add("`" + option + "`");
            }
            sb.append(sj);
        }
        // 分类
        sb.append(this.getCategory());
        return sb.toString();
    }

    // 规则值是否是布尔类型
    private boolean isBoolean() {
        return Boolean.class.isAssignableFrom(this.context.getType());
    }

    // 规则是否有扩展描述
    private boolean hasExtra() {
        return this.extra.length > 0;
    }

    // 获取参数类型名称
    private String getArgumentType() {
        Class<?> type = this.context.getType();
        if (String.class.isAssignableFrom(type) || CommandPermissionLevel.class.isAssignableFrom(type)) {
            return "字符串";
        }
        if (byte.class.isAssignableFrom(type) || short.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)
            || Byte.class.isAssignableFrom(type) || Short.class.isAssignableFrom(type) || Integer.class.isAssignableFrom(type)) {
            return "整数";
        }
        if (long.class.isAssignableFrom(type) || Long.class.isAssignableFrom(type)) {
            return "长整数";
        }
        if (float.class.isAssignableFrom(type) || double.class.isAssignableFrom(type)
            || Float.class.isAssignableFrom(type) || Double.class.isAssignableFrom(type)) {
            return "小数";
        }
        if (boolean.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type)) {
            return "布尔值";
        }
        if (char.class.isAssignableFrom(type) || Character.class.isAssignableFrom(type)) {
            return "字符";
        }
        if (Enum.class.isAssignableFrom(type)) {
            return "枚举";
        }
        throw new RuntimeException(this.context.getType().getName());
    }

    // 获取规则默认值
    private String getDefaultValue() {
        return RuleHelper.toRuleString(this.context.value());
    }

    // 获取规则分类
    private String getCategory() {
        StringJoiner stringJoiner = new StringJoiner("，", "- 分类：", System.lineSeparator());
        for (String category : this.context.getCategories()) {
            stringJoiner.add("`" + switch (category) {
                case CarpetOrgAdditionSettings.ORG -> "Org";
                case CarpetOrgAdditionSettings.HIDDEN -> "隐藏";
                case RuleCategory.BUGFIX -> "漏洞修复";
                case RuleCategory.SURVIVAL -> "生存";
                case RuleCategory.CREATIVE -> "创造";
                case RuleCategory.EXPERIMENTAL -> "试验性";
                case RuleCategory.OPTIMIZATION -> "优化";
                case RuleCategory.FEATURE -> "特性";
                case RuleCategory.COMMAND -> "命令";
                case RuleCategory.TNT -> "TNT";
                case RuleCategory.DISPENSER -> "发射器";
                case RuleCategory.SCARPET -> "Scarpet脚本语言";
                case RuleCategory.CLIENT -> "客户端";
                default -> category;
            } + "`");
        }
        return stringJoiner.toString();
    }
}
