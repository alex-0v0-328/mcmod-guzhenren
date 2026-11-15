package net.alex.guzhenren.datagen.lang;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 简转繁字形表：类能加载即表合法（初始化器拒落单、重复记号及同登记为映射与不变的字形）；用例钉住匹配顺序、非汉字原样通
 * 过、无法安置字形被拒。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

class TraditionalGlyphsTest {

    @Test
    @DisplayName("a phrase beats the glyphs inside it: 冲刷 is 沖刷, 横冲 is 橫衝")
    void phraseBeatsGlyph() {
        assertEquals("沖刷竅壁", TraditionalGlyphs.convert("冲刷窍壁"));
        assertEquals("四轉橫衝直撞蠱", TraditionalGlyphs.convert("四转横冲直撞蛊"));
        assertEquals("煉製失敗", TraditionalGlyphs.convert("炼制失败"));
        assertEquals("煉化中", TraditionalGlyphs.convert("炼化中"));
    }

    @Test
    @DisplayName("format codes, spaces and punctuation pass through untouched")
    void nonHanPassesThrough() {
        assertEquals("%1$s 壽元耗盡而亡", TraditionalGlyphs.convert("%1$s 寿元耗尽而亡"));
        assertEquals("外圈 蠱材  ·  內圈 蠱蟲", TraditionalGlyphs.convert("外圈 蛊材  ·  内圈 蛊虫"));
        assertEquals("", TraditionalGlyphs.convert(""));
    }

    @Test
    @DisplayName("glyphs both scripts share stay as they are")
    void sharedGlyphsStay() {
        assertEquals("斤", TraditionalGlyphs.convert("斤"));
        assertEquals("背包", TraditionalGlyphs.convert("背包"));
    }

    @Test
    @DisplayName("a glyph with several Traditional forms converts only inside a phrase")
    void ambiguousGlyphNeedsPhrase() {
        assertThrows(IllegalArgumentException.class, () -> TraditionalGlyphs.convert("冲"));
        assertThrows(IllegalArgumentException.class, () -> TraditionalGlyphs.convert("制度"));
    }

    @Test
    @DisplayName("a glyph the table has never seen fails instead of shipping Simplified")
    void unknownGlyphFails() {
        assertThrows(IllegalArgumentException.class, () -> TraditionalGlyphs.convert("发"));
    }
}
