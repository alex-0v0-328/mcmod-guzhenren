package net.alex.guzhenren.datagen.lang;

import java.util.Locale;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.gameplay.aperture.ApertureStatus;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.aperture.Title;
import net.alex.guzhenren.gameplay.body.Appendage;
import net.alex.guzhenren.gameplay.body.BodyPart;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Flesh;
import net.alex.guzhenren.gameplay.body.Injury;
import net.alex.guzhenren.gameplay.body.Organ;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.gameplay.body.Race;
import net.alex.guzhenren.gameplay.body.ShieldType;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.mind.Brilliance;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuAttainment;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffectCategory;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.soul.SoulTier;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuType;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.builder.ItemRegistrar;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.jetbrains.annotations.Nullable;

/**
 * 主模组简、繁中文翻译表的统一数据生成器；游戏内文本只有中文，英文只留在翻译键、注册 ID 与类名里。
 * <ul>
 *   <li>每种 {@link Language} 各自实例化运行一次并输出对应语言文件，确保各语言翻译键集合严格对齐：{@code en_us} 与
 *       {@code zh_cn} 写出同一份简体文本（原版只回退到 {@code en_us}，非中文客户端由此显示简体），{@code zh_tw} 与
 *       {@code zh_hk} 写出同一份繁体文本。</li>
 *   <li>方块、状态效果、实体与领域枚举（{@link NamedEnum}）均通过注册对象或枚举常量生成翻译键，避免改名后残留悬空键；物
 *       品译名由 {@link #addDeclaredItemKeys} 从 {@link ItemRegistrar} 注册链统一提取，无对应方块物品的纯方块则通过
 *       {@code addBlock} 补充（元泉[Spirit Spring]与纯水的方块翻译键同时供同名流体类型复用）。</li>
 *   <li>繁体中文默认由简体列经 {@link TraditionalGlyphs#convert} 自动转换字形，仅在两岸用词不同（如「服务器」与「伺服
 *       器」）时才显式传入第三列繁体文本。</li>
 *   <li>有意超过 300 行：全模组翻译词条按功能分区集中排布在一张对齐表中，一行对应一个翻译键。</li>
 *   <li>所有写入都经重写的 {@link #add(String, String)} 按语言取简体或繁体，父类的 {@code addBlock} 等便捷方法因此同
 *       样生效。</li>
 *   <li>⚠ 简体中文为手写权威定稿；若简体列包含 {@link TraditionalGlyphs} 未收录的新汉字，会在 {@code runData} 阶段直接
 *       抛错失败。</li>
 * </ul>
 */

public class ModLanguageProvider extends LanguageProvider {

    private final Language language;

    public enum Language { EN_US, ZH_CN, ZH_TW, ZH_HK }

    public ModLanguageProvider(PackOutput output, Language language) {
        super(output, Guzhenren.MOD_ID, language.name().toLowerCase(Locale.ROOT));
        this.language = language;
    }

    @Override
    protected void addTranslations() {
        addEnumKeys();
        addDisplayKeys();
        addDimensionKeys();
        addCommandKeys();
        addScreenKeys();
        addDeclaredItemKeys();
        addItemKeys();
        addEffectKeys();
        addEntityKeys();
        addAdvancementKeys();
        addDeathMessages();
    }

    @Override
    public void add(String key, String simplified) { add(key, simplified, null); }

    private void add(String key, String simplified, @Nullable String traditional) {
        super.add(key, switch (language) {
            case EN_US, ZH_CN -> simplified;
            case ZH_TW, ZH_HK -> traditional != null ? traditional : TraditionalGlyphs.convert(simplified);
        });
    }

    private void add(NamedEnum key, String simplified) { add(key.getTranslationKey(), simplified); }

    private void addEffect(Holder<MobEffect> effect, String simplified, String simplifiedDescription) {
        String key = effect.value().getDescriptionId();
        add(key, simplified);
        add(key + ".description", simplifiedDescription);
    }

    private void addDeclaredItemKeys() {
        for (ItemRegistrar.Entry entry : ItemRegistrar.of(ModItems.ITEMS).getEntries()) {
            add(entry.item().get().getDescriptionId(), entry.simplified(), entry.traditional());
        }
    }

    //region DISPLAY [显示]
    private void addDisplayKeys() {
        add("guzhenren.display.realm",                       "%s%s");
        add("guzhenren.display.realm_title",                 "%s%s");
        add("guzhenren.display.detail",                      " [%s]");
        add("guzhenren.display.brilliance_rate",             "每秒%s个念头");
        add("guzhenren.display.aptitude_line",               "%s [%s]");
        add("guzhenren.display.gu_line",                     "%s%s%s");
        add("guzhenren.display.gu",                          "蛊虫");
        add("guzhenren.display.gu_material",                 "蛊材");
        add("guzhenren.display.aperture_1",                  "第一空窍");
        add("guzhenren.display.aperture_2",                  "第二空窍");
        add("guzhenren.display.physique",                    "[%s]");
        add("guzhenren.display.gu_type",                     "%s");
        add("guzhenren.display.gu_type_branch",              "%s · %s");
        add("guzhenren.display.lifespan",                    "%s [%s岁]");
        add("guzhenren.display.base_fraction",               "%s成%s");
        add("guzhenren.display.base_round",                  "%s成");
        add("guzhenren.display.base_full",                   "十成");
        add("guzhenren.display.base_tens.2",                 "二");
        add("guzhenren.display.base_tens.3",                 "三");
        add("guzhenren.display.base_tens.4",                 "四");
        add("guzhenren.display.base_tens.5",                 "五");
        add("guzhenren.display.base_tens.6",                 "六");
        add("guzhenren.display.base_tens.7",                 "七");
        add("guzhenren.display.base_tens.8",                 "八");
        add("guzhenren.display.base_tens.9",                 "九");
        add("guzhenren.display.base_units.1",                "一");
        add("guzhenren.display.base_units.2",                "二");
        add("guzhenren.display.base_units.3",                "三");
        add("guzhenren.display.base_units.4",                "四");
        add("guzhenren.display.base_units.5",                "五");
        add("guzhenren.display.base_units.6",                "六");
        add("guzhenren.display.base_units.7",                "七");
        add("guzhenren.display.base_units.8",                "八");
        add("guzhenren.display.base_units.9",                "九");
        add("guzhenren.display.talent_grade.first",          "甲等");
        add("guzhenren.display.talent_grade.second",         "乙等");
        add("guzhenren.display.talent_grade.third",          "丙等");
        add("guzhenren.display.talent_grade.fourth",         "丁等");
        add("guzhenren.chat.awakened",                       "%s已开窍，资质%s");
        add("guzhenren.chat.awakened_aptitude",              "%s%s");
        add("guzhenren.display.none",                        "[无]");
        add("guzhenren.display.marks.both",                  "%s条道痕 %s点道痕");
        add("guzhenren.display.marks.marks",                 "%s条道痕");
        add("guzhenren.display.marks.points",                "%s点道痕");
        add("guzhenren.display.effect_permanent",            "永久");
        add("guzhenren.path_effect.race_talent",             "种族天赋 [%s]");
        add("guzhenren.path_effect.extreme_physique",        "十绝体质 [%s]");
        add("guzhenren.path_effect.body_skin",               "炼体·皮 [%s]");
        add("guzhenren.path_effect.body_tendon",             "炼体·筋 [%s]");
        add("guzhenren.path_effect.body_bone",               "炼体·骨 [%s]");
        add("guzhenren.path_effect.beast_strength",          "兽力 [%s]");
        add("guzhenren.path_effect.human_strength",          "人力 [%s] ×%s层");
        add("guzhenren.path_effect.timed_buff",              "%s");
        add("guzhenren.path_effect.timed_skin",              "限时皮 [%s]");
        add("guzhenren.path_effect.mugwort_shield",          "天蓬护盾");
        add("guzhenren.path_effect.vitality_leaf",           "生机叶");
        add("guzhenren.path_effect.watch_rate",              "更蛊 ×%s层");
        add("guzhenren.path_effect.zombie",                  "僵尸");
        add("guzhenren.path_effect.temp_zombie",             "临时僵尸化");
        add("guzhenren.path_effect.liquor_distilling",       "精炼真元");
        add("guzhenren.path_effect.stone_aperture",          "石化空窍");
        add("guzhenren.display.time_rate_up",                "%s倍");
        add("guzhenren.display.wild",                        "野生·%s");
        add("guzhenren.display.vital",                       "本命·%s");
        add("guzhenren.display.strength.beast_reading",      "%s%s之力");
        add("guzhenren.display.strength.innate_reading",     "%s本力");
        add("guzhenren.display.strength.beast_number.1",     "一");
        add("guzhenren.display.strength.beast_number.2",     "两");
        add("guzhenren.display.strength.beast_number.10",    "十");
        add("guzhenren.display.strength.beast_number.100",   "百");
        add("guzhenren.display.strength.beast_number.1000",  "千");
        add("guzhenren.display.strength.beast_number.10000", "万");
        add("guzhenren.display.strength.num_join",           "%s%s");
        add("guzhenren.display.strength.num_join_zero",      "%s零%s");
        add("guzhenren.display.strength.num_hundreds.1",     "一百");
        add("guzhenren.display.strength.num_hundreds.2",     "二百");
        add("guzhenren.display.strength.num_hundreds.3",     "三百");
        add("guzhenren.display.strength.num_tens_led.1",     "一十");
        add("guzhenren.display.strength.num_tens.1",         "十");
        add("guzhenren.display.strength.num_tens.2",         "二十");
        add("guzhenren.display.strength.num_tens.3",         "三十");
        add("guzhenren.display.strength.num_tens.4",         "四十");
        add("guzhenren.display.strength.num_tens.5",         "五十");
        add("guzhenren.display.strength.num_tens.6",         "六十");
        add("guzhenren.display.strength.num_tens.7",         "七十");
        add("guzhenren.display.strength.num_tens.8",         "八十");
        add("guzhenren.display.strength.num_tens.9",         "九十");
        add("guzhenren.display.strength.num_units.1",        "一");
        add("guzhenren.display.strength.num_units.2",        "二");
        add("guzhenren.display.strength.num_units.3",        "三");
        add("guzhenren.display.strength.num_units.4",        "四");
        add("guzhenren.display.strength.num_units.5",        "五");
        add("guzhenren.display.strength.num_units.6",        "六");
        add("guzhenren.display.strength.num_units.7",        "七");
        add("guzhenren.display.strength.num_units.8",        "八");
        add("guzhenren.display.strength.num_units.9",        "九");
        add("guzhenren.display.strength.jin_total",          " 合计：%s 斤");
        add("guzhenren.display.strength.jin_reading",        "%s斤之力");
        add("guzhenren.display.strength.jun_reading",        "%s钧之力");

        add("guzhenren.hud.lifespan",                        "寿元 %s");

        add("guzhenren.hud.refining",                        "炼化 %s/%s");
        add("guzhenren.hud.refining_plain",                  "炼化中");
        add("guzhenren.hud.using_plain",                     "使用中");
        add("guzhenren.hud.using",                           "使用 %s/%s");
        add("guzhenren.hud.store_strength",                  "蓄力中");
        add("guzhenren.hud.store_strength.full",             "蓄满");
        add("guzhenren.hud.gu_recipe.study",                 "研读中");
        add("guzhenren.hud.slider.cancel",                   "取消");
        add("guzhenren.hud.elder_gu.withdraw",               "取出 %s 组（%s 枚）");
        add("guzhenren.hud.elder_gu.detail",                 "库存 %s · 背包可装 %s 组");
        add("guzhenren.hud.nourishing",                      "温养 %s%%");
        add("guzhenren.hud.nourish_starving",                "真元将尽 %s%%");
        add("guzhenren.hud.shield",                          "%s %s / %s");
        add("guzhenren.hud.aperture_pressure",               "空窍压力 %s%%");
        add("guzhenren.hud.aperture_pressure_cd",            "空窍压力 %s%% [%s]");
    }
    //endregion

    //region DIMENSION [维度]
    private void addDimensionKeys() {
        add("guzhenren.dimension.treasure_yellow_heaven.inventory_full", "背包已满，物品将在离开宝黄天时掉落");
    }
    //endregion

    //region COMMAND [指令]
    private void addCommandKeys() {
        add("guzhenren.command.tagged",                           "[GZR] %s");
        add("guzhenren.command.updated",                          "已更新 %s 名玩家");
        add("guzhenren.command.unknown_value",                    "未知取值：%s");

        add("guzhenren.command.failed.awakened",                  "%s 已开窍");
        add("guzhenren.command.failed.unawakened",                "%s 尚未开窍");
        add("guzhenren.command.failed.mark_interference",         "%s：力道道痕互斥");
        add("guzhenren.command.failed.aperture_index",            "%s 无此空窍");
        add("guzhenren.command.failed.base_extreme",              "%s 未开窍或为十绝体");

        add("guzhenren.command.travel.entered",                   "已将 %s 送入%s");
        add("guzhenren.command.travel.exited",                    "已将 %s 送出锚定维度");
        add("guzhenren.command.travel.already_inside",            "%s 已在%s");
        add("guzhenren.command.travel.not_inside",                "%s 不在锚定维度内");
        add("guzhenren.command.travel.unknown_dimension",         "%s 不是锚定维度");
        add("guzhenren.command.travel.missing_dimension",         "本服务器未加载 %s", "本伺服器未載入 %s");
    }
    //endregion

    //region SCREEN [界面]
    private void addScreenKeys() {
        add("key.categories.guzhenren",                     "蛊真人");
        add("key.guzhenren.open_info",                      "打开信息面板");

        add("guzhenren.screen.info.title",                  "信息");
        add("guzhenren.screen.tab.aperture",                "空窍");
        add("guzhenren.screen.tab.body",                    "肉身");
        add("guzhenren.screen.tab.soul",                    "魂魄");
        add("guzhenren.screen.tab.paths",                   "流派");
        add("guzhenren.screen.tab.mind",                    "脑海");
        add("guzhenren.screen.tab.refinement",              "炼蛊");
        add("guzhenren.menu.aperture_storage",              "空窍存储");
        add("guzhenren.menu.vital",                         "本命");
        add("guzhenren.menu.refinement",                    "炼蛊");
        add("guzhenren.menu.soul_trade.trade",              "交易");
        add("guzhenren.menu.soul_trade.short",              "交易材料不足");
        add("guzhenren.menu.refinement.craft",              "炼制");
        add("guzhenren.menu.refinement.no_room",            "输出已满");
        add("guzhenren.menu.refinement.rings",              "外圈 蛊材  ·  内圈 蛊虫");
        add("guzhenren.menu.refinement.window",             "第 %s/%s 窗口  ·  余 %s 秒  ·  元石 %s/%s");
        add("guzhenren.menu.refinement.gap",                "炼制中");
        add("guzhenren.menu.refinement.lost_stones",        "元石不足，炼制失败");
        add("guzhenren.menu.refinement.lost_essence",       "真元耗尽，炼制失败");
        add("guzhenren.menu.refinement.lost_roll",          "炼制失败");
        add("guzhenren.menu.refinement.elder_spent",        "元老蛊用尽元石");
        add("guzhenren.menu.refinement.recipes",            "蛊方");
        add("guzhenren.menu.refinement.selected",           "已选  %s");
        add("guzhenren.menu.refinement.extra",              "格内有多余材料");
        add("guzhenren.menu.refinement.pick.title",         "选择蛊方");
        add("guzhenren.menu.refinement.pick.auto",          "自动选择");
        add("guzhenren.menu.refinement.pick.empty",         "尚不知晓任何蛊方");
        add("guzhenren.menu.refinement.pick.needs",         "所需");
        add("guzhenren.menu.refinement.pick.item",          "  %s × %s");
        add("guzhenren.menu.refinement.pick.windows",       "%s 个窗口  ·  共 %s 秒");
        add("guzhenren.menu.refinement.pick.stones",        "每窗元石  %s");
        add("guzhenren.menu.refinement.pick.cost",          "每秒真元 %s");
        add("guzhenren.menu.refinement.pick.chance",        "%s%%");
        add("guzhenren.menu.refinement.pick.success",       "成功率  %s%%");
        add("guzhenren.menu.refinement.stop",               "停止");
        add("guzhenren.menu.refinement.stopped",            "炼制中断");
        add("guzhenren.menu.refinement.pool",               "%s / %s");
        add("guzhenren.menu.refinement.pick.soul",          "每秒魂魄 %s");
        add("guzhenren.screen.label.primary_path",          "主修流派");
        add("guzhenren.screen.label.secondary_path",        "辅修流派");
        add("guzhenren.screen.pick.title",                  "选择辅修流派");
        add("guzhenren.screen.pick.hint",                   "点击选择");
        add("guzhenren.screen.nourish",                     "温养空窍");
        add("guzhenren.screen.nourish_stop",                "停止温养");
        add("guzhenren.screen.nourish_second",              "温养第二空窍");
        add("guzhenren.screen.impact",                      "冲刷窍壁");
        add("guzhenren.screen.button.storage",              "空窍存储");
        add("guzhenren.nourish.stage_up",                   "小境界提升");
        add("guzhenren.impact.success",                     "大境界提升");
        add("guzhenren.impact.hold",                        "冲刷失败，大境界未变");
        add("guzhenren.impact.drop_stage",                  "冲刷失败，小境界掉落");
        add("guzhenren.impact.drop_base",                   "冲刷失败，资质受损");
        add("guzhenren.screen.label.realm",                 "修为");
        add("guzhenren.screen.label.aperture_status",       "空窍状态");
        add("guzhenren.screen.label.talent",                "天赋");
        add("guzhenren.screen.label.essence",               "真元");
        add("guzhenren.screen.label.distilled",             "精炼真元");
        add("guzhenren.screen.label.physique",              "体质");
        add("guzhenren.screen.label.race",                  "种族");
        add("guzhenren.screen.label.tendon",                "筋");
        add("guzhenren.screen.label.bone",                  "骨");
        add("guzhenren.screen.label.skin",                  "皮");
        add("guzhenren.screen.label.flesh",                 "肉");
        add("guzhenren.screen.label.appendages",            "附加部位");
        add("guzhenren.screen.figure.entry",                "%s：%s");
        add("guzhenren.screen.label.wisdom_path_achieve",   "智道造诣");
        add("guzhenren.screen.label.soul",                  "魂魄");
        add("guzhenren.screen.label.lifespan",              "寿元");
        add("guzhenren.screen.label.path_marks",             "道痕");
        add("guzhenren.screen.label.effects",               "效果");
        add("guzhenren.screen.label.strength_path_achieve", "力道造诣");
        add("guzhenren.screen.label.time_path_achieve",     "宙道造诣");
        add("guzhenren.screen.label.time_rate_up",          "自身时间加速");
        add("guzhenren.screen.label.body_capacity",         "力量点");
        add("guzhenren.screen.label.attack",                "基础攻击力");
        add("guzhenren.screen.label.crit_chance",           "暴击率");
        add("guzhenren.screen.label.aperture_pressure",     "空窍压力");
        add("guzhenren.screen.capacity",                    "%s，承受力 %s");
        add("guzhenren.screen.capacity.body_overload",      "，超出 %s");
        add("guzhenren.menu.aperture_load",                 "空窍负担 %s / %s");
        add("guzhenren.screen.label.brilliance",            "才情");
    }
    //endregion

    //region ITEM [物品]
    private void addItemKeys() {
        addBlock(ModBlocks.PURE_WATER,                               "纯水");
        addBlock(ModBlocks.NINE_LEAF_VITALITY_GRASS_3,               "三转九叶生机草");
        addBlock(ModBlocks.INTIMATE_GRASS,                           "知心草");
        addBlock(ModBlocks.INTIMATE_GRASS_CROP,                      "知心草作物");
        addBlock(ModBlocks.SOULSUNDER_STONE,                          "荡魂山石");
        addBlock(ModBlocks.GUTS_STONE,                               "胆石");

        add("itemGroup.guzhenren.mortal_gu",                         "凡蛊");
        add("itemGroup.guzhenren.gu_material",                       "蛊材");
        add("itemGroup.guzhenren.strength_mortal_gu",                "力道蛊虫");
        add("itemGroup.guzhenren.wood_mortal_gu",                    "木道蛊虫");
        add("itemGroup.guzhenren.books",                             "蛊方、杀招与图册");

        add("guzhenren.item.failed.awakened",                        "你已开窍");
        add("guzhenren.item.failed.awakening",                       "开窍中");
        add("guzhenren.item.failed.nourishing",                      "温养空窍中");
        add("guzhenren.item.failed.awakening_danger",                "周围危险，无法开窍");
        add(Refusal.UNAWAKENED,                                      "尚未开窍");
        add("guzhenren.item.failed.essence_full",                    "真元已满");
        add(Refusal.RANK_MISMATCH,                                   "境界不符");
        add("guzhenren.item.failed.stage_peak",                      "已至%s巅峰");
        add(Refusal.CANNOT_USE,                                      "无法使用该蛊");
        add(Refusal.EFFECT_ACTIVE,                                   "该蛊效果未结束");
        add("guzhenren.item.failed.beast_strength_held",             "已有%s");
        add("guzhenren.item.failed.beast_strength_capacity",         "承受力不足");
        add("guzhenren.item.failed.human_strength_full",             "已叠满%s层");
        add("guzhenren.item.failed.human_strength_capacity",         "承受力不足");
        add("guzhenren.item.failed.mark_interference",               "力道道痕互斥");
        add(Refusal.AUTO_EFFECT,                                     "该蛊自动生效");
        add(Refusal.ESSENCE,                                         "真元不足");
        add("guzhenren.item.failed.elder_gu_empty",                  "元老蛊无元石储备");
        add("guzhenren.item.failed.elder_gu_full",                   "元老蛊元石储备已满");
        add(Refusal.BAG_FULL,                                        "背包已满");
        add("guzhenren.item.failed.elder_gu_no_stones",              "无元石存入元老蛊");
        add("guzhenren.item.failed.gu_cooldown",                     "冷却中 %s 秒");
        add("guzhenren.item.failed.all_out_needs_branch",            "需搭配蛊虫使用");
        add("guzhenren.item.failed.tempering_taken",                 "已是%s");
        add(Refusal.GU_HUNGRY,                                       "蛊虫饥饿");
        add("guzhenren.item.failed.forced_use_warning",              "蛊虫饥饿，强行催动会死");
        add("guzhenren.item.failed.vital_no_forced_use",             "本命蛊无法强行催动");
        add("guzhenren.item.failed.amplify_needs_hold",              "需要长按选择增幅目标");
        add("guzhenren.item.failed.amplify_no_target",               "快捷栏无可增幅目标");
        add("guzhenren.item.failed.no_leaves",                       "暂无生机叶");
        add("guzhenren.item.failed.as_before_no_record",             "无过去记录");
        add(Refusal.WRONG_TARGET,                                    "作用目标错误");
        add("guzhenren.item.failed.as_before_no_grief",              "本世界关闭了地形改动 - mobGriefing");
        add("guzhenren.item.failed.outside_soulsunder",               "须在荡魂山中催动");
        add("guzhenren.item.gu.invested",                            "已投入 %s/%s");
        add("guzhenren.item.gu.refine_progress",                     "炼化 %s/%s");
        add("guzhenren.item.gu.refine_cost",                         "炼化 %s");
        add("guzhenren.item.gu.hunger_progress",                     "饱食 %s/%s");
        add("guzhenren.item.gu.stored_stones",                       "存石 %s/%s");
        add("guzhenren.item.gu.leaves",                              "生机叶 %s/%s");
        add("guzhenren.item.gu.next_leaf",                           "下一片需 %s 秒");
        add("guzhenren.item.gu.next_point",                          "下一点需 %s 秒");
        add("guzhenren.item.gu.amplifies",                           "副手：本转及以下部分蛊效果 ×%s");
        add("guzhenren.item.gu.little_light_bright",                 "强光中");
        add("guzhenren.item.gu.killing_move_hint",                   "长按右键选杀招");
        add("guzhenren.killing_move.km_mortal_little_light_amplify", "小光蛊增幅");
        add("guzhenren.item.killing_move.learned",                   "习得杀招：%s");
        add("guzhenren.item.failed.killing_move_known",              "已习得杀招：%s");
        add("guzhenren.item.killing_move.mains",                     "主蛊：%s");
        add("guzhenren.item.killing_move.partner",                   "配合蛊：%s");
        add("guzhenren.item.killing_move.blade_factor",              "月刃伤害 ×%s");
        add("guzhenren.item.killing_move.known",                     "已学会");
        add("tooltip.guzhenren.uniform_color",                       "颜色：%s");
        add("guzhenren.uniform_color.white",                         "素麻米白");
        add("guzhenren.uniform_color.light_gray",                    "苍石浅灰");
        add("guzhenren.uniform_color.gray",                          "烟青深灰");
        add("guzhenren.uniform_color.black",                         "玄墨炭黑");
        add("guzhenren.uniform_color.brown",                         "粗麻茶棕");
        add("guzhenren.uniform_color.red",                           "赭石暗绯");
        add("guzhenren.uniform_color.orange",                        "陶土秋褐");
        add("guzhenren.uniform_color.yellow",                        "枯稻米黄");
        add("guzhenren.uniform_color.lime",                          "艾草枯绿");
        add("guzhenren.uniform_color.green",                         "松烟墨绿");
        add("guzhenren.uniform_color.cyan",                          "黛青石绿");
        add("guzhenren.uniform_color.light_blue",                    "远山淡靛");
        add("guzhenren.uniform_color.blue",                          "靛青书生蓝");
        add("guzhenren.uniform_color.purple",                        "沉稳墨紫");
        add("guzhenren.uniform_color.magenta",                       "绛紫藕褐");
        add("guzhenren.uniform_color.pink",                          "褪色藕荷");
        add("guzhenren.killing_move.started",                        "杀招：%s");
        add("guzhenren.killing_move.stopped",                        "杀招关闭：%s");
        add("item.guzhenren.gu_recipe.named",                        "蛊方 • %s");
        add("guzhenren.item.gu_recipe.learned",                      "习得蛊方：%s");
        add("guzhenren.item.gu_recipe.known",                        "已学会");
        add("guzhenren.item.failed.gu_recipe_missing",               "没有这张蛊方");
        add("guzhenren.killing_move.stop",                           "关闭%s");
        add("guzhenren.item.failed.killing_move_partner_missing",    "缺少杀招关键蛊虫：%s");
        add("guzhenren.item.gu.lifespan_gained",                     "寿元 +%s 年");
        add("guzhenren.item.gu.hungry",                              "%s饥饿");
        add("guzhenren.item.gu.starved",                             "%s饿死");
        add("guzhenren.item.gu.forced_dead",                         "%s强行催动而亡");
        add("guzhenren.item.gu.refine_dead",                         "%s炼制中死亡");
        add("guzhenren.item.gu.dissolved_outside_soulsunder",         "胆识蛊离开荡魂山，消散了");
        add("guzhenren.place.kind.secret_realm",                      "天地秘境");
        add("guzhenren.place.soulsunder_mountain",                    "荡魂山");
        add("guzhenren.blissful_land.fox_immortal_blissful_land",     "狐仙福地");
        add("guzhenren.blissful_land.claimed",                        "【福地易主】%s · %s");
        add("guzhenren.blissful_land.lost",                           "你失去了%s");
        add("guzhenren.blissful_land.expelled",                       "你被请出了%s");
        add("guzhenren.blissful_land.spirit.unowned",                 "此地无主。登上荡魂山巅者为主：山心 %s 格内、站在最高一层荡魂山石上。");
        add("guzhenren.blissful_land.spirit.owned",                   "此地之主：%s。");
        add("guzhenren.blissful_land.side.north",                     "北");
        add("guzhenren.blissful_land.side.east",                      "东");
        add("guzhenren.blissful_land.side.south",                     "南");
        add("guzhenren.blissful_land.side.west",                      "西");
        add("guzhenren.blissful_land.panel.expelled_outsiders",       "驱逐 %s 人");
        add("guzhenren.blissful_land.panel.expelled",                 "已驱逐");
        add("guzhenren.blissful_land.panel.expel_failed",             "驱逐不了：此人不在福地里或是主人");
        add("guzhenren.blissful_land.panel.whitelist_added",          "%s 已加入白名单");
        add("guzhenren.blissful_land.panel.player_not_found",         "找不到在线玩家 %s");
        add("guzhenren.blissful_land.panel.player_offline",           "此人已不在线");
        add("guzhenren.blissful_land.panel.cooldown",                 "还要等 %s 秒");
        add("guzhenren.blissful_land.panel.no_mountain",              "荡魂山尚未成形");
        add("guzhenren.blissful_land.panel.moved",                    "挪 %s 只生物上山");
        add("guzhenren.blissful_land.panel.outside_mountain",         "须在荡魂山中催动");
        add("guzhenren.blissful_land.panel.bred",                     "%s 只发情，用去饲料 %s 份");
        add("guzhenren.blissful_land.panel.slaughtered",              "宰杀 %s 头");
        add("guzhenren.blissful_land.panel.enclosed",                 "牧场围好，放下 %s 格栅栏");
        add("guzhenren.blissful_land.panel.enclosed_blocked",         "牧场围好，放下 %s 格栅栏，%s 格被挡住");
        add("guzhenren.blissful_land.panel.gathered",                 "收拢 %s 头进牧场");
        add("guzhenren.blissful_land.panel.harvested",                "收割 %s 株，补种 %s 格");
        add("guzhenren.blissful_land.panel.farm_full",                "田区已满");
        add("guzhenren.blissful_land.panel.farm_no_dry",              "田区里没有能开垦的干地");
        add("guzhenren.blissful_land.panel.grief_denied",             "此地不许改动地形");
        add("guzhenren.blissful_land.panel.reclaimed",                "开出第 %s 块标准田");
        add("guzhenren.blissful_land.panel.reclaimed_bare",           "开出第 %s 块标准田；库藏没有种子，先留空，之后有种子自动补种");
        add("guzhenren.blissful_land.panel.deposited",                "存入 %s 件");
        add("guzhenren.blissful_land.panel.nothing_matches",          "背包里没有库藏已有的同类物品");
        add("guzhenren.blissful_land.panel.cede_refused",             "中心 64×64 区块不可割弃");
        add("guzhenren.blissful_land.panel.ceded",                    "割弃%s侧 %s 区块，余 %s 亩");
        add("guzhenren.blissful_land.ui.tab.overview",                "总览");
        add("guzhenren.blissful_land.ui.tab.mountain",                "荡魂山");
        add("guzhenren.blissful_land.ui.tab.husbandry",               "养殖");
        add("guzhenren.blissful_land.ui.tab.farming",                 "种植");
        add("guzhenren.blissful_land.ui.tab.territory",               "疆域");
        add("guzhenren.blissful_land.ui.tab.storage",                 "库藏");
        add("guzhenren.blissful_land.ui.acres",                       "亩数");
        add("guzhenren.blissful_land.ui.guts_stones",                 "胆石");
        add("guzhenren.blissful_land.ui.animals",                     "动物");
        add("guzhenren.blissful_land.ui.crops",                       "作物");
        add("guzhenren.blissful_land.ui.present",                     "在场玩家");
        add("guzhenren.blissful_land.ui.people",                      "%s 人");
        add("guzhenren.blissful_land.ui.whitelist",                   "白名单");
        add("guzhenren.blissful_land.ui.owner",                       "主人");
        add("guzhenren.blissful_land.ui.expel",                       "驱逐");
        add("guzhenren.blissful_land.ui.expel_outsiders",             "驱逐外人");
        add("guzhenren.blissful_land.ui.move_mobs",                   "挪生物上山");
        add("guzhenren.blissful_land.ui.cooldown",                    "冷却 %s 秒");
        add("guzhenren.blissful_land.ui.drive_guts_gu",               "胆识蛊催动");
        add("guzhenren.blissful_land.ui.exempt",                      "荡魂山白名单");
        add("guzhenren.blissful_land.ui.add",                         "添加");
        add("guzhenren.blissful_land.ui.exempt_note",                 "白名单内的玩家不受震荡影响");
        add("guzhenren.blissful_land.ui.name_hint",                   "输入玩家名");
        add("guzhenren.blissful_land.ui.animal",                      "动物");
        add("guzhenren.blissful_land.ui.count",                       "数量");
        add("guzhenren.blissful_land.ui.hosting",                     "托管");
        add("guzhenren.blissful_land.ui.keep",                        "保持");
        add("guzhenren.blissful_land.ui.herd_note",                   "托管：栏内自动繁殖或宰杀到保持数");
        add("guzhenren.blissful_land.ui.pen",                         "栏内 %s · 栏外 %s");
        add("guzhenren.blissful_land.ui.enclose",                     "圈地");
        add("guzhenren.blissful_land.ui.gather",                      "收拢");
        add("guzhenren.blissful_land.ui.herd_status",                 "托管 · 下轮 %s 秒 · 上轮宰 %s 喂养 %s");
        add("guzhenren.blissful_land.ui.breed",                       "喂养繁殖");
        add("guzhenren.blissful_land.ui.slaughter",                   "宰杀收取");
        add("guzhenren.blissful_land.ui.crop",                        "作物");
        add("guzhenren.blissful_land.ui.mature",                      "成熟");
        add("guzhenren.blissful_land.ui.growing",                     "未熟");
        add("guzhenren.blissful_land.ui.fields",                      "标准田 %s / %s");
        add("guzhenren.blissful_land.ui.reclaim",                     "开垦");
        add("guzhenren.blissful_land.ui.auto_harvest",                "自动收割补种");
        add("guzhenren.blissful_land.ui.harvest",                     "收割并补种");
        add("guzhenren.blissful_land.ui.crop_pick",                   "开垦作物");
        add("guzhenren.blissful_land.ui.crop_auto",                   "自动");
        add("guzhenren.blissful_land.ui.seeds",                       "种子 %s");
        add("guzhenren.blissful_land.ui.farm_status",                 "托管 · 下轮 %s 秒 · 上轮收 %s 补 %s");
        add("guzhenren.blissful_land.ui.strip_width",                 "条带宽度");
        add("guzhenren.blissful_land.ui.chunk",                       "区块");
        add("guzhenren.blissful_land.ui.cede_preview",                "将割弃：%s侧 %s 区块，约 %s 亩");
        add("guzhenren.blissful_land.ui.cede_core",                   "%s侧已到中心，不可再割");
        add("guzhenren.blissful_land.ui.cede_note",                   "割弃的土地连同其上方块永久消失");
        add("guzhenren.blissful_land.ui.cede",                        "割弃");
        add("guzhenren.blissful_land.ui.confirm_title",               "确认割弃");
        add("guzhenren.blissful_land.ui.confirm_body",                "%s侧 %s 区块，约 %s 亩");
        add("guzhenren.blissful_land.ui.confirm_note",                "此操作不可撤销");
        add("guzhenren.blissful_land.ui.cancel",                      "取消");
        add("guzhenren.blissful_land.ui.kinds",                       "%s 种");
        add("guzhenren.blissful_land.ui.kinds_paged",                 "%s · %s/%s");
        add("guzhenren.blissful_land.ui.kinds_filtered",              "%s / %s 种");
        add("guzhenren.blissful_land.ui.search_hint",                 "搜索");
        add("guzhenren.blissful_land.ui.sort_order",                  "入库");
        add("guzhenren.blissful_land.ui.sort_count",                  "数量");
        add("guzhenren.blissful_land.ui.sort_name",                   "名称");
        add("guzhenren.blissful_land.ui.deposit_matching",            "存同类");
        add("guzhenren.blissful_land.tip.open_page",                  "点击打开「%s」页");
        add("guzhenren.blissful_land.tip.whitelist",                  "白名单内的人免荡魂山震荡，也不会被「驱逐外人」");
        add("guzhenren.blissful_land.tip.expel",                      "把此人送回他进福地前的位置");
        add("guzhenren.blissful_land.tip.expel_outsiders",            "把不在白名单的人都送回各自进福地前的位置（现在 %s 人）");
        add("guzhenren.blissful_land.tip.move_mobs",                  "把山外、牧场外可支使的生物至多 %s 个传上山，冷却 %s 秒；在山上震死会长出胆石");
        add("guzhenren.blissful_land.tip.drive_guts_gu",              "敲碎全山已加载的胆石（%s 块），每块一半机会出蛊，每只加魂魄上限 %s；须在山中");
        add("guzhenren.blissful_land.tip.add_whitelist",              "按名字加入荡魂山白名单，须是在线玩家；输入时灰字提示在线玩家名，按 Tab 补全");
        add("guzhenren.blissful_land.tip.remove_whitelist",           "移出荡魂山白名单");
        add("guzhenren.blissful_land.tip.breed",                      "每种成年动物凑对喂养，每头用 1 份库藏饲料，一次至多 %s 头");
        add("guzhenren.blissful_land.tip.breed_hungry",               "每种成年动物凑对喂养，每头用 1 份库藏饲料，一次至多 %s 头。缺饲料：%s");
        add("guzhenren.blissful_land.tip.slaughter",                  "每种宰到只剩保持数，保持数为 0 的不宰；产物进库藏，经验给你。现在会宰 %s 头");
        add("guzhenren.blissful_land.tip.herd_hosting",               "托管每 %s 秒一轮，宰杀与繁殖合计至多 %s 头");
        add("guzhenren.blissful_land.tip.herd_switch",                "托管这种动物：少了喂养繁殖，多了宰到保持数");
        add("guzhenren.blissful_land.tip.keep",                       "保持数减一或加一，按住 Shift 一次 %s");
        add("guzhenren.blissful_land.tip.pen",                        "牧场在 x %s～%s、z %s～%s：栏内、栏外各有几头动物；喂养、宰杀与托管只管栏内的");
        add("guzhenren.blissful_land.tip.enclose",                    "沿牧场边界放一圈橡木栅栏，北边正中一扇门；已有的不动，再按只补缺的");
        add("guzhenren.blissful_land.tip.gather",                     "把福地里栏外的动物传进牧场，一次至多 %s 头，冷却 %s 秒");
        add("guzhenren.blissful_land.tip.gather_wait",                "还要等 %s 秒");
        add("guzhenren.blissful_land.tip.feed",                       "库藏里的饲料：%s 份");
        add("guzhenren.blissful_land.tip.feed_unknown",               "眼下一只都没有，不知道吃什么");
        add("guzhenren.blissful_land.tip.crop_pick",                  "开垦用的作物，点击切换；自动 = 库藏里第一种种子");
        add("guzhenren.blissful_land.tip.reclaim",                    "在田区开一块 9×9 标准田种所选作物；没种子就开空田，之后自动补种；冷却 %s 秒");
        add("guzhenren.blissful_land.tip.farm_hosting",               "托管：每 %s 秒自动收割补种，每轮至多 %s 块田");
        add("guzhenren.blissful_land.tip.harvest",                    "收割全部成熟作物并补种，空的耕地也补上；种子从库藏扣");
        add("guzhenren.blissful_land.tip.side",                       "选择割弃哪一侧");
        add("guzhenren.blissful_land.tip.strip_width",                "条带宽度减一或加一，按住 Shift 一次 %s");
        add("guzhenren.blissful_land.tip.cede",                       "永久割弃所选条带，会弹窗确认；割弃了就不能恢复");
        add("guzhenren.blissful_land.tip.search",                     "按物品名或 ID 筛选库藏");
        add("guzhenren.blissful_land.tip.sort",                       "排序，点击切换：入库顺序、数量、名称");
        add("guzhenren.blissful_land.tip.deposit_matching",           "把背包里（不含快捷栏）库藏已有的同类物品全部存入");
        add("guzhenren.blissful_land.ui.inventory",                   "物品栏");
        add("guzhenren.blissful_land.ui.stored",                      "库藏 %s");
        add("guzhenren.blissful_land.ui.trimmed",                     "内容过多，悬停与图标不全");
        add("guzhenren.item.gu.health",                              "生命 %s / %s");
        add("guzhenren.item.gu.vital_lost",                          "本命蛊%s死亡");
    }
    //endregion

    //region EFFECT [状态效果]
    private void addEffectKeys() {
        addEffect(ModEffects.SECOND_WATCH_GU, "两更蛊效果",
                "自身时间与寿元消耗加快：每层 ×2，最多 2 层，与三更蛊合计最高 ×13。");
        addEffect(ModEffects.THIRD_WATCH_GU, "三更蛊效果",
                "自身时间与寿元消耗加快：每层 ×3，最多 3 层，与两更蛊合计最高 ×13。");
        addEffect(ModEffects.VITALITY_LEAF_GU, "生机叶蛊效果",
                "持续回复生命。");
        addEffect(ModEffects.HEAVENLY_MUGWORT_GU, "天蓬蛊效果",
                "获得天蓬护盾，替你吸收伤害。");
        addEffect(ModEffects.JADE_SKIN_GU, "玉皮蛊效果",
                "皮肤暂时化为玉皮，每次受到的伤害减少固定数值。");
        addEffect(ModEffects.WHITE_JADE_GU, "白玉蛊效果",
                "皮肤暂时化为白玉皮，每次受到的伤害减少固定数值，比玉皮减得更多。");
        addEffect(ModEffects.STRENGTH_QI_GU, "力气蛊效果",
                "锁定目标在 3 格外时，空手出拳化作远程攻击，伤害减半，落点爆开 3 格，人力钧力流暴击时按范围伤害半径爆开。");
        addEffect(ModEffects.FLOWER_BOAR_GU, "花豕蛊效果",
                "临时获得一猪之力（兽力），攻击提高。");
        addEffect(ModEffects.BRUTE_FORCE_LONGHORN_BEETLE_GU, "蛮力天牛蛊效果",
                "临时获得一牛之力（兽力），攻击提高。");
        addEffect(ModEffects.HORIZONTAL_CRASH_GU, "横冲蛊效果",
                "可向左右冲刺；力量足够时冲刺会撞碎沿路方块，停下时对前方造成范围伤害。");
        addEffect(ModEffects.VERTICAL_CRASH_GU, "直撞蛊效果",
                "可向前后冲刺；力量足够时冲刺会撞碎沿路方块，停下时对前方造成范围伤害。");
        addEffect(ModEffects.CHARGING_CRASH_GU, "横冲直撞蛊效果",
                "可向前后左右四个方向冲刺；力量足够时撞碎沿路方块，停下时对前方造成范围伤害。");
        addEffect(ModEffects.SELF_RELIANCE_GU, "自力更生蛊效果",
                "生命低于 75% 时自动按最大生命百分比快速回血，回到 75% 即停；力量点越高回血越快（0.5 到 1.5 倍）。");
        addEffect(ModEffects.HARDSHIP_STRENGTH_GU, "苦力蛊效果",
                "生命低于 75% 时自动生效：伤势越重，伤害倍率越高（+10% 到 +50%），兽力虚影流暴击抽出的兽力虚影越多，" +
                "人力钧力流暴击的范围伤害越大。");
        addEffect(ModEffects.ALL_OUT_EFFORT_GU, "全力以赴蛊效果",
                "兽力虚影流、人力钧力流的暴击率升至 80% / 90% / 100%（三 / 四 / 五转）。");
        addEffect(ModEffects.STORE_STRENGTH_GU, "蓄力蛊效果",
                "锁定目标后按住攻击键蓄力至少 1 秒，松手打出扇形重拳，蓄满 4 秒伤害倍率 3 倍且必定暴击；" +
                "超出承受力会反噬扣血但不致死。");
        addEffect(ModEffects.HABITUAL_STRENGTH_GU, "惯力蛊效果",
                "同蓄力蛊：锁定目标按住攻击键蓄力，松手打出扇形重拳，蓄满必定暴击；蓄力时可以移动。");
        addEffect(ModEffects.BIG_STRENGTH_GU, "大力蛊效果",
                "力量点 +32，兽力人力流派皆可用；超出承受力的部分照常发挥并反噬自身。");
        addEffect(ModEffects.CASUAL_GU, "随意蛊效果",
                "持续向脑海加入随机的念。");
        addEffect(ModEffects.MALICIOUS_THOUGHT_GU, "恶念蛊效果",
                "脑海持续产生恶念，恶念一段时间后转为普通的念；念过多会危及性命。");
        addEffect(ModEffects.BLEEDING, "流血",
                "每秒受到 1 点无视盔甲的伤害；是本模组唯一可通过喝牛奶解除的效果。");
        addEffect(ModEffects.LIQUOR_WORM, "酒虫效果",
                "一个游戏日内回复的真元转为精炼真元，1 点精炼真元抵 2 点普通真元。");
        addEffect(ModEffects.TEMP_ZOMBIE, "临时僵尸化效果",
                "暂时化为僵尸，攻击提高；期间或结束后 5 分钟内再催动僵尸蛊会永久变成僵尸。");
    }
    //endregion

    //region ENTITY [实体]
    private void addEntityKeys() {
        add("entity.guzhenren.hope_gu_entity",             "希望蛊");
        add("entity.guzhenren.white_boar_gu_entity",       "白豕蛊");
        add("entity.guzhenren.black_boar_gu_entity",       "黑豕蛊");
        add("entity.guzhenren.flower_boar_gu_entity",      "花豕蛊");
        add("entity.guzhenren.wild_boar",                  "野猪");
        add("entity.guzhenren.brown_bear",                 "棕熊");
        add("entity.guzhenren.asian_black_bear",           "亚洲黑熊");
        add("entity.guzhenren.american_black_bear",        "美洲黑熊");
        add("entity.guzhenren.albino_bear",                "白化熊");
        add("entity.guzhenren.tiger",                      "老虎");
        add("entity.guzhenren.white_tiger",                "白虎");
        add("entity.guzhenren.tortoise",                   "陆龟");
        add("entity.guzhenren.stone_tortoise",             "石龟");
        add("entity.guzhenren.yellow_ox",                  "黄牛");
        add("entity.guzhenren.water_buffalo",              "水牛");
        add("entity.guzhenren.azure_ox",                   "青牛");
        add("entity.guzhenren.elephant",                   "大象");
        add("entity.guzhenren.white_elephant",             "白象");
        add("entity.guzhenren.beast_phantom",              "兽力虚影");
        add("entity.guzhenren.horizontal_crash_gu_entity", "横冲蛊");
        add("entity.guzhenren.vertical_crash_gu_entity",   "直撞蛊");
        add("entity.guzhenren.charging_crash_gu_4_entity", "四转横冲直撞蛊");
        add("entity.guzhenren.charging_crash_gu_5_entity", "五转横冲直撞蛊");
        add("entity.guzhenren.test_trade_gu_immortal",     "测试交易蛊仙");
        add("entity.guzhenren.earth_spirit_fox_immortal_blissful_land", "狐仙福地地灵");
        add("entity.guzhenren.moon_blade",                 "月刃");
        add("entity.guzhenren.qi_wave",                    "气波");
        add("entity.guzhenren.beast_charge",               "兽力冲击");
    }
    //endregion

    //region ADVANCEMENT [进度]
    private void addAdvancementKeys() {
        add("advancements.guzhenren.first_awakening.title",       "开窍！");
        add("advancements.guzhenren.first_awakening.description", "使用你的第一只希望蛊，开出第一窍");
        add("advancements.guzhenren.rank_two.title",              "二转！");
        add("advancements.guzhenren.rank_two.description",        "第一窍晋升二转");
        add("advancements.guzhenren.rank_three.title",            "三转！");
        add("advancements.guzhenren.rank_three.description",      "第一窍晋升三转");
        add("advancements.guzhenren.rank_three_peak.title",       "我乃三转巅峰！");
        add("advancements.guzhenren.rank_three_peak.description", "第一窍达到三转巅峰");
        add("advancements.guzhenren.rank_four.title",             "四转！");
        add("advancements.guzhenren.rank_four.description",       "第一窍晋升四转");
        add("advancements.guzhenren.rank_five.title",             "五转！");
        add("advancements.guzhenren.rank_five.description",       "第一窍晋升五转");
    }
    //endregion

    //region DEATH [死亡]
    private void addDeathMessages() {
        add("death.attack.guzhenren.lifespan_exhausted",          "%1$s 寿元耗尽而亡");
        add("death.attack.guzhenren.soul_collapse",               "%1$s 魂魄衰竭而亡");
        add("death.attack.guzhenren.mind_ocean_shattered",        "%1$s 脑海炸裂而亡");
        add("death.attack.guzhenren.aperture_pressure_explosion", "%1$s 空窍压力爆炸而亡");
        add("death.attack.guzhenren.ten_extreme_disaster",        "%1$s 被十绝天灾波及而亡");
        add("death.attack.guzhenren.vital_gu_lost",               "%1$s 因本命蛊死亡而亡");
        add("death.attack.guzhenren.moon_blade",                  "%1$s 被 %2$s 的月刃斩杀");
        add("death.attack.guzhenren.moon_blade.item",             "%1$s 被 %2$s 用 %3$s 的月刃斩杀");
        add("death.attack.guzhenren.bleeding",                    "%1$s 流血而亡");
        add("death.attack.guzhenren.bleeding.player",             "%1$s 在与 %2$s 战斗时流血而亡");
        add("death.attack.guzhenren.area_damage",                 "%1$s 被 %2$s 的范围伤害打死了");
        add("death.attack.guzhenren.area_damage.item",            "%1$s 被 %2$s 用 %3$s 的范围伤害打死了");
        add("death.attack.guzhenren.qi_wave",                     "%1$s 被 %2$s 的气波打死了");
        add("death.attack.guzhenren.qi_wave.item",                "%1$s 被 %2$s 用 %3$s 的气波打死了");
        add("death.attack.guzhenren.body_overload",               "%1$s 过载反噬而亡");
        add("death.attack.guzhenren.charged_punch",               "%1$s 被 %2$s 的蓄力一拳打死了");
        add("death.attack.guzhenren.charged_punch.item",          "%1$s 被 %2$s 用 %3$s 的蓄力一拳打死了");
        add("death.attack.guzhenren.crash_strike",               "%1$s 被 %2$s 撞死了");
        add("death.attack.guzhenren.crash_strike.item",           "%1$s 被 %2$s 用 %3$s 撞死了");
        add("death.attack.guzhenren.soulsunder",                   "%1$s 在荡魂山魂飞魄散");
        add("death.attack.guzhenren.soulsunder.player",            "%1$s 在荡魂山与 %2$s 战斗时魂飞魄散");
    }
    //endregion

    //region ENUM [枚举]
    private void addEnumKeys() {
        addTitle();
        addRank();
        addStage();
        addApertureStatus();
        addTalent();
        addTenExtreme();
        addPhysique();
        addRace();
        addSoulTier();
        addEffectCarrier();
        addPathEffectCategory();
        addPath();
        addAttainment();
        addWisdomType();
        addBrilliance();
        addThoughtTag();
        addBeastStrength();
        addBeastStrengthFamily();
        addStrengthPathBranch();
        addHumanStrength();
        addGuType();
        addSkin();
        addShield();
        addTendon();
        addBone();
        addFlesh();
        addBodyPart();
        addOrgan();
        addAppendage();
        addInjury();
    }

    private void addGuType() {
        add(GuType.DEFENSIVE,        "防御");
        add(GuType.RETALIATION,      "反击");
        add(GuType.OFFENSIVE,        "攻击");
        add(GuType.MOVEMENT,         "移动");
        add(GuType.STEED,            "坐骑");
        add(GuType.FLYING,           "飞行");
        add(GuType.INVESTIGATION,    "调查");
        add(GuType.IDENTITY,         "识别");
        add(GuType.CONCEALMENT,      "隐匿");
        add(GuType.HEALING,          "治疗");
        add(GuType.STORAGE,          "存储");
        add(GuType.CULTIVATION,      "修炼");
        add(GuType.AMPLIFICATION,    "增幅");
        add(GuType.PATH_MARK_CARVING, "道痕蚀刻");
    }

    private void addSkin() {
        add(Skin.NATIVE,          "原生");
        add(Skin.JADE_SKIN,       "玉皮");
        add(Skin.WHITE_JADE_SKIN, "白玉皮");
        add(Skin.BRONZE_SKIN_1,   "一转铜皮");
        add(Skin.BRONZE_SKIN_2,   "二转铜皮");
        add(Skin.BRONZE_SKIN_3,   "三转铜皮");
        add(Skin.BRONZE_SKIN_4,   "四转铜皮");
        add(Skin.IRON_SKIN_1,     "一转铁皮");
        add(Skin.IRON_SKIN_2,     "二转铁皮");
        add(Skin.IRON_SKIN_3,     "三转铁皮");
        add(Skin.IRON_SKIN_4,     "四转铁皮");
    }

    private void addShield() {
        add(ShieldType.HEAVENLY_MUGWORT, "天蓬");
    }

    private void addTendon() {
        add(Tendon.NATIVE,         "原生");
        add(Tendon.STEEL_TENDON_3, "三转钢筋");
        add(Tendon.STEEL_TENDON_4, "四转钢筋");
    }

    private void addBone() {
        add(Bone.NATIVE,      "原生");
        add(Bone.IRON_BONE_3, "三转铁骨");
        add(Bone.IRON_BONE_4, "四转铁骨");
    }

    private void addFlesh() {
        add(Flesh.NATIVE, "原生");
    }

    private void addBodyPart() {
        add(BodyPart.HEAD,      "头");
        add(BodyPart.TORSO,     "躯干");
        add(BodyPart.RIGHT_ARM, "右臂");
        add(BodyPart.LEFT_ARM,  "左臂");
        add(BodyPart.RIGHT_LEG, "右腿");
        add(BodyPart.LEFT_LEG,  "左腿");
    }

    private void addOrgan() {
        add(Organ.EYES,  "眼");
        add(Organ.EARS,  "耳");
        add(Organ.NOSE,  "鼻");
        add(Organ.MOUTH, "口");
    }

    private void addAppendage() {
        add(Appendage.WINGS, "翼");
        add(Appendage.TAIL,  "尾");
        add(Appendage.HORNS, "角");
    }

    private void addInjury() {
        add(Injury.NONE,     "无伤");
        add(Injury.LIGHT,    "轻伤");
        add(Injury.MODERATE, "中伤");
        add(Injury.SEVERE,   "重伤");
        add(Injury.CRIPPLED, "残缺");
    }

    private void addBeastStrength() {
        add(BeastStrength.WHITE_BOAR, "白豕");
        add(BeastStrength.BLACK_BOAR, "黑豕");
        add(BeastStrength.BEAR,       "熊");
        add(BeastStrength.TIGER,      "虎");
        add(BeastStrength.STONE_TURTLE,    "石龟");
        add(BeastStrength.GALLOPING_HORSE, "驰马");
        add(BeastStrength.GREEN_BULL,      "青牛");
        add(BeastStrength.BROWN_BEAR,      "棕熊");
    }

    private void addStrengthPathBranch() {
        add(StrengthPathBranch.BEAST_STRENGTH_PHANTOM,       "兽力虚影流");
        add(StrengthPathBranch.HUMAN_JUN_STRENGTH,           "人力钧力流");
        add(StrengthPathBranch.ATMOSPHERIC_HEAVEN_AND_EARTH, "气象天地流");
        add(StrengthPathBranch.NORMAL,                       "基础力道");
    }

    private void addHumanStrength() {
        add(HumanStrength.JIN,     "斤");
        add(HumanStrength.TEN_JIN, "十斤");
        add(HumanStrength.JUN,     "钧");
        add(HumanStrength.TEN_JUN, "十钧");
        add(HumanStrength.HUNDRED_JUN, "百钧");
    }

    private void addTitle() {
        add(Title.MORTAL,      "凡人");
        add(Title.GU_MASTER,   "蛊师");
        add(Title.GU_IMMORTAL, "蛊仙");
    }

    private void addRank() {
        add(Rank.NONE,  "");
        add(Rank.ONE,   "一转");
        add(Rank.TWO,   "二转");
        add(Rank.THREE, "三转");
        add(Rank.FOUR,  "四转");
        add(Rank.FIVE,  "五转");
        add(Rank.SIX,   "六转");
        add(Rank.SEVEN, "七转");
        add(Rank.EIGHT, "八转");
        add(Rank.NINE,  "九转");
    }

    private void addStage() {
        add(Stage.NONE,   "");
        add(Stage.INIT,   "初阶");
        add(Stage.MIDDLE, "中阶");
        add(Stage.UPPER,  "高阶");
        add(Stage.PEAK,   "巅峰");
    }

    private void addApertureStatus() {
        add(ApertureStatus.NORMAL, "正常");
        add(ApertureStatus.DEAD,   "死窍");
    }

    private void addTalent() {
        add(Talent.TEN_EXTREMES, "十绝天资");
        add(Talent.FIRST,        "甲等资质");
        add(Talent.SECOND,       "乙等资质");
        add(Talent.THIRD,        "丙等资质");
        add(Talent.FOURTH,       "丁等资质");
        add(Talent.NONE,         "未觉醒");
    }

    private void addPhysique() {
        add(Physique.ZOMBIE,      "僵尸");
        add(Physique.TEMP_ZOMBIE, "临时僵尸化");
        add(Physique.EXTREME,     "十绝体");
    }

    private void addRace() {
        add(Race.HUMAN,       "人族");
        add(Race.HAIRY_MEN,   "毛民");
        add(Race.EGGMEN,      "蛋人");
        add(Race.ROCKMEN,     "石人");
        add(Race.FEATHERMEN,  "羽民");
        add(Race.INKMEN,      "墨人");
        add(Race.MINIMEN,     "小人");
        add(Race.MERMEN,      "鲛人");
        add(Race.BEASTMEN,    "兽人");
        add(Race.DRAGONMEN,   "龙人");
        add(Race.MUSHROOMMEN, "菇人");
        add(Race.SNOWMEN,     "雪人");
    }

    private void addTenExtreme() {
        add(ExtremePhysique.NONE,                               "");
        add(ExtremePhysique.VERDANT_GREAT_SUN,                  "太日阳莽体");
        add(ExtremePhysique.DESOLATE_ANCIENT_MOON,              "古月阴荒体");
        add(ExtremePhysique.NORTHERN_DARK_ICE_SOUL,             "北冥冰魄体");
        add(ExtremePhysique.BOUNDLESS_FOREST_SAMSARA,           "森海轮回体");
        add(ExtremePhysique.BLAZING_GLORY_LIGHTNING_BRILLIANCE, "炎煌雷泽体");
        add(ExtremePhysique.MYRIAD_GOLD_WONDROUS_ESSENCE,       "万金妙华体");
        add(ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL,        "大力真武体");
        add(ExtremePhysique.CAREFREE_WISDOM_HEART,              "逍遥智心体");
        add(ExtremePhysique.PROFOUND_EARTH_ORIGIN,              "厚土元央体");
        add(ExtremePhysique.UNIVERSE_GREAT_DERIVATION,          "宇宙大衍体");
        add(ExtremePhysique.PURE_DREAM_REALITY_SEEKER,          "纯梦求真体");
    }

    private void addPathEffectCategory() {
        add(PathEffectCategory.BENEFICIAL, "流派增益");
        add(PathEffectCategory.HARMFUL,    "流派减益");
    }

    private void addEffectCarrier() {
        add(EffectCarrier.APERTURE, "空窍");
        add(EffectCarrier.BODY,     "肉体");
        add(EffectCarrier.SOUL,     "魂魄");
        add(EffectCarrier.MIND,     "脑海");
    }

    private void addBeastStrengthFamily() {
        add(BeastStrengthFamily.BOAR, "猪");
        add(BeastStrengthFamily.OX,   "牛");
        add(BeastStrengthFamily.BEAR, "熊");
        add(BeastStrengthFamily.TIGER, "虎");
        add(BeastStrengthFamily.TURTLE, "龟");
        add(BeastStrengthFamily.HORSE, "马");
    }

    private void addPath() {
        add(GuPath.HEAVEN,         "天道");
        add(GuPath.RULE,           "律道");
        add(GuPath.SPACE,          "宇道");
        add(GuPath.TIME,           "宙道");
        add(GuPath.HUMAN,          "人道");
        add(GuPath.METAL,          "金道");
        add(GuPath.WOOD,           "木道");
        add(GuPath.WATER,          "水道");
        add(GuPath.FIRE,           "火道");
        add(GuPath.EARTH,          "土道");
        add(GuPath.ICE_SNOW,       "冰雪道");
        add(GuPath.LIGHTNING,      "雷道");
        add(GuPath.CLOUD,          "云道");
        add(GuPath.QI,             "气道");
        add(GuPath.SOUND,          "音道");
        add(GuPath.LIGHT,          "光道");
        add(GuPath.DARK,           "暗道");
        add(GuPath.MOON,           "月道");
        add(GuPath.POISON,         "毒道");
        add(GuPath.STRENGTH,       "力道");
        add(GuPath.DREAM,          "梦道");
        add(GuPath.REFINEMENT,     "炼道");
        add(GuPath.WISDOM,         "智道");
        add(GuPath.INFORMATION,    "信道");
        add(GuPath.THEFT,          "偷道");
        add(GuPath.LUCK,           "运道");
        add(GuPath.KILLING,        "杀道");
        add(GuPath.BLOOD,          "血道");
        add(GuPath.SOUL,           "魂道");
        add(GuPath.ENSLAVEMENT,    "奴道");
        add(GuPath.FOOD,           "食道");
        add(GuPath.FORMATION,      "阵道");
        add(GuPath.PAINTING,       "画道");
        add(GuPath.TRANSFORMATION, "变化道");
    }

    private void addAttainment() {
        add(GuAttainment.NONE,                      "无");
        add(GuAttainment.ORDINARY,                  "普通");
        add(GuAttainment.QUASI_MASTER,              "准大师");
        add(GuAttainment.MASTER,                    "大师");
        add(GuAttainment.QUASI_GRANDMASTER,         "准宗师");
        add(GuAttainment.GRANDMASTER,               "宗师");
        add(GuAttainment.QUASI_GREAT_GRANDMASTER,   "准大宗师");
        add(GuAttainment.GREAT_GRANDMASTER,         "大宗师");
        add(GuAttainment.QUASI_SUPREME_GRANDMASTER, "准无上大宗师");
        add(GuAttainment.SUPREME_GRANDMASTER,       "无上大宗师");
    }

    private void addSoulTier() {
        add(SoulTier.ONE,              "一人魂");
        add(SoulTier.TEN,              "十人魂");
        add(SoulTier.HUNDRED,          "百人魂");
        add(SoulTier.THOUSAND,         "千人魂");
        add(SoulTier.TEN_THOUSAND,     "万人魂");
        add(SoulTier.HUNDRED_THOUSAND, "十万人魂");
        add(SoulTier.MILLION,          "百万人魂");
        add(SoulTier.TEN_MILLION,      "千万人魂");
        add(SoulTier.HUNDRED_MILLION,  "亿人魂");
    }

    private void addWisdomType() {
        add(MindPoolType.THOUGHTS, "念");
        add(MindPoolType.WILLS,    "意");
        add(MindPoolType.EMOTIONS, "情");
    }

    private void addBrilliance() {
        add(Brilliance.ORDINARY,    "才情普通");
        add(Brilliance.DECENT,      "才情尚可");
        add(Brilliance.DISTINCTIVE, "才情不俗");
        add(Brilliance.OUTSTANDING, "才情卓越");
        add(Brilliance.UNRIVALED,   "才情旷世");
    }

    private void addThoughtTag() {
        add(ThoughtTag.NATURAL, "自然念");
        add(ThoughtTag.EVIL,    "恶念");
    }
    //endregion
}
