package net.alex.guzhenren.datagen.lang;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code zh_tw} 背后的简转繁字形表。
 *
 * <ul>
 *   <li>简繁大多只差字形：{@link ModLanguageProvider} 对没写繁体的条目经 {@link #convert} 推出 {@code zh_tw}。</li>
 *   <li>台湾字形（OpenCC {@code s2tw}，作 為 不作 爲）；词汇照写（{@code s2twp} 会把 信息 换词为 資訊）。</li>
 *   <li>{@link #PAIRS} 一对一字形，{@link #PHRASES} 词组，{@link #UNCHANGED} 简繁同形；token 为简体半段接繁体半段（蛊
 *       蠱、冲刷沖刷），{@link #convert} 逐位取最长匹配，词组胜过其中单字。</li>
 *   <li>⚠ 常见游戏文本中一简多繁的字（冲 沖/衝、制 制/製、只 只/隻、准 准/準、复 復/複/覆、回 回/迴、尽 盡/儘、喂 喂/
 *       餵、舍 舍/捨、游 游/遊、斗 斗/鬥、周 周/週）故意不进 {@link #PAIRS} 与 {@link #UNCHANGED}，只在词组内转换，新用
 *       法逐个按语境定；另一形古旧或游戏文本不见的（背 是 背包 不是 揹包；尸 屍；万 萬）按常用形归档。</li>
 *   <li>⚠ {@link #convert} 遇无法归位的汉字抛异常：含新字形的 {@code zh_cn} 字符串让 {@code runData} 与
 *       {@code LangContractTest} 同时失败，不把简体字发进繁体文件。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.datagen.lang.ModLanguageProvider
 * @since 1.0.0
 */

public final class TraditionalGlyphs {

    private static final String PAIRS = "万萬 与與 两兩 个個 临臨 为為 云雲 亚亞 亿億 仪儀 会會 伤傷 体體 余餘 储儲 僵殭 兰蘭 关關 养養 兽獸 内內 冻凍 击擊 纳納 "
            + "创創 剑劍 动動 华華 压壓 变變 叶葉 图圖 头頭 学學 宝寶 寿壽 将將 尸屍 层層 岁歲 巅巔 过過 载載 "
            + "师師 库庫 开開 强強 彻徹 态態 恶惡 悬懸 战戰 担擔 择擇 损損 数數 斩斬 断斷 无無 时時 旷曠 "
            + "晋晉 晓曉 机機 杀殺 枣棗 标標 梦夢 横橫 气氣 没沒 泽澤 测測 温溫 满滿 灭滅 灾災 点點 炼煉 "
            + "状狀 猪豬 画畫 础礎 种種 窍竅 红紅 纯純 组組 经經 绝絕 维維 罗羅 胆膽 脑腦 腾騰 肤膚 钢鋼 "
            + "别別 疗療 蚀蝕 调調 隐隱 飞飛 骑騎 栏欄 键鍵 长長 块塊 锭錠 条條 丝絲 减減 护護 "
            + "莲蓮 虚虛 虫蟲 蛊蠱 蛮蠻 装裝 见見 觉覺 识識 试試 诣詣 该該 请請 负負 败敗 质質 "
            + "资資 赋賦 转轉 轮輪 辅輔 输輸 边邊 达達 运運 进進 选選 遥遙 钧鈞 铁鐵 铜銅 银銀 锚錨 "
            + "闭閉 间間 阳陽 阴陰 阵陣 阶階 随隨 须須 饰飾 饱飽 饿餓 魇魘 鲛鮫 黄黃 龙龍 围圍 险險 暂暫 启啟 计計 量量 书書 册冊 说說 "
            + "龟龜 驰馳 马馬 骏駿 陆陸 劳勞 继繼 续續 结結 叠疊 饥飢 习習 备備 却卻 记記 录錄 错錯 误誤 离離";
    private static final String PHRASES = "一只一隻 耗尽耗盡 将尽將盡 战斗戰鬥 冲刷沖刷 横冲橫衝 轮回輪迴 回复回復 准大準大 "
            + "准宗準宗 准无準無 喂食餵食 炼制煉製 舍利舍利 游僵遊殭 漏斗漏斗 防御防禦 周围周圍 随后隨後 范围範圍 冲击衝擊 用尽用盡 放回放回";
    private static final String UNCHANGED = "一丁七三上下不世丙中丸主乃之乙九也了二互五亡交人仙以任位何作你使俗信修倍值停"
            + "偷催元先光入全八六共具再冥冰凡出刃初利到刷力功加包化北十千升半卓卸厚及取受口"
            + "古可名君味命品四因圈土在地基境墨壁外多大天太央失奴如妙存宇宗宙定害家容小尚展山峰已"
            + "希常年底度式形影律心必念息情意成我或所才打承投掉提摘撞收改攻放故效散料斤斥新方施族日易昨是普"
            + "晶智暗更月有望木未本杏材束板枚果格桶棕森止正此武死每毒毛民水求江泉法波泥洲派流浮海消"
            + "火炎炸然煌熄熊爆片牛物率王玩瓣瓶甜生用甲界病痕瘟白百的直真眼知石秒空窗竭符第等精"
            + "紫美羽老而耗肉背腐自至色芒花苦草荒荔莽菇落蓬虎蛋蛐血行衍衰被裂解豕象赤赴起越足跳身"
            + "辣送逍通速造道遭酒酸醒重野金雪零雷需青非面音食香高鬼魁魂魄魔黑"
            + "筋骨皮原抗招退限刻匿反坐增幅查治玉移防御翡翠"
            + "副手持握配合其他部分快捷按住右永久益盾危能次搭蓄噬拳冷缺少得要目去";
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
