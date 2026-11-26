package net.alex.guzhenren.datagen.lang;

import java.util.HashMap;
import java.util.Map;

/**
 * 主模组繁体中文（{@code zh_tw}）的简繁字形转换表。
 * <ul>
 *   <li>繁体中文保持原有用词而仅按台湾正体字标准（OpenCC {@code s2tw}，如用「為」不用「爲」）转换字形，由
 *       {@link ModLanguageProvider} 对未显式指定繁体文本的词条调用 {@link #convert} 生成。</li>
 *   <li>{@link #PAIRS} 收录单字映射，{@link #PHRASES} 收录多字词组，{@link #UNCHANGED} 收录简繁同形字；每个映射词元按
 *       「前一半简体、后一半繁体」编码（如 {@code 蛊蠱}、{@code 冲刷沖刷}），由 {@link #convert} 从左至右按最长前缀匹配
 *       替换，使词组优先于单字。</li>
 *   <li>⚠ 在游戏文本中存在一对多歧义的常见字（如「冲」「制」「只」「准」「复」「回」「尽」「喂」「舍」「游」「斗」
 *       「周」）刻意不放入 {@link #PAIRS} 或 {@link #UNCHANGED}，强制要求按具体语境以整词形式录入
 *       {@link #PHRASES}。</li>
 *   <li>⚠ {@link #convert} 遇到未收录的汉字会直接抛出 {@link IllegalArgumentException}，使包含未知字形的新增简体词条在
 *       {@code runData} 与单元测试中立即失败，防止简体字混入繁体语言文件。</li>
 * </ul>
 */

public final class TraditionalGlyphs {

    private static final String PAIRS = "万萬 径徑 与與 两兩 个個 临臨 为為 云雲 亚亞 亿億 仪儀 会會 伤傷 体體 余餘 储儲 僵殭 兰蘭 关關 养養 兽獸 内內 冻凍 击擊 纳納 "
            + "创創 剑劍 动動 华華 压壓 变變 叶葉 图圖 头頭 学學 宝寶 寿壽 将將 尸屍 层層 岁歲 巅巔 过過 载載 "
            + "师師 库庫 开開 强強 彻徹 态態 恶惡 悬懸 惯慣 战戰 担擔 择擇 损損 数數 斩斬 断斷 无無 时時 旷曠 "
            + "晋晉 晓曉 机機 杀殺 枣棗 标標 梦夢 横橫 气氣 没沒 泽澤 测測 温溫 满滿 灭滅 灾災 点點 炼煉 "
            + "状狀 猪豬 画畫 础礎 种種 窍竅 红紅 纯純 组組 经經 绝絕 维維 罗羅 胆膽 荡蕩 脑腦 腾騰 肤膚 钢鋼 "
            + "别別 疗療 蚀蝕 调調 隐隱 飞飛 骑騎 栏欄 键鍵 长長 块塊 锭錠 条條 丝絲 减減 护護 "
            + "莲蓮 虚虛 虫蟲 蛊蠱 蛮蠻 装裝 见見 觉覺 识識 试試 诣詣 该該 请請 负負 败敗 质質 "
            + "资資 赋賦 转轉 轮輪 辅輔 输輸 边邊 达達 运運 进進 选選 遥遙 钧鈞 铁鐵 铜銅 银銀 锚錨 "
            + "闭閉 间間 阳陽 阴陰 阵陣 阶階 随隨 须須 饰飾 饱飽 饿餓 魇魘 鲛鮫 黄黃 龙龍 围圍 险險 暂暫 启啟 计計 量量 书書 册冊 说說 "
            + "龟龜 驰馳 马馬 骏駿 陆陸 劳勞 继繼 续續 结結 叠疊 饥飢 习習 备備 却卻 记記 录錄 错錯 误誤 离離 "
            + "残殘 轻輕 躯軀 类類 远遠 视視 够夠 对對 势勢 连連 产產 锁鎖 读讀 这這 张張 传傳 节節"
            + " 东東 亩畝 侧側 区區 单單 弃棄 灵靈 线線 补補 许許 还還 饲飼 驱驅"
            + " 场場 响響 垦墾 宽寬 带帶 总總 确確 约約 览覽 认認 销銷"
            + " 来來 称稱 页頁 现現 驯馴 绳繩 凑湊 从從 验驗 给給 换換 弹彈 筛篩 顺順"
            + " 浅淺 烟煙 稳穩 绛絳 绯緋 绿綠 苍蒼 蓝藍 颜顏"
            + " 拢攏 挡擋 栅柵 门門"
            + " 迁遷 规規 废廢 队隊";
    private static final String PHRASES = "一只一隻 耗尽耗盡 将尽將盡 战斗戰鬥 冲刷沖刷 横冲橫衝 轮回輪迴 回复回復 准大準大 "
            + "准宗準宗 准无準無 喂食餵食 炼制煉製 舍利舍利 游僵遊殭 漏斗漏斗 防御防禦 周围周圍 随后隨後 范围範圍 冲击衝擊 用尽用盡 放回放回 "
            + "躯干軀幹 冲刺衝刺 发出發出 游戏遊戲 获得獲得 分钟分鐘 低于低於 回到回到 回血回血 前后前後 时间后時間後 目标后目標後 "
            + "结束后結束後 松手鬆手 并反噬並反噬 发挥發揮"
            + " 只发情隻發情 只生物隻生物 标准標準 并补並補 福地里福地裡 托管託管 喂养餵養 干地乾地 田区里田區裡"
            + " 之后之後 包里包裡 藏里藏裡 每只每隻 发情發情 只剩只剩 送回送回 什么什麼 恢复恢復"
            + " 制服制服 松烟松煙 几头幾頭 只管只管 只补只補 只蛊隻蠱 捕获捕獲 丑时丑時"
            + " 规划規劃";
    private static final String UNCHANGED = "一丁七三上下不世丙中丸主乃之乙九也了二互五亡交人仙以任位何作你使俗信修倍值停"
            + "偷催元先光入全八六共具再冥冰凡出刃初利到刷力功加包化北十千升半卓卸厚及取受口"
            + "古可名君味命品四因圈土在地基境墨壁外多大天太央失奴如妙子存宇宗宙定害家容小尚展山峰已"
            + "希常年底度式形影律心必念息情意成我或所才打承投掉提摘撞收改攻放故效散料斤斥新方施族日易昨是普"
            + "晶智暗暴且抽更月有望木未本杏材束板枚果格桶棕森止正此武死每毒毛民水求江泉法波泥洲派流浮海消"
            + "火炎炸然煌熄熊爆片牛物率王玩瓣瓶甜生用甲界病痕瘟白百的直真眼知石秒秘空窗竭符第等精"
            + "紫美羽老而耗肉背腐自至色芒花苦草荒荔莽菇落蓬虎蛋蛐血行衍衰被裂解豕象赤赴起越足跳身"
            + "辣送逍通速造道遭酒酸醒重野金雪零雷需青非面音食香高鬼魁魂魄魔黑"
            + "筋骨皮原抗招退限刻匿反坐增幅查治玉移防御翡翠"
            + "副手持握配合其他部分快捷按住右永久益盾危能次搭蓄噬拳冷缺少得要目去"
            + "尾左翼耳肢腿臂角附鼻耕"
            + "概盔模唯喝奶除向前碎沿路低替吸固抵段性最比即扇扣但致程期同皆超研照祖"
            + "份割南宰找挪株狐田登福站者西逐"
            + "保免撤操植殖添熟疆管繁藏域震"
            + "服件搜索把置都各支含拴敲接字灰示剩吃什切就哪恢排序留"
            + "堂布枯沉淡深炭玄秋稻米粗素艾茶荷藕衣褐褪赭陶靛鞋麻黛"
            + "好橡牧捕"
            + "寅卯辰巳午申酉戌亥"
            + "拆";
    private static final Map<String, String> TOKENS = buildTokens();
    private static final int LONGEST = TOKENS.keySet().stream().mapToInt(String::length).max().orElse(1);

    private TraditionalGlyphs() {}

    public static String convert(String simplified) {
        StringBuilder traditional = new StringBuilder(simplified.length());
        int index = 0;
        while (index < simplified.length()) {
            int length = getMatchLength(simplified, index);
            if (length > 0) {
                traditional.append(TOKENS.get(simplified.substring(index, index + length)));
                index += length;
                continue;
            }

            char glyph = simplified.charAt(index);
            if (Character.UnicodeScript.of(glyph) == Character.UnicodeScript.HAN && UNCHANGED.indexOf(glyph) < 0) {
                throw new IllegalArgumentException("no Traditional glyph for '" + glyph + "' in \"" + simplified
                        + "\": add it to PAIRS or UNCHANGED, or to a phrase if it has more than one Traditional form");
            }
            traditional.append(glyph);
            index++;
        }
        return traditional.toString();
    }

    private static int getMatchLength(String text, int start) {
        for (int length = Math.min(LONGEST, text.length() - start); length > 0; length--) {
            if (TOKENS.containsKey(text.substring(start, start + length))) return length;
        }
        return 0;
    }

    private static Map<String, String> buildTokens() {
        Map<String, String> tokens = new HashMap<>();
        for (String token : (PAIRS + " " + PHRASES).split(" ")) {
            int half = token.length() / 2;
            if (token.length() % 2 != 0 || tokens.put(token.substring(0, half), token.substring(half)) != null) {
                throw new IllegalStateException("glyph token is odd or repeated: " + token);
            }
        }
        for (char glyph : UNCHANGED.toCharArray()) {
            if (tokens.containsKey(String.valueOf(glyph))) {
                throw new IllegalStateException("glyph is both mapped and unchanged: " + glyph);
            }
        }
        return Map.copyOf(tokens);
    }
}
