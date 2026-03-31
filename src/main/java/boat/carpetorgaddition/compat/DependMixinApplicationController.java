package boat.carpetorgaddition.compat;

import boat.carpetorgaddition.CarpetOrgAddition;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DependMixinApplicationController implements MixinApplicationController {
    @SuppressWarnings("unchecked")
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            ClassNode classNode = MixinService.getService().getBytecodeProvider().getClassNode(mixinClassName);
            AnnotationNode node = Annotations.getVisible(classNode, Depend.class);
            if (node == null) {
                return true;
            }
            Map<String, Object> arguments = new HashMap<>();
            List<Object> values = node.values;
            for (int i = 0; i < values.size(); i += 2) {
                arguments.put((String) values.get(i), values.get(i + 1));
            }
            List<String> list = (List<String>) arguments.get("value");
            boolean result = list.stream().allMatch(FabricLoader.getInstance()::isModLoaded);
            CarpetOrgAddition.LOGGER.debug("Allow loading: {}", mixinClassName);
            return result;
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            CarpetOrgAddition.LOGGER.warn("Error checking mixin application conditions: ", e);
            return false;
        }
    }
}
