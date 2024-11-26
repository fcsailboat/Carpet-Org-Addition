package org.carpetorgaddition.logger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LoggerConfig {
    /**
     * @return 记录器名称
     */
    String name();

    /**
     * @return 记录器默认选项
     */
    String defaultOption() default "";

    /**
     * @return 记录器选项
     */
    String[] options() default {};

    /**
     * @return 选项是否严格
     */
    boolean strictOptions() default false;

    /**
     * @return 记录器类型
     */
    LoggerType type();
}
