package boat.carpetorgaddition.debug;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.compat.MixinApplicationController;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

import java.io.IOException;

public class DebugMixinConfigPlugin implements MixinApplicationController {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            ClassNode classNode = MixinService.getService().getBytecodeProvider().getClassNode(mixinClassName);
            AnnotationNode annotationNode = Annotations.getVisible(classNode, OnlyDeveloped.class);
            // Mixin类没有被@OnlyDeveloped注解
            if (annotationNode == null) {
                return true;
            }
            // 类被注解，且开发环境
            if (CarpetOrgAddition.isDebugMode()) {
                CarpetOrgAddition.LOGGER.debug("Mixin class has been allowed to load in development environment: {}", mixinClassName);
                return true;
            }
            return false;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }
}
