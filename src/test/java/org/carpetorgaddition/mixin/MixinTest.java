package org.carpetorgaddition.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.carpetorgaddition.util.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class MixinTest {
    /**
     * Mixin包的路径
     */
    private final String packagePath;
    /**
     * 每个Mixin类文件
     */
    private final List<File> mixinClass;

    public MixinTest() throws IOException {
        File file = new File("src/main/resources/carpet-org-addition.mixins.json");
        JsonObject json = IOUtils.loadJson(file);
        this.packagePath = "src/main/java/" + json.get("package").getAsString().replace(".", "/");
        ArrayList<String> list = new ArrayList<>();
        // 获取每个Mixin类文件
        Consumer<JsonElement> consumer = element -> list.add(this.packagePath + "/" + element.getAsString().replace(".", "/") + ".java");
        for (String path : Set.of("mixins", "server", "client")) {
            Optional.ofNullable(json.get(path)).ifPresent(array -> array.getAsJsonArray().asList().forEach(consumer));
        }
        this.mixinClass = list.stream().map(File::new).toList();
    }

    // 检查json文件
    @Test
    public void mixinsJsonTest() {
        // 检查是否有重复元素
        HashMap<File, File> map = new HashMap<>();
        for (File file : this.mixinClass) {
            File put = map.put(file, file);
            if (put == null) {
                continue;
            }
            Assert.fail("重复声明的Mixin类：" + put.getPath());
        }
        // 检查json中声明的文件是否全部存在
        for (File file : this.mixinClass) {
            Assert.assertTrue("不存在的Mixin类：" + file.getPath(), file.isFile());
        }
    }

    // 检查Mixin类
    @Test
    public void mixinClassTest() {
        File file = new File(this.packagePath);
        this.traverse(file);
    }

    // 遍历文件夹
    private void traverse(File file) {
        // 如果是文件，检查类文件是否被声明，否则遍历文件夹
        if (file.isFile() && !"package-info.java".equals(file.getName())) {
            Assert.assertTrue("未声明的Mixin类：" + file.getPath(), this.mixinClass.contains(file));
        } else {
            File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            // 递归遍历文件
            for (File each : files) {
                this.traverse(each);
            }
        }
    }
}
