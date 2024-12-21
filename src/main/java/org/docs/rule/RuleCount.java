package org.docs.rule;

import carpet.api.settings.Rule;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.Removed;

import java.lang.reflect.Field;

public class RuleCount {
    public static void main(String[] args) {
        Field[] fields = CarpetOrgAdditionSettings.class.getFields();
        int count = 0;
        for (Field field : fields) {
            if (field.isAnnotationPresent(Removed.class)) {
                continue;
            }
            if (field.isAnnotationPresent(Rule.class)) {
                count++;
            }
        }
        System.out.println("当前一共有" + count + "条规则。");
    }
}
