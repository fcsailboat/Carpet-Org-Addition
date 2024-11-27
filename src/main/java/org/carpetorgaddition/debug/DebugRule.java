package org.carpetorgaddition.debug;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DebugRule {
    String name();

    String desc() default "";

    String[] extra() default {};

    String[] options() default {};
}
