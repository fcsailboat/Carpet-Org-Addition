package boat.carpetorgaddition.translate;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.RuleContext;
import boat.carpetorgaddition.translate.TranslateParser.Entry;
import boat.carpetorgaddition.util.IOUtils;
import boat.carpetorgaddition.wheel.SimpleCounter;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslateTest {
    private static final String ZH_CN = "zh_cn";
    private static final String EN_US = "en_us";
    private static final String PLACEHOLDER = "%(?:(\\d+)\\$)?([A-Za-z%]|$)";
    private final HashMap<String, TranslateParser> parsers = new HashMap<>();

    public TranslateTest() throws FileNotFoundException {
        for (String lang : List.of(ZH_CN, EN_US)) {
            parsers.put(lang, new TranslateParser(lang));
        }
    }

    /**
     * 比较中英文翻译的键的内容和顺序是否完全相同
     */
    @Test
    public void testKey() {
        List<String> zhKey = this.parsers.get(ZH_CN).listOtherTranslate().stream().map(Entry::key).toList();
        List<String> enKey = this.parsers.get(EN_US).listOtherTranslate().stream().map(Entry::key).toList();
        // 中英文翻译的差异
        boolean identical = true;
        StringJoiner error = new StringJoiner("\n", "中英文翻译键不匹配：\n", "");
        // 包含在中文翻译中，但在英文翻译中没有
        for (String key : zhKey) {
            if (enKey.contains(key)) {
                continue;
            }
            error.add("en_us缺失：" + key);
            identical = false;
        }
        // 包含在英文翻译中，但在中文翻译中没有
        for (String key : enKey) {
            if (zhKey.contains(key)) {
                continue;
            }
            error.add("zh_cn缺失：" + key);
            identical = false;
        }
        // 比较中英文规则翻译键
        List<Entry> zhRuleKey = this.parsers.get(ZH_CN).listRuleTranslate().stream().filter(Entry::isRuleNonName).toList();
        List<Entry> enRuleKey = this.parsers.get(EN_US).listRuleTranslate().stream().filter(Entry::isRuleNonName).toList();
        for (Entry entry : zhRuleKey) {
            if (enRuleKey.contains(entry)) {
                continue;
            }
            error.add("en_us缺失：" + entry.key());
            identical = false;
        }
        for (Entry entry : enRuleKey) {
            if (zhRuleKey.contains(entry)) {
                continue;
            }
            error.add("en_us缺失：" + entry.key());
            identical = false;
        }
        Assertions.assertTrue(identical, error.toString());
    }

    /**
     * 检查中英文翻译值占位符的个数是否一致
     */
    @Test
    public void testValue() {
        List<String> zhValue = this.parsers.get(ZH_CN).listOtherTranslate().stream().map(Entry::value).toList();
        List<String> enValue = this.parsers.get(EN_US).listOtherTranslate().stream().map(Entry::value).toList();
        int size = zhValue.size();
        if (size != enValue.size()) {
            Assertions.fail("中英文翻译键不相同");
        }
        boolean perfectMatch = true;
        StringJoiner error = new StringJoiner("\n", "中英文翻译值不匹配：\n", "");
        for (int index = 0; index < size; index++) {
            String zhTranslate = zhValue.get(index);
            String enTranslate = enValue.get(index);
            if (substringCount(zhTranslate) == substringCount(enValue.get(index))) {
                continue;
            }
            error.add(zhTranslate + " --- " + enTranslate);
            perfectMatch = false;
        }
        Assertions.assertTrue(perfectMatch, error.toString());
    }

    /**
     * @return 翻译中的占位符个数
     */
    private int substringCount(String value) {
        Matcher matcher = Pattern.compile(TranslateTest.PLACEHOLDER).matcher(value);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * 检查是否包含未使用的翻译键
     */
    @Test
    public void testUsage() throws IOException {
        File rootPath = new File("src/main/java/boat/carpetorgaddition");
        SimpleCounter<String> counter = new SimpleCounter<>();
        List<String> keys = this.parsers.get(ZH_CN)
                .listOtherTranslate()
                .stream()
                .filter(entry -> !entry.isCategory())
                .map(Entry::key)
                .toList();
        keys.forEach(key -> counter.set(key, 0));
        checkUsage(rootPath, keys, counter);
        // 所有未被使用的翻译键
        List<String> list = counter.stream().map(Map.Entry::getKey).filter(s -> counter.getCount(s) == 0).toList();
        StringJoiner errorReport = new StringJoiner("\n", "包含未使用的翻译键：\n", "");
        for (String key : list) {
            errorReport.add(key);
        }
        Assertions.assertTrue(list.isEmpty(), errorReport.toString());
    }

    // 递归遍历所有源代码文件
    private void checkUsage(File root, List<String> notRule, SimpleCounter<String> counter) throws IOException {
        // 检查当前文件是否是文件夹
        File[] files = root.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile()) {
                // 检查当前文件是否是Java文件
                if (file.getName().endsWith(".java")) {
                    readFile(file, notRule, counter);
                }
            } else if (file.isDirectory()) {
                // 递归遍历所有Java文件
                checkUsage(file, notRule, counter);
            }
        }
    }

    private void readFile(File java, List<String> notRule, SimpleCounter<String> counter) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(java));
        try (reader) {
            String line;
            while ((line = reader.readLine()) != null) {
                for (String key : notRule) {
                    int index = 0;
                    while ((line.indexOf("\"" + key + "\"", index)) != -1) {
                        index++;
                        counter.increment(key);
                    }
                }
            }
        }
    }

    /**
     * 检查是否有未使用的规则翻译键
     */
    @Test
    public void testRuleKeyUsage() {
        Set<String> set = this.parsers.get(ZH_CN).listAllRule();
        List<String> list = CarpetOrgAdditionSettings.listRules().stream().map(RuleContext::getName).toList();
        if (set.size() == list.size()) {
            return;
        }
        StringJoiner sj = new StringJoiner("\n", "包含未定义的规则翻译：\n", "");
        boolean unused = false;
        for (String str : set) {
            if (list.contains(str)) {
                continue;
            }
            unused = true;
            sj.add(str);
        }
        Assertions.assertFalse(unused, sj.toString());
    }

    /**
     * 检查英文翻译中是否包含中文字符
     */
    @Test
    public void testChineseCharacter() {
        List<Entry> list = this.parsers.get(EN_US).listAll();
        StringJoiner sj = new StringJoiner("\n", "英文翻译中不应包含中文字符：\n", "");
        boolean hasChineseCharacter = false;
        for (Entry entry : list) {
            String value = entry.value();
            if (value.contains("更新抑制器")) {
                continue;
            }
            Matcher matcher = Pattern.compile("[\\u4e00-\\u9fa5]|[，：（）]").matcher(value);
            if (matcher.find()) {
                hasChineseCharacter = true;
                sj.add(entry.toString());
            }
        }
        Assertions.assertFalse(hasChineseCharacter, sj.toString());
    }

    /**
     * 检查中文翻译中是否包含英文字符
     */
    @Test
    public void testEnglishCharacter() {
        List<Entry> list = this.parsers.get(ZH_CN).listAll();
        StringJoiner sj = new StringJoiner("\n", "中文翻译中不应包含英文字符：\n", "");
        boolean hasChineseCharacter = false;
        for (Entry entry : list) {
            String value = entry.value();
            if (value.contains("Mismatch in destroy block pos: {} {}") || value.contains("Carpet Mod中符合“%s”的选项：:") || value.matches("%s \\([+\\-]%s\\) \\[%s->%s]")) {
                continue;
            }
            Matcher matcher = Pattern.compile("[,:()]").matcher(value);
            // 中文冒号后不加空格
            if (matcher.find() || value.contains("： ")) {
                hasChineseCharacter = true;
                sj.add(entry.toString());
            }
        }
        Assertions.assertFalse(hasChineseCharacter, sj.toString());
    }

    @Test
    public void testUnuse() throws IOException {
        @Language("json") String lang = """
                {
                  "carpet.generic.data.type.string": "字符串",
                  "carpet.generic.data.type.integer": "整数",
                  "carpet.generic.data.type.long": "长整数",
                  "carpet.generic.data.type.boolean": "布尔",
                  "carpet.generic.data.type.double": "浮点数",
                  "carpet.generic.data.type.float": "浮点数",
                  "carpet.generic.data.type.enum": "枚举",
                  "carpet.generic.undefined": "未定义",
                  "carpet.generic.operator": "管理员",
                  "carpet.generic.inventory": "物品栏",
                  "carpet.generic.ender_chest": "末影箱",
                  "carpet.command.not_fake_player": "%s不是假玩家",
                  "carpet.command.self_or_fake_player": "只允许操作自己或假玩家",
                  "carpet.command.uuid.parse.fail": "无法从字符串解析UUID",
                  "carpet.command.argument.player.toomany": "只允许一名玩家，但目标选择器返回了多名玩家",
                  "carpet.command.dimension.overworld": "主世界",
                  "carpet.command.dimension.the_nether": "下界",
                  "carpet.command.dimension.the_end": "末地",
                  "carpet.command.block_pos.dimension": "%s[%s]",
                  "carpet.command.boolean.true": "是",
                  "carpet.command.boolean.false": "否",
                  "carpet.command.time.tick": "%s游戏刻",
                  "carpet.command.time.second": "%s秒",
                  "carpet.command.time.minute": "%s分钟",
                  "carpet.command.time.hour": "%s小时",
                  "carpet.command.time.minute_second": "%s分%s秒",
                  "carpet.command.time.hour_minute": "%s小时%s分",
                  "carpet.command.time.format": "%s年%s月%s日%s时%s分%s秒",
                  "carpet.command.item.group": "%s组",
                  "carpet.command.item.remainder": "%s个",
                  "carpet.command.item.count": "%s组%s个",
                  "carpet.command.item.item": "物品",
                  "carpet.command.item.predicate.wildcard": "任意物品",
                  "carpet.command.text.click.here": "[这里]",
                  "carpet.command.text.click.input": "单击输入\\"%s\\"",
                  "carpet.command.text.click.run": "单击执行\\"%s\\"",
                  "carpet.command.text.click.login": "点击上线",
                  "carpet.command.text.self": "自己",
                  "carpet.command.file.name.valid": "无效的文件名称",
                  "carpet.command.thread.wait.last": "等待上一个线程执行完毕",
                  "carpet.command.task.wait.last": "等待上一个任务执行完毕",
                  "carpet.command.task.timeout": "任务超时",
                  "carpet.command.error.io": "发生IO错误",
                  "carpet.command.operation.timeout": "操作超时",
                  "carpet.command.data.unit.byte": "%s字节",
                  "carpet.command.page.prev": "上一页",
                  "carpet.command.page.next": "下一页",
                  "carpet.command.page.invalid": "试图访问第%s页，但总页数为%s",
                  "carpet.command.page.non_existent": "页面不存在或已被回收",
                  "carpet.command.permission.insufficient": "权限不足！",
                  "carpet.commands.finder.toobig": "查找范围水平跨度不能超过%s格",
                  "carpet.commands.finder.block.too_much_blocks": "周围%s过多，无法统计",
                  "carpet.commands.finder.block.not_found_block": "在指定范围内找不到%s",
                  "carpet.commands.finder.block.find": "在指定范围内找到%s个%s",
                  "carpet.commands.finder.block.feedback": "在%s找到了%s个%s",
                  "carpet.commands.finder.may_affect_world_eater_block.name": "可能会影响世吞运行的方块",
                  "carpet.commands.finder.item.drops": "掉落物",
                  "carpet.commands.finder.item.each": "在位于%s的%s中找到%s个",
                  "carpet.commands.finder.item.too_much_container": "周围包含%s的容器过多，无法统计",
                  "carpet.commands.finder.item.find.not_item": "在周围的容器中找不到%s",
                  "carpet.commands.finder.item.find": "在周围%s个容器中找到%s个%s",
                  "carpet.commands.finder.item.offline_player": "在%s个离线玩家的物品栏中找到了%s个%s：",
                  "carpet.commands.finder.item.offline_player.not_found": "未在离线玩家的物品栏中找到%s",
                  "carpet.commands.finder.item.offline_player.each": "在%s的%s找到了%s个",
                  "carpet.commands.finder.item.offline_player.prompt": "如果未在召唤出来的玩家的物品栏找到你需要的物品，\\n请检查玩家UUID与命令反馈中的UUID是否一致",
                  "carpet.commands.finder.item.offline_player.query.name": "单击以通过Mojang API查询玩家名称",
                  "carpet.commands.finder.item.offline_player.query.non_authentic": "请勿使用此功能查询非正版玩家的玩家名称",
                  "carpet.commands.finder.item.offline_player.total": "总计：%s",
                  "carpet.commands.finder.item.offline_player.found": "已找到：%s",
                  "carpet.commands.finder.item.offline_player.open.inventory": "单击以打开玩家物品栏",
                  "carpet.commands.finder.item.offline_player.open.ender_chest": "单击以打开玩家末影箱",
                  "carpet.commands.finder.item.offline_player.container.inventory": "物品栏",
                  "carpet.commands.finder.item.offline_player.container.enderchest": "末影箱",
                  "carpet.commands.finder.trade.item.not_trade": "在周围找不到出售%s的%s",
                  "carpet.commands.finder.trade.item.each": "位于%s的%s的第%s项交易出售该物品",
                  "carpet.commands.finder.trade.item.result": "在周围找到了%s个出售%s的%s",
                  "carpet.commands.finder.trade.enchanted_book.not_trade": "在周围找不到出售%s附魔书的%s",
                  "carpet.commands.finder.trade.enchanted_book.each": "位于%s的%s的第%s项交易出售%s",
                  "carpet.commands.finder.trade.enchanted_book.result": "在周围找到了%s个出售%s附魔书的%s",
                  "carpet.commands.itemshadowing.main_hand_is_empty": "主手为空",
                  "carpet.commands.itemshadowing.off_hand_not_empty": "副手不为空",
                  "carpet.commands.itemshadowing.broadcast": "%s制作了一个%s的物品分身",
                  "carpet.commands.killMe.suicide": "%s残酷地结束了自己的生命",
                  "carpet.commands.locations.show.overworld": "%s位于主世界：%s",
                  "carpet.commands.locations.show.overworld_and_the_nether": "%s位于主世界：%s（下界：%s）",
                  "carpet.commands.locations.show.the_nether": "%s位于下界：%s",
                  "carpet.commands.locations.show.the_nether_and_overworld": "%s位于下界：%s（主世界：%s）",
                  "carpet.commands.locations.show.the_end": "%s位于末地：%s",
                  "carpet.commands.locations.show.custom_dimension": "%s位于%s：%s",
                  "carpet.commands.locations.add.fail.already_exists": "无法添加路径点：[%s]已存在",
                  "carpet.commands.locations.add.success": "路径点[%s]已添加（%s）",
                  "carpet.commands.locations.list.parse": "无法解析坐标[%s]",
                  "carpet.commands.locations.list.no_waypoint": "没有路径点被列出",
                  "carpet.commands.locations.comment.remove": "移除了路径点[%s]的注释",
                  "carpet.commands.locations.comment.add": "将注释“%s”添加到[%s]中",
                  "carpet.commands.locations.comment.io": "无法为路径点[%s]添加注释",
                  "carpet.commands.locations.another.add": "另一个坐标已添加",
                  "carpet.commands.locations.another.add.fail": "只允许为主世界和下界添加另一个坐标",
                  "carpet.commands.locations.another.io": "无法为路径点[%s]添加另一个坐标",
                  "carpet.commands.locations.remove.success": "路径点[%s]已删除",
                  "carpet.commands.locations.remove.fail": "无法删除路径点[%s]",
                  "carpet.commands.locations.set": "已修改路径点[%s]的位置",
                  "carpet.commands.locations.set.io": "无法修改路径点[%s]的坐标",
                  "carpet.commands.locations.here.overworld": "%s在主世界：%s <-- %s",
                  "carpet.commands.locations.here.the_nether": "%s在下界：%s <-- %s",
                  "carpet.commands.locations.here.the_end": "%s在末地：%s",
                  "carpet.commands.locations.here.default": "%s在%s：%s",
                  "carpet.commands.xpTransfer.all": "将%s所有的经验共%s转移给%s",
                  "carpet.commands.xpTransfer.half": "将%s一半的经验共%s转移给%s",
                  "carpet.commands.xpTransfer.point.fail": "%s没有这么多经验（%s > %s）",
                  "carpet.commands.xpTransfer.point": "将%s的%s点经验转移给%s",
                  "carpet.commands.xpTransfer.upgradeto.negative": "升级所需经验为负数",
                  "carpet.commands.xpTransfer.calculate.fail": "无法计算升级所需的经验",
                  "carpet.commands.xpTransfer.upgrade": "%s (+%s) [%s->%s]",
                  "carpet.commands.xpTransfer.degrade": "%s (-%s) [%s->%s]",
                  "carpet.commands.playerAction.info.stop": "%s没有任何动作",
                  "carpet.commands.playerAction.info.craft.no_crafting_table": "%s没有打开%s",
                  "carpet.commands.playerAction.info.craft.result": "%s正在合成%s：",
                  "carpet.commands.playerAction.info.craft.state": "%s当前合成物品的状态：",
                  "carpet.commands.playerAction.info.craft.gui": "设置合成配方",
                  "carpet.commands.playerAction.info.sorting.predicate": "%s正在分拣%s：",
                  "carpet.commands.playerAction.info.sorting.this": "%s：%s",
                  "carpet.commands.playerAction.info.sorting.other": "其他物品：%s",
                  "carpet.commands.playerAction.info.clean.predicate": "%s正在清空容器中的%s",
                  "carpet.commands.playerAction.info.fill.predicate": "%s正在向容器填充%s",
                  "carpet.commands.playerAction.info.rename.item": "%s正在将%s重命名为“%s”",
                  "carpet.commands.playerAction.info.rename.xp": "剩余%s级经验",
                  "carpet.commands.playerAction.info.rename.no_anvil": "%s没有打开%s",
                  "carpet.commands.playerAction.info.stonecutting.item": "%s正在使用%s和%s制作%s",
                  "carpet.commands.playerAction.info.stonecutting.button": "按钮索引：%s",
                  "carpet.commands.playerAction.info.stonecutting.no_stonecutting": "%s没有打开%s",
                  "carpet.commands.playerAction.info.stonecutter.gui": "设置切石机配方",
                  "carpet.commands.playerAction.info.trade.item": "%s正在交易第%s个选项中的物品",
                  "carpet.commands.playerAction.info.trade.disabled": "交易已被锁定",
                  "carpet.commands.playerAction.info.trade.state": "交易状态：",
                  "carpet.commands.playerAction.info.trade.no_villager": "%s没有打开交易GUI",
                  "carpet.commands.playerAction.info.fishing": "%s正在钓鱼",
                  "carpet.commands.playerAction.info.farm": "%s正在种植",
                  "carpet.commands.playerAction.info.bedrock": "%s正在破基岩",
                  "carpet.commands.playerAction.info.bedrock.cuboid.range": "范围：从%s到%s",
                  "carpet.commands.playerAction.info.bedrock.cylinder.center": "中心：%s",
                  "carpet.commands.playerAction.info.bedrock.cylinder.radius": "半径：%s格",
                  "carpet.commands.playerAction.info.bedrock.cylinder.height": "高度：%s格",
                  "carpet.commands.playerAction.info.bedrock.ai.enable": "AI已启用",
                  "carpet.commands.playerAction.info.goto.block": "%s正在移动到%s",
                  "carpet.commands.playerAction.info.goto.entity": "%s正在跟随%s",
                  "carpet.commands.playerAction.stone_cutting": "无法合成物品，停止合成",
                  "carpet.commands.playerAction.trade": "无法交易物品：停止交易",
                  "carpet.commands.playerAction.craft": "无法合成物品，停止合成",
                  "carpet.commands.playerAction.rename.not_experience": "经验不足，停止重命名",
                  "carpet.commands.playerAction.rename": "无法重命名，操作自动停止",
                  "carpet.commands.playerAction.bedrock.share": "请勿向其他人传播自动破基岩功能",
                  "carpet.commands.playerAction.exception.runtime": "%s在%s时遇到了意料之外的错误",
                  "carpet.commands.playerAction.action.stop": "停止",
                  "carpet.commands.playerAction.action.sorting": "分拣物品",
                  "carpet.commands.playerAction.action.clean": "清空容器",
                  "carpet.commands.playerAction.action.fill": "填充容器",
                  "carpet.commands.playerAction.action.crafting_table_craft": "工作台合成物品",
                  "carpet.commands.playerAction.action.inventory_craft": "物品栏合成物品",
                  "carpet.commands.playerAction.action.rename": "重命名物品",
                  "carpet.commands.playerAction.action.stonecutting": "切石",
                  "carpet.commands.playerAction.action.trade": "交易",
                  "carpet.commands.playerAction.action.fishing": "钓鱼",
                  "carpet.commands.playerAction.action.farm": "种植",
                  "carpet.commands.playerAction.action.bedrock": "破基岩",
                  "carpet.commands.playerAction.action.goto": "行走",
                  "carpet.commands.playerManager.cannot_find_file": "找不到%s的玩家数据",
                  "carpet.commands.playerManager.reload": "已重新加载玩家数据",
                  "carpet.commands.playerManager.save.success": "已保存%s的玩家数据",
                  "carpet.commands.playerManager.save.file_already_exist": "玩家数据已存在，点击%s重新保存",
                  "carpet.commands.playerManager.save.resave": "已重新保存%s的玩家数据",
                  "carpet.commands.playerManager.spawn.fail": "尝试生成假玩家时出现意外问题",
                  "carpet.commands.playerManager.spawn.player_exist": "玩家已存在",
                  "carpet.commands.playerManager.comment.modify": "将%s玩家数据的注释设置为[%s]",
                  "carpet.commands.playerManager.comment.remove": "移除了%s玩家数据的注释",
                  "carpet.commands.playerManager.autologin.setup": "已为%s设置自动登录",
                  "carpet.commands.playerManager.autologin.cancel": "取消设置%s的自动登录",
                  "carpet.commands.playerManager.delete.success": "已成功删除玩家数据",
                  "carpet.commands.playerManager.delete.non_existent": "玩家不存在",
                  "carpet.commands.playerManager.delete.fail": "未能成功删除玩家数据",
                  "carpet.commands.playerManager.click.online": "点击上线",
                  "carpet.commands.playerManager.click.offline": "点击下线",
                  "carpet.commands.playerManager.info.pos": "玩家位置：[%s]",
                  "carpet.commands.playerManager.info.direction": "玩家朝向：[%s，%s]",
                  "carpet.commands.playerManager.info.dimension": "维度：%s",
                  "carpet.commands.playerManager.info.gamemode": "游戏模式：%s",
                  "carpet.commands.playerManager.info.flying": "是否飞行：%s",
                  "carpet.commands.playerManager.info.sneaking": "是否潜行：%s",
                  "carpet.commands.playerManager.info.autologin": "自动登录：%s",
                  "carpet.commands.playerManager.info.comment": "注释：%s",
                  "carpet.commands.playerManager.info.left_click": "左键：",
                  "carpet.commands.playerManager.info.right_click": "右键：",
                  "carpet.commands.playerManager.info.continuous": "长按",
                  "carpet.commands.playerManager.info.interval": "间隔%s游戏刻",
                  "carpet.commands.playerManager.info.action": "动作：",
                  "carpet.commands.playerManager.info.startup": "登录时：",
                  "carpet.commands.playerManager.info.startup.delay": "延迟：%s刻",
                  "carpet.commands.playerManager.info.startup.attack": "左键单击",
                  "carpet.commands.playerManager.info.startup.use": "右键单击",
                  "carpet.commands.playerManager.info.startup.kill": "退出游戏",
                  "carpet.commands.playerManager.list.no_player": "没有玩家被列出",
                  "carpet.commands.playerManager.list.expand": "点击组名称展开玩家列表：",
                  "carpet.commands.playerManager.group.add": "玩家%s已添加到组[%s]",
                  "carpet.commands.playerManager.group.remove": "玩家%s已从组[%s]中移除",
                  "carpet.commands.playerManager.group.remove.fail": "无变化，该玩家本就不在这个组中",
                  "carpet.commands.playerManager.group.player": "此组共包含%s名玩家",
                  "carpet.commands.playerManager.group.name.ungrouped": "[未分组]",
                  "carpet.commands.playerManager.group.name.all": "[所有]",
                  "carpet.commands.playerManager.group.non_existent": "没有名称为[%s]的组",
                  "carpet.commands.playerManager.group.list": "位于组[%s]的中的玩家：",
                  "carpet.commands.playerManager.group.list.ungrouped": "未分组的玩家：",
                  "carpet.commands.playerManager.group.list.all": "所有玩家：",
                  "carpet.commands.playerManager.schedule.login": "%s将于%s后上线",
                  "carpet.commands.playerManager.schedule.login.try": "%s将于%s后再次尝试上线",
                  "carpet.commands.playerManager.schedule.login.modify": "%s的上线时间修改为%s后",
                  "carpet.commands.playerManager.schedule.login.cancel": "取消了%s于%s后上线的计划",
                  "carpet.commands.playerManager.schedule.logout": "%s将于%s后下线",
                  "carpet.commands.playerManager.schedule.logout.modify": "%s的下线时间修改为%s后",
                  "carpet.commands.playerManager.schedule.logout.cancel": "取消了%s于%s后下线的计划",
                  "carpet.commands.playerManager.schedule.relogin": "%s每%s游戏刻重新上线一次",
                  "carpet.commands.playerManager.schedule.relogin.condition": "此功能需要启用“假玩家生成内存泄漏修复”，点击%s启用规则",
                  "carpet.commands.playerManager.schedule.relogin.set_interval": "将%s重新上线时间周期更改为%s游戏刻",
                  "carpet.commands.playerManager.schedule.relogin.cancel": "结束了%s的周期性重新上线任务",
                  "carpet.commands.playerManager.schedule.relogin.rule.disable": "规则“假玩家生成内存泄漏修复”未启用",
                  "carpet.commands.playerManager.schedule.list.empty": "计划列表中没有玩家",
                  "carpet.commands.playerManager.schedule.cancel.fail": "没有计划被取消",
                  "carpet.commands.playerManager.safeafk.successfully_set_up": "将%s的安全挂机阈值设置为%s，点击%s永久更改",
                  "carpet.commands.playerManager.safeafk.successfully_set_up.save": "%s的安全挂机阈值已永久更改为%s",
                  "carpet.commands.playerManager.safeafk.successfully_set_up.cancel": "已取消设置%s的安全挂机，点击%s永久更改",
                  "carpet.commands.playerManager.safeafk.successfully_set_up.remove": "已取消永久更改%s的安全挂机阈值",
                  "carpet.commands.playerManager.safeafk.successfully_set_up.auto": "将%s的安全挂机阈值设置为%s",
                  "carpet.commands.playerManager.safeafk.threshold_too_high": "安全挂机阈值必须小于假玩家最大生命值",
                  "carpet.commands.playerManager.safeafk.list.each": "%s的安全挂机阈值为%s",
                  "carpet.commands.playerManager.safeafk.list.empty": "没有玩家被设置安全挂机",
                  "carpet.commands.playerManager.safeafk.trigger.success": "%s在生命值剩余%s时触发了安全挂机",
                  "carpet.commands.playerManager.safeafk.trigger.fail": "%s安全挂机触发失败",
                  "carpet.commands.playerManager.safeafk.info.attacker": "攻击者：%s",
                  "carpet.commands.playerManager.safeafk.info.source": "伤害来源：%s",
                  "carpet.commands.playerManager.safeafk.info.amount": "伤害大小：%s",
                  "carpet.commands.playerManager.safeafk.info.type": "伤害类型：%s",
                  "carpet.commands.playerManager.batch.exceeds_limit": "预期召唤的玩家数为%s，但单次最大召唤数量为%s个",
                  "carpet.commands.playerManager.batch.preload": "正在预加载玩家档案：%s/%s",
                  "carpet.commands.playerManager.batch.preload.done": "预加载完成",
                  "carpet.commands.playerManager.batch.summoner": "%s召唤了%s-%s共%s名玩家",
                  "carpet.commands.spectator.teleport.fail": "你当前未处于%s",
                  "carpet.commands.spectator.teleport.success.dimension": "已将%s传送至%s",
                  "carpet.commands.navigate.start_navigation": "%s开始导航 -> %s",
                  "carpet.commands.navigate.parse_uuid_fail": "无法从字符串解析UUID",
                  "carpet.commands.navigate.unable_to_find": "查询不到%s的%s",
                  "carpet.commands.navigate.name.spawnpoint": "重生点",
                  "carpet.commands.navigate.name.last_death_location": "上一次死亡位置",
                  "carpet.commands.navigate.exception": "导航器遇到了意料之外的错误",
                  "carpet.commands.navigate.hud.reach": "已到达目的地",
                  "carpet.commands.navigate.hud.of": "%s的%s",
                  "carpet.commands.navigate.hud.in": "%s在%s",
                  "carpet.commands.navigate.hud.distance": "距离：%s格",
                  "carpet.commands.navigate.hud.target_death": "目标已死亡或已被清除",
                  "carpet.commands.navigate.hud.stop": "导航已结束",
                  "carpet.commands.mail.structure": "发送快递前请将要发送的物品放在手上",
                  "carpet.commands.mail.sending.sender": "向%s发送了%s个%s，点击%s撤回",
                  "carpet.commands.mail.sending.recipient": "%s向你发送了%s个%s，点击%s接收",
                  "carpet.commands.mail.sending.permission": "对方可能没有执行接收物品命令的权限",
                  "carpet.commands.mail.sending.offline_player": "对方正处于离线状态，物品将在该玩家上线后送达",
                  "carpet.commands.mail.sending.notice": "%s接收了一件你发送的快递",
                  "carpet.commands.mail.sending.multiple": "向%s发送了%s个%s，点击%s全部撤回",
                  "carpet.commands.mail.receive.success": "成功接收了%s个%s",
                  "carpet.commands.mail.receive.insufficient_capacity": "快递未能成功接收，请检查物品栏是否有充足容量",
                  "carpet.commands.mail.receive.partial_reception": "接收了%s个物品，但仍有%s个物品未能接收，请检查物品栏是否有充足容量",
                  "carpet.commands.mail.receive.recipient": "你只能接收发送给自己的快递",
                  "carpet.commands.mail.receive.non_existent": "不存在单号为%s的快递",
                  "carpet.commands.mail.receive.all.non_existent": "没有快递可以接收",
                  "carpet.commands.mail.receive.cancel": "当前快递已被对方撤回",
                  "carpet.commands.mail.receive.multiple": "%s向你发送了%s个%s，点击%s全部接收",
                  "carpet.commands.mail.cancel.success": "成功撤回了%s个%s",
                  "carpet.commands.mail.cancel.insufficient_capacity": "快递未能成功撤回，请检查物品栏是否有充足容量",
                  "carpet.commands.mail.cancel.partial_reception": "撤回了%s个物品，但仍有%s个物品未能撤回，请检查物品栏是否有充足容量",
                  "carpet.commands.mail.cancel.notice": "%s撤回了一件发送给你快递",
                  "carpet.commands.mail.cancel.recipient": "你只能撤回自己发送的快递",
                  "carpet.commands.mail.cancel.all.non_existent": "没有快递可以撤回",
                  "carpet.commands.mail.intercept.success": "成功拦截了%s个%s",
                  "carpet.commands.mail.intercept.insufficient_capacity": "快递未能成功拦截，请检查物品栏是否有充足容量",
                  "carpet.commands.mail.intercept.partial_reception": "拦截了%s个物品，但仍有%s个物品未能拦截，请检查物品栏是否有充足容量",
                  "carpet.commands.mail.intercept.notice.sender": "%s拦截了一件你发送给%s的快递",
                  "carpet.commands.mail.intercept.notice.recipient": "%s拦截了一件%s发送给你的快递",
                  "carpet.commands.mail.action.receive": "接收",
                  "carpet.commands.mail.action.cancel": "撤回",
                  "carpet.commands.mail.action.intercept": "拦截",
                  "carpet.commands.mail.action.version.fail": "无法%s物品，该物品是在%s的Minecraft版本中发送的，点击%s强制%s",
                  "carpet.commands.mail.action.version.new": "更新",
                  "carpet.commands.mail.action.version.old": "更旧",
                  "carpet.commands.mail.action.version.expect": "预期版本：%s",
                  "carpet.commands.mail.action.version.actua": "实际版本：%s",
                  "carpet.commands.mail.list.each": "单号：%s，物品：%s，发件人：%s，收件人：%s %s",
                  "carpet.commands.mail.list.sending": "点击撤回",
                  "carpet.commands.mail.list.receive": "点击接收",
                  "carpet.commands.mail.list.intercept": "点击拦截",
                  "carpet.commands.mail.list.id": "单号：%s",
                  "carpet.commands.mail.list.sender": "发件人：%s",
                  "carpet.commands.mail.list.recipient": "收件人：%s",
                  "carpet.commands.mail.list.item": "物品：%s*%s",
                  "carpet.commands.mail.list.time": "时间：%s",
                  "carpet.commands.mail.list.empty": "没有快递被列出",
                  "carpet.commands.mail.prompt_receive": "你有一件包含%s个%s的快递尚未接收，点击%s接收",
                  "carpet.commands.mail.check_player": "不能将快递发送给自己和假玩家",
                  "carpet.commands.mail.multiple.gui": "发送物品",
                  "carpet.commands.mail.multiple.error": "批量发送物品时遇到意外错误",
                  "carpet.commands.ruleSearch.feedback": "Carpet Mod中符合“%s”的选项：:",
                  "carpet.commands.orange.version": "%s的版本为：%s",
                  "carpet.commands.orange.permission.node.not_found": "命令权限节点不存在",
                  "carpet.commands.orange.permission.value.invalid": "无效的命令权限等级",
                  "carpet.commands.orange.ruleself.failed": "无效或不支持单独开关的规则",
                  "carpet.commands.orange.ruleself.enable": "规则“%s”现在对%s启用",
                  "carpet.commands.orange.ruleself.disable": "规则“%s”现在对%s禁用",
                  "carpet.commands.orange.ruleself.invalid": "无效，因为服务器不允许玩家自行启用规则",
                  "carpet.commands.orange.ruleself.info.player": "玩家：%s",
                  "carpet.commands.orange.ruleself.info.rule": "规则：%s",
                  "carpet.commands.orange.ruleself.info.enable": "已启用：%s",
                  "carpet.commands.runtime.memory.jvm": "JVM内存：",
                  "carpet.commands.runtime.memory.jvm.used": "- 已使用：%s",
                  "carpet.commands.runtime.memory.jvm.total": "- 已分配：%s",
                  "carpet.commands.runtime.memory.jvm.max": "- 最大可分配（总内存）：%s",
                  "carpet.commands.runtime.memory.physical": "物理内存：",
                  "carpet.commands.runtime.memory.physical.total": "- 总内存：%s",
                  "carpet.commands.runtime.memory.physical.used": "- 已使用：%s",
                  "carpet.commands.runtime.gc": "已清理约%s的内存",
                  "carpet.commands.runtime.gc.prompt": "此功能对服务器优化§l没有任何帮助§r",
                  "carpet.commands.player.inventory.offline.display_name": "%s（离线）",
                  "carpet.commands.player.inventory.offline.no_file_found": "未找到该玩家的存储文件",
                  "carpet.commands.player.inventory.offline.permission": "打开白名单玩家物品栏需要OP权限",
                  "carpet.commands.player.tp.success": "将%s传送至%s",
                  "carpet.logger.wanderingTrader.time.second": "%s秒",
                  "carpet.logger.wanderingTrader.time.minutes": "%s分",
                  "carpet.logger.wanderingTrader.time.minutes_and_seconds": "%s分%s秒",
                  "carpet.logger.wanderingTrader.hud": "流浪商人将于%s后尝试生成，成功率%s",
                  "carpet.logger.wanderingTrader.message": "流浪商人在%s成功生成",
                  "carpet.logger.wanderingTrader.message.click": "流浪商人在%s成功生成，点击%s",
                  "carpet.logger.wanderingTrader.message.navigate": "[导航]",
                  "carpet.logger.wanderingTrader.message.navigate.hover": "点击导航到此%s",
                  "carpet.logger.wanderingTrader.gamerule.not_enabled": "游戏规则“%s”未启用",
                  "carpet.logger.fishing.appear": "距离鱼出现还有%s刻",
                  "carpet.logger.fishing.bite": "距离鱼上钩还有%s刻",
                  "carpet.logger.fishing.break_free": "距离鱼挣脱还有%s刻",
                  "carpet.logger.obsidian.generate": "在%s处生成了一块黑曜石",
                  "carpet.dialog.metadata.version": "版本：%s",
                  "carpet.dialog.function.title": "功能",
                  "carpet.dialog.function.open_inventory.title": "打开玩家物品栏",
                  "carpet.dialog.function.query_player_name.title": "查询玩家名称",
                  "carpet.dialog.function.uuid.textbox": "UUID",
                  "carpet.dialog.generic.back": "返回",
                  "carpet.dialog.generic.entry": "确定",
                  "carpet.dialog.generic.close": "关闭",
                  "carpet.dialog.generic.error": "错误",
                  "carpet.clickevent.uuid.from_string.fail": "字符串“%s”无法解析为UUID",
                  "carpet.clickevent.open_inventory.fail": "此功能只能用于打开离线玩家物品栏",
                  "carpet.clickevent.query_player_name.start": "正在通过Mojang API查询玩家名称",
                  "carpet.clickevent.query_player_name.success": "此%s对应的玩家名是[%s]",
                  "carpet.clickevent.query_player_name.fail": "查询不到此%s对应的玩家名",
                  "carpet.inventory.item.placeholder": "占位",
                  "carpet.client.command.matching_pattern.invalid": "无效的匹配模式",
                  "carpet.client.command.string.broad": "字符串过于宽泛",
                  "carpet.client.commands.highlight": "点击以高亮路径点",
                  "carpet.client.commands.dictionary.not_matched": "字符串与任何对象都不匹配",
                  "carpet.client.commands.dictionary.id": "%s的ID是：%s",
                  "carpet.client.commands.dictionary.multiple.id": "字符串与%s个对象匹配：",
                  "carpet.client.commands.dictionary.multiple.each": "- %s",
                  "carpet.client.render.waypoint.error": "渲染路径点时遇到意外错误",
                  "carpet.client.key.keyboard.waypoint.clear": "清除高亮路径点"
                }
                """;
        JsonObject json = IOUtils.GSON.fromJson(lang, JsonObject.class);
        List<String> keys = json.entrySet().stream().map(Map.Entry::getKey).toList();
        File root = new File("src/main/java/boat/carpetorgaddition");
        Map<String, Map<File, IntList>> map = new TreeMap<>();
        this.traverse(root, keys, map);
        Assertions.assertTrue(map.isEmpty(), () -> {
            StringBuilder builder = new StringBuilder();
            builder.append("%s个翻译键仍在使用\n".formatted(map.size()));
            for (Map.Entry<String, Map<File, IntList>> entry : map.entrySet()) {
                builder.append(entry.getKey()).append(" 用法：\n");
                Map<File, IntList> files = entry.getValue();
                for (Map.Entry<File, IntList> file : files.entrySet()) {
                    builder.append("  - ").append(file.getKey().getName()).append(": 第").append(file.getValue()).append("行仍在使用\n");
                }
                builder.append("\n");
            }
            return builder.toString();
        });
    }


    private void traverse(File path, List<String> keys, Map<String, Map<File, IntList>> map) throws IOException {
        HashMap<File, ArrayList<String>> codes = new HashMap<>();
        this.traverse(path, codes);
        for (String key : keys) {
            for (Map.Entry<File, ArrayList<String>> entry : codes.entrySet()) {
                ArrayList<String> list = entry.getValue();
                File file = entry.getKey();
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).contains(key)) {
                        Map<File, IntList> files = map.computeIfAbsent(key, _ -> new TreeMap<>());
                        IntList lineNumbers = files.computeIfAbsent(file, _ -> new IntArrayList());
                        lineNumbers.add(i);
                    }
                }
            }
        }
    }


    private void traverse(File path, Map<File, ArrayList<String>> codes) throws IOException {
        File[] files = path.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile()) {
                if (file.getName().endsWith(".java")) {
                    ArrayList<String> list = codes.computeIfAbsent(file, _ -> new ArrayList<>());
                    BufferedReader reader = IOUtils.toReader(file);
                    try (reader) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            list.add(line);
                        }
                    }
                }
            } else if (file.isDirectory()) {
                traverse(file, codes);
            }
        }
    }
}
