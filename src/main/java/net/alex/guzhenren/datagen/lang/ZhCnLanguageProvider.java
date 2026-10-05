package net.alex.guzhenren.datagen.lang;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.gameplay.aperture.ApertureStatus;
import net.alex.guzhenren.gameplay.aperture.EssenceColor;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.aperture.Title;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.gameplay.body.Race;
import net.alex.guzhenren.gameplay.mind.Brilliance;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import net.alex.guzhenren.gameplay.mind.WisdomType;
import net.alex.guzhenren.gameplay.path.GuAttainment;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.MarkTag;
import net.alex.guzhenren.gameplay.path.qi.QiKind;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.soul.SoulTier;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * The Chinese strings, written as an aligned table beside the English one.
 *
 * <p>Extends {@link net.neoforged.neoforge.common.data.LanguageProvider} for {@code zh_cn}. Every
 * entry takes the registered object or the enum constant, never a raw key string. The value column
 * aligns per {@code add*()} method to that method's longest key.
 *
 * <p>⚠ These renderings are the authority, not a translation of the English. Deriving either side
 * from the other is how a name quietly comes to mean something it never meant.
 *
 * <p>In {@link #addItemKeys}, the Spirit Spring [元泉] block uses {@code addBlock}: one key name
 * simultaneously covers the block, its block item, and its fluid type (they share one description
 * id).
 *
 * <p>{@link net.alex.guzhenren.datagen.lang.ZhTwLanguageProvider} extends this table through the
 * protected locale constructor and converts every value on its way in, so a string written here
 * reaches {@code zh_tw} with no second line.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.datagen.lang.EnUsLanguageProvider
 * @since 1.0.0
 */

public class ZhCnLanguageProvider extends LanguageProvider {

    public ZhCnLanguageProvider(PackOutput output) {
        this(output, "zh_cn");
    }

    protected ZhCnLanguageProvider(PackOutput output, String locale) {
        super(output, Guzhenren.MOD_ID, locale);
    }

    @Override
    protected void addTranslations() {
        addEnumKeys();
        addDisplayKeys();
        addDimensionKeys();
        addCommandKeys();
        addScreenKeys();
        addItemKeys();
        addEntityKeys();
        addAdvancementKeys();
        addDeathMessages();
    }

    private void add(NamedEnum key, String value) { add(key.getTranslationKey(), value); }

    //region DISPLAY
    private void addDisplayKeys() {
        add("guzhenren.display.realm", "%s%s");
        add("guzhenren.display.realm_title", "%s%s");
        add("guzhenren.display.aptitude_line", "%s [%s]");
        add("guzhenren.display.gu_line", "%s%s%s");
        add("guzhenren.display.gu", "蛊虫");
        add("guzhenren.display.aperture_1", "第一空窍");
        add("guzhenren.display.aperture_2", "第二空窍");
        add("guzhenren.display.gu_material", "蛊材");
        add("guzhenren.display.physique", "[%s]");
        add("guzhenren.display.physique_line", "体质：%s");
        add("guzhenren.display.lifespan", "%s [%s岁]");
        add("guzhenren.display.base_fraction", "%s成%s");
        add("guzhenren.display.base_round", "%s成");
        add("guzhenren.display.base_full", "十成");
        add("guzhenren.display.base_tens.2", "二");
        add("guzhenren.display.base_tens.3", "三");
        add("guzhenren.display.base_tens.4", "四");
        add("guzhenren.display.base_tens.5", "五");
        add("guzhenren.display.base_tens.6", "六");
        add("guzhenren.display.base_tens.7", "七");
        add("guzhenren.display.base_tens.8", "八");
        add("guzhenren.display.base_tens.9", "九");
        add("guzhenren.display.base_units.1", "一");
        add("guzhenren.display.base_units.2", "二");
        add("guzhenren.display.base_units.3", "三");
        add("guzhenren.display.base_units.4", "四");
        add("guzhenren.display.base_units.5", "五");
        add("guzhenren.display.base_units.6", "六");
        add("guzhenren.display.base_units.7", "七");
        add("guzhenren.display.base_units.8", "八");
        add("guzhenren.display.base_units.9", "九");
        add("guzhenren.display.none", "[无]");
        add("guzhenren.display.time_rate_up", "%s倍");
        add("guzhenren.display.wild", "野生·%s");
        add("guzhenren.display.vital", "本命·%s");
        add("guzhenren.display.strength.beast_reading", "[%s%s之力]");
        add("guzhenren.display.strength.beast_number.1", "一");
        add("guzhenren.display.strength.beast_number.2", "两");
        add("guzhenren.display.strength.beast_number.10", "十");
        add("guzhenren.display.strength.beast_number.100", "百");
        add("guzhenren.display.strength.beast_number.1000", "千");
        add("guzhenren.display.strength.beast_number.10000", "万");
        add("guzhenren.display.strength.num_join", "%s%s");
        add("guzhenren.display.strength.num_join_zero", "%s零%s");
        add("guzhenren.display.strength.num_hundreds.1", "一百");
        add("guzhenren.display.strength.num_hundreds.2", "二百");
        add("guzhenren.display.strength.num_hundreds.3", "三百");
        add("guzhenren.display.strength.num_tens_led.1", "一十");
        add("guzhenren.display.strength.num_tens.1", "十");
        add("guzhenren.display.strength.num_tens.2", "二十");
        add("guzhenren.display.strength.num_tens.3", "三十");
        add("guzhenren.display.strength.num_tens.4", "四十");
        add("guzhenren.display.strength.num_tens.5", "五十");
        add("guzhenren.display.strength.num_tens.6", "六十");
        add("guzhenren.display.strength.num_tens.7", "七十");
        add("guzhenren.display.strength.num_tens.8", "八十");
        add("guzhenren.display.strength.num_tens.9", "九十");
        add("guzhenren.display.strength.num_units.1", "一");
        add("guzhenren.display.strength.num_units.2", "二");
        add("guzhenren.display.strength.num_units.3", "三");
        add("guzhenren.display.strength.num_units.4", "四");
        add("guzhenren.display.strength.num_units.5", "五");
        add("guzhenren.display.strength.num_units.6", "六");
        add("guzhenren.display.strength.num_units.7", "七");
        add("guzhenren.display.strength.num_units.8", "八");
        add("guzhenren.display.strength.num_units.9", "九");
        add("guzhenren.display.strength.jin_total", "[%s斤]");
        add("guzhenren.display.strength.jin_reading", "[%s斤之力]");
        add("guzhenren.display.strength.jun_reading", "[%s钧之力]");

        add("guzhenren.hud.lifespan", "寿元 %s");
        add("guzhenren.display.tenth.2", "二成");
        add("guzhenren.display.tenth.3", "三成");
        add("guzhenren.display.tenth.4", "四成");
        add("guzhenren.display.tenth.5", "五成");
        add("guzhenren.display.tenth.6", "六成");
        add("guzhenren.display.tenth.7", "七成");
        add("guzhenren.display.tenth.8", "八成");
        add("guzhenren.display.tenth.9", "九成");
        add("guzhenren.display.tenth.10", "十成十");

        add("guzhenren.hud.refining", "炼化 %s/%s");
        add("guzhenren.hud.using", "使用 %s/%s");
        add("guzhenren.hud.refining_plain", "炼化中");
        add("guzhenren.hud.using_plain", "使用中");
        add("guzhenren.hud.nourishing", "温养 %s%%");
        add("guzhenren.hud.nourish_starving", "真元将尽 %s%%");
        add("guzhenren.hud.aperture_pressure", "空窍压力 %s%%");
        add("guzhenren.hud.aperture_pressure_cd", "空窍压力 %s%% [%s]");
    }
    //endregion

    //region DIMENSION
    private void addDimensionKeys() {
        add("guzhenren.dimension.treasure_yellow_heaven.inventory_full", "背包已满，放不下的物品悬浮在身边");
    }
    //endregion

    //region COMMAND
    private void addCommandKeys() {
        add("guzhenren.command.header", "[GZR]");
        add("guzhenren.command.tagged", "[GZR] %s");
        add("guzhenren.command.updated", "已更新 %s 名玩家");
        add("guzhenren.command.unknown_value", "未知的取值：%s");

        add("guzhenren.command.failed.awakened", "%s 已开窍");
        add("guzhenren.command.failed.unawakened", "%s 尚未开窍");
        add("guzhenren.command.failed.aperture_index", "%s 没有该空窍");
        add("guzhenren.command.failed.extreme_physique_required", "%s 必须使用具体的十绝体质");

        add("guzhenren.command.info.realm", "玩家修为  %s");
        add("guzhenren.command.info.aperture_status", "空窍状态  %s");
        add("guzhenren.command.info.talent", "玩家天赋  %s");
        add("guzhenren.command.info.essence", "玩家真元  %s / %s");
        add("guzhenren.command.info.distilled", "精炼真元  %s / %s");
        add("guzhenren.command.info.pressure", "空窍压力  %s / %s%%");
        add("guzhenren.command.info.primary_path", "主修流派  %s");
        add("guzhenren.command.info.secondary_path", "辅修流派  %s");
        add("guzhenren.command.info.soul", "玩家魂魄  %s / %s");
        add("guzhenren.command.info.lifespan", "玩家寿元  %s");
        add("guzhenren.command.info.physique", "%s");
        add("guzhenren.command.info.race", "种族      %s");
        add("guzhenren.command.info.wisdom_path_achieve", "智道造诣");
        add("guzhenren.command.info.wisdom_path_achieve_entry", "  %s  %s");
        add("guzhenren.command.info.qi_path_achieve", "气道造诣");
        add("guzhenren.command.info.qi_path_achieve_entry", "  %s  %s");
        add("guzhenren.command.info.paths", "流派造诣");
        add("guzhenren.command.info.strength_path_achieve", "力道造诣");
        add("guzhenren.command.info.strength_path_achieve_entry", "  %s  %s");
        add("guzhenren.command.info.time_path_achieve", "宙道造诣");
        add("guzhenren.command.info.time_rate_up_entry", "  自身时间加速  %s");
        add("guzhenren.command.info.capacity", "肉身承受  %s / %s斤");
        add("guzhenren.command.info.attack", "基础攻击力  %s");
        add("guzhenren.command.info.brilliance", "才情  %s");
        add("guzhenren.command.info.brilliance_rate", "每秒%s个念头");
        add("guzhenren.command.info.mind", "脑海");
        add("guzhenren.command.info.mind_entry", "  %s  %s / %s");

        add("guzhenren.command.info.detail", " [%s]");

        add("guzhenren.command.travel.entered", "已将 %s 送入%s");
        add("guzhenren.command.travel.exited", "已将 %s 送出锚定维度");
        add("guzhenren.command.travel.already_inside", "%s 已经在%s内");
        add("guzhenren.command.travel.not_inside", "%s 不在任何锚定维度内");
        add("guzhenren.command.travel.unknown_dimension", "%s 不是锚定维度");
    }
    //endregion

    //region SCREEN
    private void addScreenKeys() {
        add("key.categories.guzhenren", "蛊真人");
        add("key.guzhenren.open_info", "打开信息面板");

        add("guzhenren.screen.info.title", "信息");
        add("guzhenren.screen.tab.aperture", "空窍");
        add("guzhenren.screen.tab.body", "肉身");
        add("guzhenren.screen.tab.soul", "魂魄");
        add("guzhenren.screen.tab.path", "流派造诣");
        add("guzhenren.screen.tab.mind", "脑海");
        add("guzhenren.screen.tab.refinement", "炼蛊");
        add("guzhenren.menu.aperture_storage", "空窍存储");
        add("guzhenren.menu.vital", "本命");
        add("guzhenren.menu.refinement", "炼蛊");
        add("guzhenren.menu.soul_trade.trade", "交易");
        add("guzhenren.menu.soul_trade.short", "材料不足，无法交易");
        add("guzhenren.menu.soul_trade.no_room", "背包空间不足，请先腾出空间再交易");
        add("guzhenren.menu.refinement.essence", "需真元%s炼制");
        add("guzhenren.menu.refinement.craft", "炼制");
        add("guzhenren.menu.refinement.no_room", "输出无空位");
        add("guzhenren.menu.refinement.not_awakened", "未开窍，无法炼蛊");
        add("guzhenren.menu.refinement.rings", "外圈 蛊材  ·  内圈 蛊虫");
        add("guzhenren.menu.refinement.window", "第 %s/%s 窗口  ·  余 %s 秒  ·  元石 %s/%s");
        add("guzhenren.menu.refinement.gap", "炼制中");
        add("guzhenren.menu.refinement.lost_stones", "元石不足，炼制失败");
        add("guzhenren.menu.refinement.lost_essence", "真元耗尽，炼制失败");
        add("guzhenren.menu.refinement.lost_roll", "炼制失败");
        add("guzhenren.menu.refinement.elder_spent", "元老蛊已耗尽，已卸下");
        add("guzhenren.menu.refinement.recipes", "蛊方");
        add("guzhenren.menu.refinement.selected", "已选  %s");
        add("guzhenren.menu.refinement.extra", "格内有多余材料");
        add("guzhenren.menu.refinement.pick.title", "选择蛊方");
        add("guzhenren.menu.refinement.pick.auto", "自动选择");
        add("guzhenren.menu.refinement.pick.empty", "尚不知晓任何蛊方");
        add("guzhenren.menu.refinement.pick.needs", "所需");
        add("guzhenren.menu.refinement.pick.item", "  %s × %s");
        add("guzhenren.menu.refinement.pick.windows", "%s 个窗口  ·  共 %s 秒");
        add("guzhenren.menu.refinement.pick.stones", "每窗元石  %s");
        add("guzhenren.menu.refinement.pick.cost", "每秒真元 %s");
        add("guzhenren.menu.refinement.pick.chance", "%s%%");
        add("guzhenren.menu.refinement.pick.success", "成功率  %s%%");
        add("guzhenren.menu.refinement.stop", "停止");
        add("guzhenren.menu.refinement.stopped", "你中断了炼制");
        add("guzhenren.menu.refinement.pool", "%s / %s");
        add("guzhenren.menu.refinement.pick.soul", "每秒魂魄 %s");
        add("guzhenren.screen.label.primary_path", "主修流派");
        add("guzhenren.screen.label.secondary_path", "辅修流派");
        add("guzhenren.screen.pick.title", "选择辅修流派");
        add("guzhenren.screen.pick.hint", "点击选择");
        add("guzhenren.screen.nourish", "温养空窍");
        add("guzhenren.screen.nourish_stop", "停止温养");
        add("guzhenren.screen.nourish_second", "温养第二空窍");
        add("guzhenren.screen.impact", "冲刷窍壁");
        add("guzhenren.screen.button.storage", "空窍存储");

        add("guzhenren.nourish.starved", "真元见底，中止温养");
        add("guzhenren.nourish.stage_up", "小境界提升");
        add("guzhenren.impact.poor", "冲刷窍壁需真元 %s");
        add("guzhenren.impact.success", "大境界提升");
        add("guzhenren.impact.hold", "冲刷失败，大境界未变");
        add("guzhenren.impact.drop_stage", "冲刷失败，小境界掉落");
        add("guzhenren.impact.drop_base", "冲刷失败，资质受损");
        add("guzhenren.screen.label.realm", "修为");
        add("guzhenren.screen.label.aperture_status", "空窍状态");
        add("guzhenren.screen.label.talent", "天赋");
        add("guzhenren.screen.label.essence", "真元");
        add("guzhenren.screen.label.distilled", "精炼真元");
        add("guzhenren.screen.label.physique", "体质");
        add("guzhenren.screen.label.race", "种族");
        add("guzhenren.screen.label.wisdom_path_achieve", "智道造诣");
        add("guzhenren.screen.label.soul", "魂魄");
        add("guzhenren.screen.label.lifespan", "寿元");
        add("guzhenren.screen.label.qi_path_achieve", "气道造诣");
        add("guzhenren.screen.label.paths", "流派造诣");
        add("guzhenren.screen.label.strength_path_achieve", "力道造诣");
        add("guzhenren.screen.label.time_path_achieve", "宙道造诣");
        add("guzhenren.screen.label.time_rate_up", "自身时间加速");
        add("guzhenren.screen.label.body_capacity", "承受");
        add("guzhenren.screen.label.attack", "基础攻击力");
        add("guzhenren.screen.label.aperture_pressure", "空窍压力");
        add("guzhenren.screen.capacity", "%s / %s斤");
        add("guzhenren.menu.aperture_load", "空窍负担 %s / %s");
        add("guzhenren.screen.label.brilliance", "才情");
        add("guzhenren.display.path_marks", " 道痕%s");
    }
    //endregion

    //region ITEM
    private void addItemKeys() {
        addItem(ModItems.HOPE_GU, "希望蛊");
        addItem(ModItems.COPPER_RELICS_GU, "青铜舍利蛊");
        addItem(ModItems.STEEL_RELICS_GU, "赤铁舍利蛊");
        addItem(ModItems.SILVER_RELICS_GU, "白银舍利蛊");
        addItem(ModItems.GOLD_RELICS_GU, "黄金舍利蛊");
        addItem(ModItems.CRYSTAL_RELICS_GU, "紫晶舍利蛊");
        addItem(ModItems.WHITE_BOAR_GU, "白豕蛊");
        addItem(ModItems.BLACK_BOAR_GU, "黑豕蛊");
        addItem(ModItems.FLOWER_BOAR_GU, "花豕蛊");
        addItem(ModItems.BEAR_STRENGTH_GU, "熊力蛊");
        addItem(ModItems.DRAGONPILL_CRICKET_GU, "龙丸蛐蛐蛊");
        addItem(ModItems.BRUTE_FORCE_LONGHORN_BEETLE_GU, "蛮力天牛蛊");
        addItem(ModItems.HORIZONTAL_CRASH_GU, "横冲蛊");
        addItem(ModItems.VERTICAL_CRASH_GU, "直撞蛊");
        addItem(ModItems.CHARGING_CRASH_GU_4, "四转横冲直撞蛊");
        addItem(ModItems.CHARGING_CRASH_GU_5, "五转横冲直撞蛊");
        addItem(ModItems.SELF_RELIANCE_GU_2, "二转自力更生蛊");
        addItem(ModItems.SELF_RELIANCE_GU_3, "三转自力更生蛊");
        addItem(ModItems.SELF_RELIANCE_GU_4, "四转自力更生蛊");
        addItem(ModItems.HARDSHIP_STRENGTH_GU, "苦力蛊");
        addItem(ModItems.ALL_OUT_EFFORT_GU_3, "三转全力以赴蛊");
        addItem(ModItems.ALL_OUT_EFFORT_GU_4, "四转全力以赴蛊");
        addItem(ModItems.ALL_OUT_EFFORT_GU_5, "五转全力以赴蛊");
        addItem(ModItems.JIN_STRENGTH_GU, "斤力蛊");
        addItem(ModItems.TENS_JIN_STRENGTH_GU, "十斤力蛊");
        addItem(ModItems.JUN_STRENGTH_GU, "钧力蛊");
        addItem(ModItems.TENS_JUN_STRENGTH_GU, "十钧力蛊");
        addItem(ModItems.VITALITY_LEAF_GU, "生机叶蛊");
        addItem(ModItems.LIFESPAN_GU, "寿蛊");
        addItem(ModItems.TENS_LIFESPAN_GU, "十年寿蛊");
        addItem(ModItems.HUNDREDS_LIFESPAN_GU, "百年寿蛊");
        addItem(ModItems.THOUSANDS_LIFESPAN_GU, "千年寿蛊");
        addItem(ModItems.LIQUOR_WORM, "酒虫");
        addItem(ModItems.FOUR_FLAVORS_LIQUOR_WORM, "四味酒虫");
        addItem(ModItems.SEVEN_FRAGRANCES_LIQUOR_WORM, "七香酒虫");
        addItem(ModItems.NINE_EYES_LIQUOR_WORM, "九眼酒虫");
        addItem(ModItems.ROAMING_ZOMBIE_GU, "游僵蛊");
        addItem(ModItems.HAIRY_ZOMBIE_GU, "毛僵蛊");
        addItem(ModItems.HOPPING_ZOMBIE_GU, "跳僵蛊");
        addItem(ModItems.HEAVENLY_DEMON_ZOMBIE_GU, "天魔尸蛊");
        addItem(ModItems.NIGHTMARE_ZOMBIE_GU, "梦魇尸蛊");
        addItem(ModItems.ASURA_ZOMBIE_GU, "修罗尸蛊");
        addItem(ModItems.EARTH_CHIEF_ZOMBIE_GU, "地魁尸蛊");
        addItem(ModItems.PLAGUE_ZOMBIE_GU, "病瘟尸蛊");
        addItem(ModItems.BLOOD_WIGHT_GU, "血鬼尸蛊");
        addItem(ModItems.PRIMEVAL_ELDER_GU_1, "一转元老蛊");
        addItem(ModItems.PRIMEVAL_ELDER_GU_2, "二转元老蛊");
        addItem(ModItems.PRIMEVAL_ELDER_GU_3, "三转元老蛊");
        addItem(ModItems.PRIMEVAL_ELDER_GU_4, "四转元老蛊");
        addItem(ModItems.PRIMEVAL_ELDER_GU_5, "五转元老蛊");
        addItem(ModItems.HEAVENLY_ESSENCE_TREASURE_LOTUS_GU, "天元宝莲");
        addItem(ModItems.HEAVENLY_ESSENCE_TREASURE_MONARCH_LOTUS_GU, "天元宝君莲");
        addItem(ModItems.HEAVENLY_ESSENCE_TREASURE_KING_LOTUS_GU, "天元宝王莲");
        addItem(ModItems.SECOND_WATCH_GU, "两更蛊");
        addItem(ModItems.THIRD_WATCH_GU, "三更蛊");
        addItem(ModItems.MALICIOUS_THOUGHT_GU_2, "二转恶念蛊");
        addItem(ModItems.MALICIOUS_THOUGHT_GU_3, "三转恶念蛊");
        addItem(ModItems.MALICIOUS_THOUGHT_GU_4, "四转恶念蛊");
        addItem(ModItems.MALICIOUS_THOUGHT_GU_5, "五转恶念蛊");
        addItem(ModItems.GUTS_GU, "一转胆识蛊");
        addItem(ModItems.CASUAL_GU_1, "一转随意蛊");
        addItem(ModItems.CASUAL_GU_2, "二转随意蛊");
        addItem(ModItems.STONE_APERTURE_GU_3, "三转石窍蛊");
        addItem(ModItems.STONE_APERTURE_GU_4, "四转石窍蛊");
        addItem(ModItems.STONE_APERTURE_GU_5, "五转石窍蛊");
        addItem(ModItems.SECOND_APERTURE_GU_1, "一转第二空窍蛊");
        addItem(ModItems.SECOND_APERTURE_GU_2, "二转第二空窍蛊");
        addItem(ModItems.SECOND_APERTURE_GU_3, "三转第二空窍蛊");
        addItem(ModItems.SECOND_APERTURE_GU_4, "四转第二空窍蛊");
        addItem(ModItems.SECOND_APERTURE_GU_5, "五转第二空窍蛊");
        addItem(ModItems.PRIMEVAL_STONE, "元石");
        addItem(ModItems.HUMAN_APERTURE_1, "一转人窍");
        addItem(ModItems.HUMAN_APERTURE_2, "二转人窍");
        addItem(ModItems.HUMAN_APERTURE_3, "三转人窍");
        addItem(ModItems.HUMAN_APERTURE_4, "四转人窍");
        addItem(ModItems.HUMAN_APERTURE_5, "五转人窍");
        addItem(ModItems.LIQUOR, "酒");
        addItem(ModItems.SOUR_LIQUOR, "酸酒");
        addItem(ModItems.SWEET_LIQUOR, "甜酒");
        addItem(ModItems.BITTER_LIQUOR, "苦酒");
        addItem(ModItems.SPICY_LIQUOR, "辣酒");
        addBlock(ModBlocks.SPIRIT_SPRING, "元泉");

        addItem(ModItems.SWORD_QI_1, "一转剑气");
        addItem(ModItems.SWORD_QI_2, "二转剑气");
        addItem(ModItems.SWORD_QI_3, "三转剑气");
        addItem(ModItems.SWORD_QI_4, "四转剑气");
        addItem(ModItems.SWORD_QI_5, "五转剑气");
        addItem(ModItems.STRENGTH_QI_1, "一转力气");
        addItem(ModItems.STRENGTH_QI_2, "二转力气");
        addItem(ModItems.STRENGTH_QI_3, "三转力气");
        addItem(ModItems.STRENGTH_QI_4, "四转力气");
        addItem(ModItems.STRENGTH_QI_5, "五转力气");
        addItem(ModItems.LIFE_QI_1, "一转生气");
        addItem(ModItems.LIFE_QI_2, "二转生气");
        addItem(ModItems.LIFE_QI_3, "三转生气");
        addItem(ModItems.LIFE_QI_4, "四转生气");
        addItem(ModItems.LIFE_QI_5, "五转生气");
        addItem(ModItems.ESSENCE_QI_1, "一转元气");
        addItem(ModItems.ESSENCE_QI_2, "二转元气");
        addItem(ModItems.ESSENCE_QI_3, "三转元气");
        addItem(ModItems.ESSENCE_QI_4, "四转元气");
        addItem(ModItems.ESSENCE_QI_5, "五转元气");
        addItem(ModItems.DEATH_QI_5, "五转死气");

        add("itemGroup.guzhenren.mortal_gu", "凡蛊");
        add("itemGroup.guzhenren.gu_material", "蛊材");
        add("itemGroup.guzhenren.strength_mortal_gu", "力道凡蛊");

        add("guzhenren.item.failed.awakened", "你已开窍");
        add("guzhenren.item.failed.unawakened", "你未开窍");
        add("guzhenren.item.failed.essence_full", "真元已满");
        add("guzhenren.item.failed.rank_mismatch", "境界不符 — 此蛊需%s");
        add("guzhenren.item.failed.stage_peak", "已至小境界巅峰");
        add("guzhenren.item.failed.second_aperture_rank", "第二空窍已是该转数或更高");
        add("guzhenren.item.failed.aperture_unavailable", "没有可以作用的空窍");
        add("guzhenren.item.failed.beast_strength_held", "已有%s之力");
        add("guzhenren.item.failed.human_strength_full", "此力已满%s层");
        add("guzhenren.item.failed.vitality_active", "生机叶效果未散");
        add("guzhenren.item.failed.refine_essence", "真元不足无法炼化");
        add("guzhenren.item.failed.essence", "真元不足");
        add("guzhenren.item.failed.liquor_rank", "%s蛊师才可使用");
        add("guzhenren.item.failed.liquor_distilling", "精炼未止");
        add("guzhenren.item.failed.elder_gu_empty", "蛊中无元石");
        add("guzhenren.item.failed.elder_gu_full", "蛊中元石已满");
        add("guzhenren.item.failed.elder_gu_no_stones", "身上无元石可存");
        add("guzhenren.item.failed.gu_cooldown", "此蛊尚需 %s 秒");
        add("guzhenren.item.failed.all_out_active", "全力以赴蛊效果未散");
        add("guzhenren.item.failed.zombie_already", "彻底转变为了僵尸");
        add("guzhenren.item.failed.gu_starving", "蛊已太饿，需先喂食");
        add("guzhenren.item.failed.no_use", "此蛊无需使用");

        add("guzhenren.item.gu.invested", "已投入 %s/%s");
        add("guzhenren.item.gu.refine_progress", "炼化 %s/%s");
        add("guzhenren.item.gu.refine_cost", "炼化 %s");
        add("guzhenren.item.gu.hunger_progress", "饱食 %s/%s");
        add("guzhenren.item.gu.stored_stones", "存石 %s/%s");
        add("guzhenren.item.gu.lifespan_gained", "寿元 +%s 年");
        add("guzhenren.item.gu.hungry", "%s饿了");
        add("guzhenren.item.gu.starved", "%s饿死了");
        add("guzhenren.item.gu.exhausted", "强行催动%s，蛊已死亡");
        add("guzhenren.item.gu.ruined", "%s在炼制中失败");
        add("guzhenren.item.gu.health", "生命 %s / %s");
        add("guzhenren.item.gu.vital_lost", "本命%s死亡，你也遭受重创");
        add("guzhenren.item.death_qi_cured", "死气化解 — 回复 %s 年寿元");

        add("effect.guzhenren.vitality_leaf", "生机叶蛊效果");
        add("effect.guzhenren.liquor_worm", "酒虫效果");
        add("effect.guzhenren.life_qi", "生气效果");
        add("effect.guzhenren.essence_qi", "元气效果");
        add("effect.guzhenren.death_qi", "死气效果");
        add("effect.guzhenren.strength_qi", "力气效果");
        add("effect.guzhenren.flower_boar_gu", "花豕蛊效果");
        add("effect.guzhenren.dragonpill_cricket_gu", "龙丸蛐蛐蛊效果");
        add("effect.guzhenren.brute_force_longhorn_beetle_gu", "蛮力天牛蛊效果");
        add("effect.guzhenren.horizontal_crash_gu", "横冲蛊效果");
        add("effect.guzhenren.vertical_crash_gu", "直撞蛊效果");
        add("effect.guzhenren.charging_crash_gu", "横冲直撞蛊效果");
        add("effect.guzhenren.self_reliance_gu", "自力更生蛊效果");
        add("effect.guzhenren.hardship_strength_gu", "苦力蛊效果");
        add("effect.guzhenren.all_out_effort", "全力以赴蛊效果");
        add("effect.guzhenren.half_zombie", "半生半僵效果");
        add("effect.guzhenren.second_watch_gu", "两更蛊效果");
        add("effect.guzhenren.third_watch_gu", "三更蛊效果");
        add("effect.guzhenren.malicious_thought_gu", "恶念蛊效果");
        add("effect.guzhenren.casual_gu", "随意蛊效果");
    }
    //endregion

    //region ENTITY
    private void addEntityKeys() {
        add("entity.guzhenren.hope_gu_entity", "希望蛊");
        add("entity.guzhenren.white_boar_gu_entity", "白豕蛊");
        add("entity.guzhenren.black_boar_gu_entity", "黑豕蛊");
        add("entity.guzhenren.flower_boar_gu_entity", "花豕蛊");
        add("entity.guzhenren.wild_boar", "野猪");
        add("entity.guzhenren.brown_bear", "棕熊");
        add("entity.guzhenren.asian_black_bear", "亚洲黑熊");
        add("entity.guzhenren.american_black_bear", "美洲黑熊");
        add("entity.guzhenren.albino_bear", "白化熊");
        add("entity.guzhenren.tiger", "老虎");
        add("entity.guzhenren.white_tiger", "白虎");
        add("entity.guzhenren.horizontal_crash_gu_entity", "横冲蛊");
        add("entity.guzhenren.vertical_crash_gu_entity", "直撞蛊");
        add("entity.guzhenren.charging_crash_gu_4_entity", "四转横冲直撞蛊");
        add("entity.guzhenren.charging_crash_gu_5_entity", "五转横冲直撞蛊");
        add("entity.guzhenren.test_trade_gu_immortal", "测试交易蛊仙");
    }
    //endregion

    //region ADVANCEMENT
    private void addAdvancementKeys() {
        add("advancements.guzhenren.first_awakening.title", "开窍！");
        add("advancements.guzhenren.first_awakening.description", "使用你的第一只希望蛊，开出第一窍");
    }
    //endregion

    //region DEATH
    private void addDeathMessages() {
        add("death.attack.guzhenren.lifespan_exhausted", "%1$s 寿元耗尽而亡");
        add("death.attack.guzhenren.soul_collapse", "%1$s 魂魄衰竭而亡");
        add("death.attack.guzhenren.mind_ocean_shattered", "%1$s 脑海炸裂而亡");
        add("death.attack.guzhenren.aperture_pressure_explosion", "%1$s 空窍压力爆炸而亡");
        add("death.attack.guzhenren.ten_extreme_disaster", "%1$s 被十绝天灾波及而亡");
        add("death.attack.guzhenren.vital_gu_lost", "%1$s 因本命蛊死亡而亡");
    }
    //endregion

    //region ENUM
    private void addEnumKeys() {
        addTitle();
        addRank();
        addStage();
        addApertureStatus();
        addTalent();
        addEssenceColor();
        addTenExtreme();
        addPhysique();
        addRace();
        addSoulTier();
        addMarkTag();
        addQiKind();
        addPath();
        addAttainment();
        addWisdomType();
        addBrilliance();
        addThoughtTag();
        addBeastStrength();
        addBeastStrengthFamily();
        addStrengthPathBranch();
        addHumanStrength();
    }

    private void addBeastStrength() {
        add(BeastStrength.WHITE_BOAR, "白豕");
        add(BeastStrength.BLACK_BOAR, "黑豕");
        add(BeastStrength.BEAR, "熊");
    }

    private void addStrengthPathBranch() {
        add(StrengthPathBranch.BEAST_STRENGTH_PHANTOM, "兽力虚影流");
        add(StrengthPathBranch.HUMAN_JUN_STRENGTH, "人力钧力流");
        add(StrengthPathBranch.ATMOSPHERIC_HEAVEN_AND_EARTH, "气象天地流");
        add(StrengthPathBranch.NORMAL, "基础力道");
    }

    private void addHumanStrength() {
        add(HumanStrength.JIN, "斤");
        add(HumanStrength.TEN_JIN, "十斤");
        add(HumanStrength.JUN, "钧");
        add(HumanStrength.TEN_JUN, "十钧");
    }

    private void addTitle() {
        add(Title.MORTAL, "凡人");
        add(Title.GU_MASTER, "蛊师");
        add(Title.GU_IMMORTAL, "蛊仙");
    }

    private void addRank() {
        add(Rank.NONE, "");
        add(Rank.ONE, "一转");
        add(Rank.TWO, "二转");
        add(Rank.THREE, "三转");
        add(Rank.FOUR, "四转");
        add(Rank.FIVE, "五转");
        add(Rank.SIX, "六转");
        add(Rank.SEVEN, "七转");
        add(Rank.EIGHT, "八转");
        add(Rank.NINE, "九转");
    }

    private void addStage() {
        add(Stage.NONE, "");
        add(Stage.INIT, "初阶");
        add(Stage.MIDDLE, "中阶");
        add(Stage.UPPER, "高阶");
        add(Stage.PEAK, "巅峰");
    }

    private void addApertureStatus() {
        add(ApertureStatus.NORMAL, "正常");
        add(ApertureStatus.DEAD, "死窍");
    }

    private void addTalent() {
        add(Talent.EXTREME, "十绝天资");
        add(Talent.FIRST, "甲等资质");
        add(Talent.SECOND, "乙等资质");
        add(Talent.THIRD, "丙等资质");
        add(Talent.FOURTH, "丁等资质");
        add(Talent.NONE, "未觉醒");
    }

    private void addPhysique() {
        add(Physique.ZOMBIE, "僵");
        add(Physique.HALF_ZOMBIE, "半僵");
        add(Physique.EXTREME, "十绝体");
    }

    private void addRace() {
        add(Race.HUMAN, "人族");
        add(Race.HAIRY_MEN, "毛民");
        add(Race.EGGMEN, "蛋人");
        add(Race.ROCKMEN, "石人");
        add(Race.FEATHERMEN, "羽民");
        add(Race.INKMEN, "墨人");
        add(Race.MINIMEN, "小人");
        add(Race.MERMEN, "鲛人");
        add(Race.BEASTMEN, "兽人");
        add(Race.DRAGONMEN, "龙人");
        add(Race.MUSHROOMMEN, "菇人");
        add(Race.SNOWMEN, "雪人");
    }

    private void addEssenceColor() {
        add(EssenceColor.NONE, "无");
        add(EssenceColor.GREEN_COPPER, "青铜色");
        add(EssenceColor.RED_STEEL, "赤铁色");
        add(EssenceColor.WHITE_SILVER, "白银色");
        add(EssenceColor.YELLOW_GOLDEN, "黄金色");
        add(EssenceColor.PURPLE_CRYSTAL, "紫晶色");
        add(EssenceColor.GREEN_GRAPE, "青提");
        add(EssenceColor.RED_DATE, "红枣");
        add(EssenceColor.WHITE_LITCHI, "白荔");
        add(EssenceColor.YELLOW_APRICOT, "黄杏");
    }

    private void addTenExtreme() {
        add(ExtremePhysique.NONE, "");
        add(ExtremePhysique.VERDANT_GREAT_SUN, "太日阳莽体");
        add(ExtremePhysique.DESOLATE_ANCIENT_MOON, "古月阴荒体");
        add(ExtremePhysique.NORTHERN_DARK_ICE_SOUL, "北冥冰魄体");
        add(ExtremePhysique.BOUNDLESS_FOREST_SAMSARA, "森海轮回体");
        add(ExtremePhysique.BLAZING_GLORY_LIGHTNING_BRILLIANCE, "炎煌雷泽体");
        add(ExtremePhysique.MYRIAD_GOLD_WONDROUS_ESSENCE, "万金妙华体");
        add(ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL, "大力真武体");
        add(ExtremePhysique.CAREFREE_WISDOM_HEART, "逍遥智心体");
        add(ExtremePhysique.PROFOUND_EARTH_ORIGIN, "厚土元央体");
        add(ExtremePhysique.UNIVERSE_GREAT_DERIVATION, "宇宙大衍体");
        add(ExtremePhysique.PURE_DREAM_REALITY_SEEKER, "纯梦求真体");
    }

    private void addMarkTag() {
        add(MarkTag.NATURAL, "自然");
        add(MarkTag.RACE, "种族");
        add(MarkTag.EXTREME_PHYSIQUE, "十绝体质");
    }

    private void addBeastStrengthFamily() {
        add(BeastStrengthFamily.BOAR, "猪");
        add(BeastStrengthFamily.BEAR, "熊");
    }

    private void addQiKind() {
        add(QiKind.SWORD, "剑气");
        add(QiKind.STRENGTH, "力气");
        add(QiKind.LIFE, "生气");
        add(QiKind.ESSENCE, "元气");
        add(QiKind.DEATH, "死气");
        add(QiKind.HUMAN, "人气");
        add(QiKind.HEAVEN, "天气");
        add(QiKind.EARTH, "地气");
    }

    private void addPath() {
        add(GuPath.HEAVEN, "天道");
        add(GuPath.RULE, "律道");
        add(GuPath.SPACE, "宇道");
        add(GuPath.TIME, "宙道");
        add(GuPath.HUMAN, "人道");
        add(GuPath.METAL, "金道");
        add(GuPath.WOOD, "木道");
        add(GuPath.WATER, "水道");
        add(GuPath.FIRE, "火道");
        add(GuPath.EARTH, "土道");
        add(GuPath.ICE_SNOW, "冰雪道");
        add(GuPath.LIGHTNING, "雷道");
        add(GuPath.CLOUD, "云道");
        add(GuPath.QI, "气道");
        add(GuPath.SOUND, "音道");
        add(GuPath.LIGHT, "光道");
        add(GuPath.DARK, "暗道");
        add(GuPath.POISON, "毒道");
        add(GuPath.STRENGTH, "力道");
        add(GuPath.DREAM, "梦道");
        add(GuPath.REFINEMENT, "炼道");
        add(GuPath.WISDOM, "智道");
        add(GuPath.INFORMATION, "信道");
        add(GuPath.THEFT, "偷道");
        add(GuPath.LUCK, "运道");
        add(GuPath.KILLING, "杀道");
        add(GuPath.BLOOD, "血道");
        add(GuPath.SOUL, "魂道");
        add(GuPath.ENSLAVEMENT, "奴道");
        add(GuPath.FOOD, "食道");
        add(GuPath.FORMATION, "阵道");
        add(GuPath.PAINTING, "画道");
        add(GuPath.TRANSFORMATION, "变化道");
    }

    private void addAttainment() {
        add(GuAttainment.NONE, "无");
        add(GuAttainment.ORDINARY, "普通");
        add(GuAttainment.QUASI_MASTER, "准大师");
        add(GuAttainment.MASTER, "大师");
        add(GuAttainment.QUASI_GRANDMASTER, "准宗师");
        add(GuAttainment.GRANDMASTER, "宗师");
        add(GuAttainment.QUASI_GREAT_GRANDMASTER, "准大宗师");
        add(GuAttainment.GREAT_GRANDMASTER, "大宗师");
        add(GuAttainment.QUASI_SUPREME_GRANDMASTER, "准无上大宗师");
        add(GuAttainment.SUPREME_GRANDMASTER, "无上大宗师");
    }

    private void addSoulTier() {
        add(SoulTier.ONE, "一人魂");
        add(SoulTier.TEN, "十人魂");
        add(SoulTier.HUNDRED, "百人魂");
        add(SoulTier.THOUSAND, "千人魂");
        add(SoulTier.TEN_THOUSAND, "万人魂");
        add(SoulTier.HUNDRED_THOUSAND, "十万人魂");
        add(SoulTier.MILLION, "百万人魂");
        add(SoulTier.TEN_MILLION, "千万人魂");
        add(SoulTier.HUNDRED_MILLION, "亿人魂");
    }

    private void addWisdomType() {
        add(WisdomType.THOUGHTS, "念");
        add(WisdomType.WILLS, "意");
        add(WisdomType.EMOTIONS, "情");
    }

    private void addBrilliance() {
        add(Brilliance.ORDINARY, "才情普通");
        add(Brilliance.DECENT, "才情尚可");
        add(Brilliance.DISTINCTIVE, "才情不俗");
        add(Brilliance.OUTSTANDING, "才情卓越");
        add(Brilliance.UNRIVALED, "才情旷世");
    }

    private void addThoughtTag() {
        add(ThoughtTag.NATURAL, "自然念");
        add(ThoughtTag.EVIL, "恶念");
    }
    //endregion
}
