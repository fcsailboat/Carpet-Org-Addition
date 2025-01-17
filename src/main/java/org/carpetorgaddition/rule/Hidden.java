package org.carpetorgaddition.rule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 带有此注解的规则默认是隐藏的，只有在启动时加上jvm参数{@code -DCarpetOrgAddition.ShowHiddenRule=true}才会显示
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Hidden {
}
