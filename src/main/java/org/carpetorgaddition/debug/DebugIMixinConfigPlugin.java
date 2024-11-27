package org.carpetorgaddition.debug;

import org.carpetorgaddition.CarpetOrgAddition;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class DebugIMixinConfigPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

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
            if (CarpetOrgAddition.isDebugDevelopment()) {
                CarpetOrgAddition.LOGGER.info("Mixin类已被允许开发环境下加载：{}", mixinClassName);
                return true;
            }
            return false;
        } catch (IOException | ClassNotFoundException e) {
            return true;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
