package org.carpetorgaddition.mixin.rule.carpet;

import carpet.api.settings.Rule;
import carpet.api.settings.SettingsManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.rule.Customized;
import org.carpetorgaddition.rule.HideRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

@Mixin(value = SettingsManager.class, remap = false)
public class SettingsManagerMixin {
    @SuppressWarnings("unchecked")
    @WrapOperation(method = "parseSettingsClass", at = @At(value = "INVOKE", target = "Ljava/lang/reflect/Field;getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;", ordinal = 0))
    private <T extends Annotation> T shouldRegister(Field field, Class<Rule> annotationClass, Operation<Rule> original) {
        return this.shouldRegister(field) ? (T) original.call(field, annotationClass) : null;
    }

    @Unique
    private boolean shouldRegister(Field field) {
        if (field.isAnnotationPresent(HideRule.class)) {
            return false;
        }
        if (field.isAnnotationPresent(Customized.class)) {
            return CarpetOrgAddition.ENABLE_CUSTOMIZED_RULE;
        }
        return true;
    }
}
