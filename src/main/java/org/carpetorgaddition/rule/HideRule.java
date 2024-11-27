package org.carpetorgaddition.rule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 带有此注解的规则为隐藏规则，隐藏规则不会在游戏中显示，不会被写入文档，不会计入规则数量
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface HideRule {
}
