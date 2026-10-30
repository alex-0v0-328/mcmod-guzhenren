package net.alex.guzhenren.datagen.lang;

import java.util.Locale;
import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.gameplay.aperture.ApertureStatus;
import net.alex.guzhenren.gameplay.aperture.EssenceColor;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.aperture.Title;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Flesh;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.gameplay.body.Race;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.mind.Brilliance;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuAttainment;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffectCategory;
import net.alex.guzhenren.gameplay.path.qi.QiKind;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.soul.SoulTier;
import net.alex.guzhenren.item.gu.GuType;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.builder.ItemRegistrar;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.jetbrains.annotations.Nullable;

/**
 * Every string of this mod in all three languages, written as one aligned table: each line is a key, its English
 * rendering and its Simplified Chinese one. The same provider runs once per {@link Language} and writes only its own
 * column, so no language can miss a key the others have.
 *
 * <p>Extends {@link net.neoforged.neoforge.common.data.LanguageProvider}. Items, blocks, effects, entities and enum
 * constants are added through the registered object or the constant, never a raw key string, so a renamed
 * registration cannot leave a key behind pointing at nothing; the interface, command and message strings have no
 * object and are added by their key. Past 300 lines, and its sections past 40, on purpose: it is a data table, one
 * line per string. The value
 * columns align per {@code add*()} method; an entry too long for the line wraps its two renderings onto the next.
 *
 * <p>⚠ Both renderings are the authority, not a translation of each other. Deriving either side from the other is how
 * a name quietly comes to mean something it never meant.
 *
 * <p>Traditional Chinese [繁中] ({@code zh_tw}) converts the Simplified column through {@link TraditionalGlyphs},
 * which throws on a glyph it cannot place, so a new string with an unseen glyph fails {@code runData} instead of
 * shipping a Simplified glyph. An entry writes a fourth, Traditional rendering by hand only where the wording itself
 * differs (信息, 資訊); a glyph-only difference belongs in the glyph table, which fixes it everywhere it occurs.
 *
 * <p>In {@link #addItemKeys}, one {@code addBlock} entry names the Spirit Spring block, its BlockItem and the fluid
 * type together, since they share one description id; the Pure Water block and its fluid type share one likewise.
 *
 * <p>An item declared through {@link ItemRegistrar} carries its names on its registration chain instead of a line
 * here; {@link #addDeclaredItemKeys} writes them under the item's description id (a BlockItem's is its block's)
 * through the same column switch, so the Traditional conversion and the repeated-key check hold for them too.
 *
 * @author Alex
 * @version 1.0.0
 * @see TraditionalGlyphs
 * @since 1.0.0
 */

public class ModLanguageProvider extends LanguageProvider {

    private final Language language;

    public enum Language { EN_US, ZH_CN, ZH_TW }

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
        addEntityKeys();
        addAdvancementKeys();
        addDeathMessages();
    }

    private void add(String key, String english, String simplified) { add(key, english, simplified, null); }

    private void add(String key, String english, String simplified, @Nullable String traditional) {
        add(key, switch (language) {
            case EN_US -> english;
            case ZH_CN -> simplified;
            case ZH_TW -> traditional != null ? traditional : TraditionalGlyphs.convert(simplified);
        });
    }

    private void add(NamedEnum key, String english, String simplified) {
        add(key.getTranslationKey(), english, simplified);
    }

    private void addItem(Supplier<? extends Item> item, String english, String simplified) {
        add(item.get().getDescriptionId(), english, simplified);
    }

    private void addBlock(Supplier<? extends Block> block, String english, String simplified) {
        add(block.get().getDescriptionId(), english, simplified);
    }

    private void addDeclaredItemKeys() {
        for (ItemRegistrar.Entry entry : ItemRegistrar.of(ModItems.ITEMS).getEntries()) {
            add(entry.item().get().getDescriptionId(), entry.english(), entry.simplified(), entry.traditional());
        }
    }

    //region DISPLAY
    private void addDisplayKeys() {
        add("guzhenren.display.realm",                       "%s %s",                       "%s%s");
        add("guzhenren.display.realm_title",                 "%s %s",                       "%s%s");
        add("guzhenren.display.detail",                      " [%s]",                       " [%s]");
        add("guzhenren.display.brilliance_rate",             "%s thoughts/s",               "每秒%s个念头");
        add("guzhenren.display.aptitude_line",               "%s [%s]",                     "%s [%s]");
        add("guzhenren.display.gu_line",                     "%s %s %s",                    "%s%s%s");
        add("guzhenren.display.gu",                          "Gu",                          "蛊虫");
        add("guzhenren.display.gu_material",                 "Gu Material",                 "蛊材");
        add("guzhenren.display.aperture_1",                  "First Aperture",              "第一空窍");
        add("guzhenren.display.aperture_2",                  "Second Aperture",             "第二空窍");
        add("guzhenren.display.physique",                    "[%s]",                        "[%s]");
        add("guzhenren.display.gu_type",                     "%s",                          "%s");
        add("guzhenren.display.gu_type_branch",              "%s · %s",                     "%s · %s");
        add("guzhenren.display.lifespan",                    "%s [age %s]",                 "%s [%s岁]");
        add("guzhenren.display.base_fraction",               "%s %s",                       "%s成%s");
        add("guzhenren.display.base_round",                  "%s",                          "%s成");
        add("guzhenren.display.base_full",                   "One Hundred",                 "十成");
        add("guzhenren.display.base_tens.2",                 "Twenty",                      "二");
        add("guzhenren.display.base_tens.3",                 "Thirty",                      "三");
        add("guzhenren.display.base_tens.4",                 "Forty",                       "四");
        add("guzhenren.display.base_tens.5",                 "Fifty",                       "五");
        add("guzhenren.display.base_tens.6",                 "Sixty",                       "六");
        add("guzhenren.display.base_tens.7",                 "Seventy",                     "七");
        add("guzhenren.display.base_tens.8",                 "Eighty",                      "八");
        add("guzhenren.display.base_tens.9",                 "Ninety",                      "九");
        add("guzhenren.display.base_units.1",                "One",                         "一");
        add("guzhenren.display.base_units.2",                "Two",                         "二");
        add("guzhenren.display.base_units.3",                "Three",                       "三");
        add("guzhenren.display.base_units.4",                "Four",                        "四");
        add("guzhenren.display.base_units.5",                "Five",                        "五");
        add("guzhenren.display.base_units.6",                "Six",                         "六");
        add("guzhenren.display.base_units.7",                "Seven",                       "七");
        add("guzhenren.display.base_units.8",                "Eight",                       "八");
        add("guzhenren.display.base_units.9",                "Nine",                        "九");
        add("guzhenren.display.none",                        "[NONE]",                      "[无]");
        add("guzhenren.display.marks.both",                  "%s Marks %s Strands",         "%s条道痕 %s丝道痕");
        add("guzhenren.display.marks.marks",                 "%s Marks",                    "%s条道痕");
        add("guzhenren.display.marks.strands",               "%s Strands",                  "%s丝道痕");
        add("guzhenren.display.effect_permanent",            "Permanent",                   "永久");
        add("guzhenren.path_effect.race_talent",             "Race Talent [%s]",            "种族天赋 [%s]");
        add("guzhenren.path_effect.extreme_physique",        "Ten-Extremes Physique [%s]",  "十绝体质 [%s]");
        add("guzhenren.path_effect.body_skin",               "Tempered Skin [%s]",          "炼体·皮 [%s]");
        add("guzhenren.path_effect.body_tendon",             "Tempered Tendons [%s]",       "炼体·筋 [%s]");
        add("guzhenren.path_effect.body_bone",               "Tempered Bones [%s]",         "炼体·骨 [%s]");
        add("guzhenren.path_effect.beast_strength",          "Beast Strength [%s]",         "兽力 [%s]");
        add("guzhenren.path_effect.human_strength",          "Human Strength [%s] x%s",     "人力 [%s] ×%s层");
        add("guzhenren.path_effect.timed_buff",              "%s",                          "%s");
        add("guzhenren.path_effect.timed_skin",              "Timed Skin [%s]",             "限时皮 [%s]");
        add("guzhenren.path_effect.mugwort_shield",          "Heavenly Mugwort Shield",     "天蓬护盾");
        add("guzhenren.path_effect.vitality_leaf",           "Vitality Leaf",               "生机叶");
        add("guzhenren.path_effect.watch_rate",              "Watch Rate x%s",              "更蛊 ×%s层");
        add("guzhenren.path_effect.zombie",                  "Zombie",                      "僵尸");
        add("guzhenren.path_effect.temp_zombie",             "Temp Zombie",                 "临时僵尸化");
        add("guzhenren.path_effect.liquor_distilling",       "Distilling Essence",          "精炼真元");
        add("guzhenren.path_effect.stone_aperture",          "Petrified Aperture",          "石化空窍");
        add("guzhenren.display.time_rate_up",                "x%s",                         "%s倍");
        add("guzhenren.display.wild",                        "Wild %s",                     "野生·%s");
        add("guzhenren.display.vital",                       "Vital %s",                    "本命·%s");
        add("guzhenren.display.strength.beast_reading",      "[%s %s Strength]",            "[%s%s之力]");
        add("guzhenren.display.strength.beast_number.1",     "One",                         "一");
        add("guzhenren.display.strength.beast_number.2",     "Two",                         "两");
        add("guzhenren.display.strength.beast_number.10",    "Ten",                         "十");
        add("guzhenren.display.strength.beast_number.100",   "Hundred",                     "百");
        add("guzhenren.display.strength.beast_number.1000",  "Thousand",                    "千");
        add("guzhenren.display.strength.beast_number.10000", "Ten Thousand",                "万");
        add("guzhenren.display.strength.num_join",           "%s %s",                       "%s%s");
        add("guzhenren.display.strength.num_join_zero",      "%s %s",                       "%s零%s");
        add("guzhenren.display.strength.num_hundreds.1",     "One Hundred",                 "一百");
        add("guzhenren.display.strength.num_hundreds.2",     "Two Hundred",                 "二百");
        add("guzhenren.display.strength.num_hundreds.3",     "Three Hundred",               "三百");
        add("guzhenren.display.strength.num_tens_led.1",     "Ten",                         "一十");
        add("guzhenren.display.strength.num_tens.1",         "Ten",                         "十");
        add("guzhenren.display.strength.num_tens.2",         "Twenty",                      "二十");
        add("guzhenren.display.strength.num_tens.3",         "Thirty",                      "三十");
        add("guzhenren.display.strength.num_tens.4",         "Forty",                       "四十");
        add("guzhenren.display.strength.num_tens.5",         "Fifty",                       "五十");
        add("guzhenren.display.strength.num_tens.6",         "Sixty",                       "六十");
        add("guzhenren.display.strength.num_tens.7",         "Seventy",                     "七十");
        add("guzhenren.display.strength.num_tens.8",         "Eighty",                      "八十");
        add("guzhenren.display.strength.num_tens.9",         "Ninety",                      "九十");
        add("guzhenren.display.strength.num_units.1",        "One",                         "一");
        add("guzhenren.display.strength.num_units.2",        "Two",                         "二");
        add("guzhenren.display.strength.num_units.3",        "Three",                       "三");
        add("guzhenren.display.strength.num_units.4",        "Four",                        "四");
        add("guzhenren.display.strength.num_units.5",        "Five",                        "五");
        add("guzhenren.display.strength.num_units.6",        "Six",                         "六");
        add("guzhenren.display.strength.num_units.7",        "Seven",                       "七");
        add("guzhenren.display.strength.num_units.8",        "Eight",                       "八");
        add("guzhenren.display.strength.num_units.9",        "Nine",                        "九");
        add("guzhenren.display.strength.jin_total",          "[%s Jin]",                    "[%s斤]");
        add("guzhenren.display.strength.jin_reading",        "[%s Jin Strength]",           "[%s斤之力]");
        add("guzhenren.display.strength.jun_reading",        "[%s Jun Strength]",           "[%s钧之力]");

        add("guzhenren.hud.lifespan",                        "Lifespan %s",                 "寿元 %s");
        add("guzhenren.display.tenth.2",                     "20%%",                        "二成");
        add("guzhenren.display.tenth.3",                     "30%%",                        "三成");
        add("guzhenren.display.tenth.4",                     "40%%",                        "四成");
        add("guzhenren.display.tenth.5",                     "50%%",                        "五成");
        add("guzhenren.display.tenth.6",                     "60%%",                        "六成");
        add("guzhenren.display.tenth.7",                     "70%%",                        "七成");
        add("guzhenren.display.tenth.8",                     "80%%",                        "八成");
        add("guzhenren.display.tenth.9",                     "90%%",                        "九成");
        add("guzhenren.display.tenth.10",                    "100%%",                       "十成十");

        add("guzhenren.hud.refining",                        "Refine %s/%s",                "炼化 %s/%s");
        add("guzhenren.hud.refining_plain",                  "Refining",                    "炼化中");
        add("guzhenren.hud.using_plain",                     "Using",                       "使用中");
        add("guzhenren.hud.using",                           "Use %s/%s",                   "使用 %s/%s");
        add("guzhenren.hud.nourishing",                      "Nourishing %s%%",             "温养 %s%%");
        add("guzhenren.hud.nourish_starving",                "Essence running dry %s%%",    "真元将尽 %s%%");
        add("guzhenren.hud.mugwort_shield",                  "Heavenly Mugwort %s / %s",    "天蓬 %s / %s");
        add("guzhenren.hud.aperture_pressure",               "Aperture Pressure %s%%",      "空窍压力 %s%%");
        add("guzhenren.hud.aperture_pressure_cd",            "Aperture Pressure %s%% [%s]", "空窍压力 %s%% [%s]");
    }
    //endregion

    //region DIMENSION
    private void addDimensionKeys() {
        add("guzhenren.dimension.treasure_yellow_heaven.inventory_full",
                "Inventory full: what does not fit floats beside you", "背包已满，放不下的物品悬浮在身边");
    }
    //endregion

    //region COMMAND
    private void addCommandKeys() {
        add("guzhenren.command.tagged",                           "[GZR] %s", "[GZR] %s");
        add("guzhenren.command.updated",                          "Updated %s player(s)", "已更新 %s 名玩家");
        add("guzhenren.command.unknown_value",                    "Unknown value: %s", "未知的取值：%s");

        add("guzhenren.command.failed.awakened",                  "%s has already awakened", "%s 已开窍");
        add("guzhenren.command.failed.unawakened",                "%s has not awakened", "%s 尚未开窍");
        add("guzhenren.command.failed.aperture_index",            "%s has no aperture there", "%s 没有该空窍");
        add("guzhenren.command.failed.base_extreme",
                "%s is unawakened or a Ten Extremes physique; set the latter through talent",
                "%s 未开窍或是十绝体；十绝体须用 talent 修改");

        add("guzhenren.command.travel.entered",                   "Sent %s into %s", "已将 %s 送入%s");
        add("guzhenren.command.travel.exited",
                "Returned %s from the anchored dimension", "已将 %s 送出锚定维度");
        add("guzhenren.command.travel.already_inside",            "%s is already inside %s", "%s 已经在%s内");
        add("guzhenren.command.travel.not_inside",
                "%s is not inside an anchored dimension", "%s 不在任何锚定维度内");
        add("guzhenren.command.travel.unknown_dimension",         "%s is not an anchored dimension", "%s 不是锚定维度");
        add("guzhenren.command.travel.missing_dimension",
                "%s is not loaded on this server", "%s 未在此服务器加载", "%s 未在此伺服器載入");
    }
    //endregion

    //region SCREEN
    private void addScreenKeys() {
        add("key.categories.guzhenren",                     "Guzhenren", "蛊真人");
        add("key.guzhenren.open_info",                      "Open Info Panel", "打开信息面板");

        add("guzhenren.screen.info.title",                  "Info", "信息");
        add("guzhenren.screen.tab.aperture",                "Aperture", "空窍");
        add("guzhenren.screen.tab.body",                    "Body", "肉身");
        add("guzhenren.screen.tab.soul",                    "Soul", "魂魄");
        add("guzhenren.screen.tab.paths",                   "Paths", "流派");
        add("guzhenren.screen.tab.path",                    "Path Attainment", "流派造诣");
        add("guzhenren.screen.tab.mind",                    "Mind", "脑海");
        add("guzhenren.screen.tab.refinement",              "Refinement", "炼蛊");
        add("guzhenren.menu.aperture_storage",              "Aperture Storage", "空窍存储");
        add("guzhenren.menu.vital",                         "Vital", "本命");
        add("guzhenren.menu.refinement",                    "Gu Refinement", "炼蛊");
        add("guzhenren.menu.soul_trade.trade",              "Trade", "交易");
        add("guzhenren.menu.soul_trade.short",              "Not enough to pay for this trade", "材料不足，无法交易");
        add("guzhenren.menu.soul_trade.no_room",
                "Not enough inventory space: make room before trading", "背包空间不足，请先腾出空间再交易");
        add("guzhenren.menu.refinement.essence",            "Refining needs %s essence", "需真元%s炼制");
        add("guzhenren.menu.refinement.craft",              "Refine", "炼制");
        add("guzhenren.menu.refinement.no_room",            "No output space", "输出无空位");
        add("guzhenren.menu.refinement.not_awakened",       "No aperture for refining", "未开窍，无法炼蛊");
        add("guzhenren.menu.refinement.rings",
                "Outer ring: material  ·  Inner ring: Gu", "外圈 蛊材  ·  内圈 蛊虫");
        add("guzhenren.menu.refinement.window",
                "Window %s/%s  ·  %ss left  ·  stones %s/%s", "第 %s/%s 窗口  ·  余 %s 秒  ·  元石 %s/%s");
        add("guzhenren.menu.refinement.gap",                "Refining", "炼制中");
        add("guzhenren.menu.refinement.lost_stones",        "Failed: Not enough Primeval Stone", "元石不足，炼制失败");
        add("guzhenren.menu.refinement.lost_essence",       "Failed: Your essence ran dry", "真元耗尽，炼制失败");
        add("guzhenren.menu.refinement.lost_roll",          "The refinement failed", "炼制失败");
        add("guzhenren.menu.refinement.elder_spent",
                "The Primeval Elder Gu is spent, and is back in your bag", "元老蛊已耗尽，已卸下");
        add("guzhenren.menu.refinement.recipes",            "Recipes", "蛊方");
        add("guzhenren.menu.refinement.selected",           "Chosen  %s", "已选  %s");
        add("guzhenren.menu.refinement.extra",              "The grid holds more than this recipe wants", "格内有多余材料");
        add("guzhenren.menu.refinement.pick.title",         "Choose a Gu Recipe", "选择蛊方");
        add("guzhenren.menu.refinement.pick.auto",          "Auto choose", "自动选择");
        add("guzhenren.menu.refinement.pick.empty",         "You know no Gu Recipe", "尚不知晓任何蛊方");
        add("guzhenren.menu.refinement.pick.needs",         "Needs", "所需");
        add("guzhenren.menu.refinement.pick.item",          "  %s × %s", "  %s × %s");
        add("guzhenren.menu.refinement.pick.windows",       "%s windows  ·  %ss", "%s 个窗口  ·  共 %s 秒");
        add("guzhenren.menu.refinement.pick.stones",        "Stones a window  %s", "每窗元石  %s");
        add("guzhenren.menu.refinement.pick.cost",          "%s essence a second", "每秒真元 %s");
        add("guzhenren.menu.refinement.pick.chance",        "%s%%", "%s%%");
        add("guzhenren.menu.refinement.pick.success",       "Success  %s%%", "成功率  %s%%");
        add("guzhenren.menu.refinement.stop",               "Stop", "停止");
        add("guzhenren.menu.refinement.stopped",            "You broke off the refinement", "你中断了炼制");
        add("guzhenren.menu.refinement.pool",               "%s / %s", "%s / %s");
        add("guzhenren.menu.refinement.pick.soul",          "%s soul a second", "每秒魂魄 %s");
        add("guzhenren.screen.label.primary_path",          "Primary Path", "主修流派");
        add("guzhenren.screen.label.secondary_path",        "Secondary Path", "辅修流派");
        add("guzhenren.screen.pick.title",                  "Choose a Secondary Path", "选择辅修流派");
        add("guzhenren.screen.pick.hint",                   "click to set", "点击选择");
        add("guzhenren.screen.nourish",                     "Nourish Aperture", "温养空窍");
        add("guzhenren.screen.nourish_stop",                "Stop Nourishing", "停止温养");
        add("guzhenren.screen.nourish_second",              "Nourish Second Aperture", "温养第二空窍");
        add("guzhenren.screen.impact",                      "Flush Aperture Wall", "冲刷窍壁");
        add("guzhenren.screen.button.storage",              "Aperture Storage", "空窍存储");
        add("guzhenren.nourish.starved",                    "The essence ran out", "真元见底，中止温养");
        add("guzhenren.nourish.stage_up",                   "Aperture Stage Up", "小境界提升");
        add("guzhenren.impact.poor",                        "Flushing the wall needs %s essence", "冲刷窍壁需真元 %s");
        add("guzhenren.impact.success",                     "Aperture Rank Up", "大境界提升");
        add("guzhenren.impact.hold",                        "Flush Failed, No change", "冲刷失败，大境界未变");
        add("guzhenren.impact.drop_stage",                  "Flush Failed, Stage dropped", "冲刷失败，小境界掉落");
        add("guzhenren.impact.drop_base",                   "Flush Failed, Aptitude dropped", "冲刷失败，资质受损");
        add("guzhenren.screen.label.realm",                 "Realm", "修为");
        add("guzhenren.screen.label.aperture_status",       "Aperture Status", "空窍状态");
        add("guzhenren.screen.label.talent",                "Aptitude", "天赋");
        add("guzhenren.screen.label.essence",               "Essence", "真元");
        add("guzhenren.screen.label.distilled",             "Distilled", "精炼真元");
        add("guzhenren.screen.label.physique",              "Physique", "体质");
        add("guzhenren.screen.label.race",                  "Race", "种族");
        add("guzhenren.screen.label.tendon",                "Tendons", "筋");
        add("guzhenren.screen.label.bone",                  "Bones", "骨");
        add("guzhenren.screen.label.skin",                  "Skin", "皮");
        add("guzhenren.screen.label.flesh",                 "Flesh", "肉");
        add("guzhenren.screen.label.wisdom_path_achieve",   "Wisdom Path Attainment", "智道造诣");
        add("guzhenren.screen.label.soul",                  "Soul", "魂魄");
        add("guzhenren.screen.label.lifespan",              "Lifespan", "寿元");
        add("guzhenren.screen.label.qi_path_achieve",       "Qi Path Attainment", "气道造诣");
        add("guzhenren.screen.label.paths",                 "Paths", "流派");
        add("guzhenren.screen.label.strength_path_achieve", "Strength Path Attainment", "力道造诣");
        add("guzhenren.screen.label.time_path_achieve",     "Time Path Attainment", "宙道造诣");
        add("guzhenren.screen.label.time_rate_up",          "Time Rate Up", "自身时间加速");
        add("guzhenren.screen.label.body_capacity",         "Capacity", "承受");
        add("guzhenren.screen.label.attack",                "Base Attack", "基础攻击力");
        add("guzhenren.screen.label.aperture_pressure",     "Aperture Pressure", "空窍压力");
        add("guzhenren.screen.capacity",                    "%s / %s jin", "%s / %s斤");
        add("guzhenren.menu.aperture_load",                 "Load %s / %s", "空窍负担 %s / %s");
        add("guzhenren.screen.label.brilliance",            "Brilliance", "才情");
        add("guzhenren.screen.label.bare_marks",            "Body Marks", "肉体道痕");
    }
    //endregion

    //region ITEM
    private void addItemKeys() {
        addItem(ModItems.HOPE_GU,                                    "Hope Gu", "希望蛊");
        addItem(ModItems.COPPER_RELICS_GU,                           "Green Copper Relics Gu", "青铜舍利蛊");
        addItem(ModItems.STEEL_RELICS_GU,                            "Red Steel Relics Gu", "赤铁舍利蛊");
        addItem(ModItems.SILVER_RELICS_GU,                           "White Silver Relics Gu", "白银舍利蛊");
        addItem(ModItems.GOLD_RELICS_GU,                             "Yellow Gold Relics Gu", "黄金舍利蛊");
        addItem(ModItems.CRYSTAL_RELICS_GU,                          "Purple Crystal Relics Gu", "紫晶舍利蛊");
        addItem(ModItems.WHITE_BOAR_GU,                              "White Boar Gu", "白豕蛊");
        addItem(ModItems.BLACK_BOAR_GU,                              "Black Boar Gu", "黑豕蛊");
        addItem(ModItems.FLOWER_BOAR_GU,                             "Flower Boar Gu", "花豕蛊");
        addItem(ModItems.BEAR_STRENGTH_GU,                           "Bear Strength Gu", "熊力蛊");
        addItem(ModItems.DRAGONPILL_CRICKET_GU,                      "Dragonpill Cricket Gu", "龙丸蛐蛐蛊");
        addItem(ModItems.BRUTE_FORCE_LONGHORN_BEETLE_GU,             "Brute Force Longhorn Beetle Gu", "蛮力天牛蛊");
        addItem(ModItems.HORIZONTAL_CRASH_GU,                        "Horizontal Crash Gu", "横冲蛊");
        addItem(ModItems.VERTICAL_CRASH_GU,                          "Vertical Crash Gu", "直撞蛊");
        addItem(ModItems.CHARGING_CRASH_GU_4,                        "Charging Crash Gu IV", "四转横冲直撞蛊");
        addItem(ModItems.CHARGING_CRASH_GU_5,                        "Charging Crash Gu V", "五转横冲直撞蛊");
        addItem(ModItems.SELF_RELIANCE_GU_2,                         "Self Reliance Gu II", "二转自力更生蛊");
        addItem(ModItems.SELF_RELIANCE_GU_3,                         "Self Reliance Gu III", "三转自力更生蛊");
        addItem(ModItems.SELF_RELIANCE_GU_4,                         "Self Reliance Gu IV", "四转自力更生蛊");
        addItem(ModItems.HARDSHIP_STRENGTH_GU,                       "Hardship Strength Gu", "苦力蛊");
        addItem(ModItems.HEAVENLY_MUGWORT_GU,                        "Heavenly Mugwort Gu", "天蓬蛊");
        addItem(ModItems.ALL_OUT_EFFORT_GU_3,                        "All-Out Effort Gu III", "三转全力以赴蛊");
        addItem(ModItems.ALL_OUT_EFFORT_GU_4,                        "All-Out Effort Gu IV", "四转全力以赴蛊");
        addItem(ModItems.ALL_OUT_EFFORT_GU_5,                        "All-Out Effort Gu V", "五转全力以赴蛊");
        addItem(ModItems.JIN_STRENGTH_GU,                            "Jin Strength Gu", "斤力蛊");
        addItem(ModItems.TENS_JIN_STRENGTH_GU,                       "Ten Jin Strength Gu", "十斤力蛊");
        addItem(ModItems.JUN_STRENGTH_GU,                            "Jun Strength Gu", "钧力蛊");
        addItem(ModItems.TENS_JUN_STRENGTH_GU,                       "Ten Jun Strength Gu", "十钧力蛊");
        addItem(ModItems.JADE_SKIN_GU,                               "Jade Skin Gu", "玉皮蛊");
        addItem(ModItems.WHITE_JADE_GU,                              "White Jade Gu", "白玉蛊");
        addItem(ModItems.BRONZE_SKIN_GU_1,                           "Bronze Skin Gu I", "一转铜皮蛊");
        addItem(ModItems.BRONZE_SKIN_GU_2,                           "Bronze Skin Gu II", "二转铜皮蛊");
        addItem(ModItems.BRONZE_SKIN_GU_3,                           "Bronze Skin Gu III", "三转铜皮蛊");
        addItem(ModItems.BRONZE_SKIN_GU_4,                           "Bronze Skin Gu IV", "四转铜皮蛊");
        addItem(ModItems.IRON_SKIN_GU_1,                             "Iron Skin Gu I", "一转铁皮蛊");
        addItem(ModItems.IRON_SKIN_GU_2,                             "Iron Skin Gu II", "二转铁皮蛊");
        addItem(ModItems.IRON_SKIN_GU_3,                             "Iron Skin Gu III", "三转铁皮蛊");
        addItem(ModItems.IRON_SKIN_GU_4,                             "Iron Skin Gu IV", "四转铁皮蛊");
        addItem(ModItems.STEEL_TENDON_GU_3,                          "Steel Tendon Gu III", "三转钢筋蛊");
        addItem(ModItems.STEEL_TENDON_GU_4,                          "Steel Tendon Gu IV", "四转钢筋蛊");
        addItem(ModItems.IRON_BONE_GU_3,                             "Iron Bone Gu III", "三转铁骨蛊");
        addItem(ModItems.IRON_BONE_GU_4,                             "Iron Bone Gu IV", "四转铁骨蛊");
        addItem(ModItems.VITALITY_LEAF_GU,                           "Vitality Leaf Gu", "生机叶蛊");
        addItem(ModItems.LIFESPAN_GU,                                "Lifespan Gu", "寿蛊");
        addItem(ModItems.TENS_LIFESPAN_GU,                           "Ten-Year Lifespan Gu", "十年寿蛊");
        addItem(ModItems.HUNDREDS_LIFESPAN_GU,                       "Hundred-Year Lifespan Gu", "百年寿蛊");
        addItem(ModItems.THOUSANDS_LIFESPAN_GU,                      "Thousand-Year Lifespan Gu", "千年寿蛊");
        addItem(ModItems.LIQUOR_WORM,                                "Liquor Worm", "酒虫");
        addItem(ModItems.FOUR_FLAVORS_LIQUOR_WORM,                   "Four Flavors Liquor Worm", "四味酒虫");
        addItem(ModItems.SEVEN_FRAGRANCES_LIQUOR_WORM,               "Seven Fragrances Liquor Worm", "七香酒虫");
        addItem(ModItems.NINE_EYES_LIQUOR_WORM,                      "Nine Eyes Liquor Worm", "九眼酒虫");
        addItem(ModItems.ROAMING_ZOMBIE_GU,                          "Roaming Zombie Gu", "游僵蛊");
        addItem(ModItems.HAIRY_ZOMBIE_GU,                            "Hairy Zombie Gu", "毛僵蛊");
        addItem(ModItems.HOPPING_ZOMBIE_GU,                          "Hopping Zombie Gu", "跳僵蛊");
        addItem(ModItems.HEAVENLY_DEMON_ZOMBIE_GU,                   "Heavenly Demon Zombie Gu", "天魔尸蛊");
        addItem(ModItems.NIGHTMARE_ZOMBIE_GU,                        "Nightmare Zombie Gu", "梦魇尸蛊");
        addItem(ModItems.ASURA_ZOMBIE_GU,                            "Asura Zombie Gu", "修罗尸蛊");
        addItem(ModItems.EARTH_CHIEF_ZOMBIE_GU,                      "Earth Chief Zombie Gu", "地魁尸蛊");
        addItem(ModItems.PLAGUE_ZOMBIE_GU,                           "Plague Zombie Gu", "病瘟尸蛊");
        addItem(ModItems.BLOOD_WIGHT_GU,                             "Blood Wight Gu", "血鬼尸蛊");
        addItem(ModItems.PRIMEVAL_ELDER_GU_1,                        "Primeval Elder Gu I", "一转元老蛊");
        addItem(ModItems.PRIMEVAL_ELDER_GU_2,                        "Primeval Elder Gu II", "二转元老蛊");
        addItem(ModItems.PRIMEVAL_ELDER_GU_3,                        "Primeval Elder Gu III", "三转元老蛊");
        addItem(ModItems.PRIMEVAL_ELDER_GU_4,                        "Primeval Elder Gu IV", "四转元老蛊");
        addItem(ModItems.PRIMEVAL_ELDER_GU_5,                        "Primeval Elder Gu V", "五转元老蛊");
        addItem(ModItems.HEAVENLY_ESSENCE_TREASURE_LOTUS_GU,         "Heavenly Essence Treasure Lotus Gu", "天元宝莲");
        addItem(ModItems.HEAVENLY_ESSENCE_TREASURE_MONARCH_LOTUS_GU,
                "Heavenly Essence Treasure Monarch Lotus Gu", "天元宝君莲");
        addItem(ModItems.HEAVENLY_ESSENCE_TREASURE_KING_LOTUS_GU,
                "Heavenly Essence Treasure King Lotus Gu", "天元宝王莲");
        addItem(ModItems.NINE_LEAF_VITALITY_GRASS,                   "Nine Leaf Vitality Grass", "九叶生机草");
        addItem(ModItems.SECOND_WATCH_GU,                            "Second Watch Gu", "两更蛊");
        addItem(ModItems.THIRD_WATCH_GU,                             "Third Watch Gu", "三更蛊");
        addItem(ModItems.MAN_AS_BEFORE_GU,                           "Man As Before Gu", "人如故");
        addItem(ModItems.MOUNTAIN_AS_BEFORE_GU,                      "Mountain As Before Gu", "山如故");
        addItem(ModItems.RIVER_AS_BEFORE_GU,                         "River As Before Gu", "江如故");
        addItem(ModItems.MALICIOUS_THOUGHT_GU_2,                     "Malicious Thought Gu II", "二转恶念蛊");
        addItem(ModItems.MALICIOUS_THOUGHT_GU_3,                     "Malicious Thought Gu III", "三转恶念蛊");
        addItem(ModItems.MALICIOUS_THOUGHT_GU_4,                     "Malicious Thought Gu IV", "四转恶念蛊");
        addItem(ModItems.MALICIOUS_THOUGHT_GU_5,                     "Malicious Thought Gu V", "五转恶念蛊");
        addItem(ModItems.GUTS_GU,                                    "Guts Gu", "一转胆识蛊");
        addItem(ModItems.CASUAL_GU_1,                                "Casual Gu I", "一转随意蛊");
        addItem(ModItems.CASUAL_GU_2,                                "Casual Gu II", "二转随意蛊");
        addItem(ModItems.STONE_APERTURE_GU_3,                        "Stone Aperture Gu III", "三转石窍蛊");
        addItem(ModItems.STONE_APERTURE_GU_4,                        "Stone Aperture Gu IV", "四转石窍蛊");
        addItem(ModItems.STONE_APERTURE_GU_5,                        "Stone Aperture Gu V", "五转石窍蛊");
        addItem(ModItems.AMPLIFY_EFFECT_GU_2,                        "Amplify Effect Gu II", "二转功倍蛊");
        addItem(ModItems.AMPLIFY_EFFECT_GU_3,                        "Amplify Effect Gu III", "三转功倍蛊");
        addItem(ModItems.AMPLIFY_EFFECT_GU_4,                        "Amplify Effect Gu IV", "四转功倍蛊");
        addItem(ModItems.AMPLIFY_EFFECT_GU_5,                        "Amplify Effect Gu V", "五转功倍蛊");
        addItem(ModItems.SECOND_APERTURE_GU_1,                       "Second Aperture Gu I", "一转第二空窍蛊");
        addItem(ModItems.SECOND_APERTURE_GU_2,                       "Second Aperture Gu II", "二转第二空窍蛊");
        addItem(ModItems.SECOND_APERTURE_GU_3,                       "Second Aperture Gu III", "三转第二空窍蛊");
        addItem(ModItems.SECOND_APERTURE_GU_4,                       "Second Aperture Gu IV", "四转第二空窍蛊");
        addItem(ModItems.SECOND_APERTURE_GU_5,                       "Second Aperture Gu V", "五转第二空窍蛊");
        addItem(ModItems.PRIMEVAL_STONE,                             "Primeval Stone", "元石");
        addItem(ModItems.HUMAN_APERTURE_1,                           "Human Aperture I", "一转人窍");
        addItem(ModItems.HUMAN_APERTURE_2,                           "Human Aperture II", "二转人窍");
        addItem(ModItems.HUMAN_APERTURE_3,                           "Human Aperture III", "三转人窍");
        addItem(ModItems.HUMAN_APERTURE_4,                           "Human Aperture IV", "四转人窍");
        addItem(ModItems.HUMAN_APERTURE_5,                           "Human Aperture V", "五转人窍");
        addItem(ModItems.LIQUOR,                                     "Liquor", "酒");
        addItem(ModItems.SOUR_LIQUOR,                                "Sour Liquor", "酸酒");
        addItem(ModItems.SWEET_LIQUOR,                               "Sweet Liquor", "甜酒");
        addItem(ModItems.BITTER_LIQUOR,                              "Bitter Liquor", "苦酒");
        addItem(ModItems.SPICY_LIQUOR,                               "Spicy Liquor", "辣酒");
        addBlock(ModBlocks.SPIRIT_SPRING,                            "Spirit Spring", "元泉");
        addBlock(ModBlocks.PURE_WATER,                               "Pure Water", "纯水");
        addItem(ModItems.PURE_WATER_BUCKET,                          "Pure Water Bucket", "纯水桶");
        addBlock(ModBlocks.NINE_LEAF_VITALITY_GRASS,                 "Nine Leaf Vitality Grass", "九叶生机草");
        addBlock(ModBlocks.INTIMATE_GRASS,                           "Intimate Grass", "知心草");
        addItem(ModItems.INTIMATE_GRASS_BUNDLE,                      "Intimate Grass Bundle", "知心草束");
        addItem(ModItems.MOON_ORCHID_PETALS,                         "Moon Orchid Petals", "月兰花瓣");
        addItem(ModItems.ROTTEN_FROZEN_MUD,                          "Rotten Frozen Mud", "腐泥冻土");
        addItem(ModItems.JADE,                                       "Jade", "玉");
        addItem(ModItems.ROUGH_JADE,                                 "Rough Jade", "玉原石");
        addItem(ModItems.JADEITE,                                    "Jadeite", "翡翠");
        addItem(ModItems.ROUGH_JADEITE,                              "Rough Jadeite", "翡翠原石");
        addItem(ModItems.STEEL_INGOT,                                "Steel Ingot", "钢锭");
        addBlock(ModBlocks.STEEL_BLOCK,                              "Steel Block", "钢块");

        addItem(ModItems.SWORD_QI_1,                                 "Sword Qi I", "一转剑气");
        addItem(ModItems.SWORD_QI_2,                                 "Sword Qi II", "二转剑气");
        addItem(ModItems.SWORD_QI_3,                                 "Sword Qi III", "三转剑气");
        addItem(ModItems.SWORD_QI_4,                                 "Sword Qi IV", "四转剑气");
        addItem(ModItems.SWORD_QI_5,                                 "Sword Qi V", "五转剑气");
        addItem(ModItems.STRENGTH_QI_1,                              "Strength Qi I", "一转力气");
        addItem(ModItems.STRENGTH_QI_2,                              "Strength Qi II", "二转力气");
        addItem(ModItems.STRENGTH_QI_3,                              "Strength Qi III", "三转力气");
        addItem(ModItems.STRENGTH_QI_4,                              "Strength Qi IV", "四转力气");
        addItem(ModItems.STRENGTH_QI_5,                              "Strength Qi V", "五转力气");
        addItem(ModItems.LIFE_QI_1,                                  "Life Qi I", "一转生气");
        addItem(ModItems.LIFE_QI_2,                                  "Life Qi II", "二转生气");
        addItem(ModItems.LIFE_QI_3,                                  "Life Qi III", "三转生气");
        addItem(ModItems.LIFE_QI_4,                                  "Life Qi IV", "四转生气");
        addItem(ModItems.LIFE_QI_5,                                  "Life Qi V", "五转生气");
        addItem(ModItems.ESSENCE_QI_1,                               "Essence Qi I", "一转元气");
        addItem(ModItems.ESSENCE_QI_2,                               "Essence Qi II", "二转元气");
        addItem(ModItems.ESSENCE_QI_3,                               "Essence Qi III", "三转元气");
        addItem(ModItems.ESSENCE_QI_4,                               "Essence Qi IV", "四转元气");
        addItem(ModItems.ESSENCE_QI_5,                               "Essence Qi V", "五转元气");
        addItem(ModItems.DEATH_QI_5,                                 "Death Qi V", "五转死气");
        addItem(ModItems.ADVANCEMENT_ICON,                           "Advancement Icon", "进度图标");

        add("itemGroup.guzhenren.mortal_gu",                         "Mortal Gu", "凡蛊");
        add("itemGroup.guzhenren.gu_material",                       "Gu Material", "蛊材");
        add("itemGroup.guzhenren.strength_mortal_gu",                "Strength Mortal Gu", "力道凡蛊");
        add("itemGroup.guzhenren.wood_mortal_gu",                    "Wood Mortal Gu", "木道凡蛊");

        add("guzhenren.item.failed.awakened",                        "You have already awakened", "你已开窍");
        add("guzhenren.item.failed.unawakened",                      "You have NOT awakened", "你未开窍");
        add("guzhenren.item.failed.essence_full",                    "Essence is already FULL", "真元已满");
        add("guzhenren.item.failed.rank_mismatch",                   "Wrong realm - this Gu needs %s", "境界不符 — 此蛊需%s");
        add("guzhenren.item.failed.stage_peak",                      "You are at the Stage Peak", "已至小境界巅峰");
        add("guzhenren.item.failed.second_aperture_rank",
                "The second aperture already stands at or above this rank", "第二空窍已是该转数或更高");
        add("guzhenren.item.failed.aperture_unavailable",            "No usable aperture remains", "没有可以作用的空窍");
        add("guzhenren.item.failed.beast_strength_held",             "You already hold the %s's strength", "已有%s之力");
        add("guzhenren.item.failed.human_strength_full",
                "This strength is already at %s layers", "此力已满%s层");
        add("guzhenren.item.failed.vitality_active",                 "Vitality Leaf is still working", "生机叶效果未散");
        add("guzhenren.item.failed.refine_essence",                  "NOT enough essence to refine", "真元不足无法炼化");
        add("guzhenren.item.failed.essence",                         "NOT enough essence", "真元不足");
        add("guzhenren.item.failed.liquor_rank",                     "Only a %s Gu Master can use this", "%s蛊师才可使用");
        add("guzhenren.item.failed.liquor_distilling",               "You are already distilling", "精炼未止");
        add("guzhenren.item.failed.elder_gu_empty",                  "This Gu holds no Primeval Stones", "蛊中无元石");
        add("guzhenren.item.failed.elder_gu_full",                   "This Gu is full of Primeval Stones", "蛊中元石已满");
        add("guzhenren.item.failed.elder_gu_no_stones",
                "You carry no Primeval Stones to store", "身上无元石可存");
        add("guzhenren.item.failed.gu_cooldown",                     "This Gu needs another %s seconds", "此蛊尚需 %s 秒");
        add("guzhenren.item.failed.all_out_active",
                "The all-out effort has not yet passed", "全力以赴蛊效果未散");
        add("guzhenren.item.failed.buff_stronger",
                "A stronger %s effect is still running", "更强的%s效果未散");
        add("guzhenren.item.failed.skin_taken",
                "Your skin is already %s", "皮肤已是%s");
        add("guzhenren.item.failed.tendon_taken",
                "Your tendons are already %s", "筋已是%s");
        add("guzhenren.item.failed.bone_taken",
                "Your bones are already %s", "骨已是%s");
        add("guzhenren.item.failed.zombie_already",
                "Already a zombie, nothing left to turn", "已经是僵尸，无可再变");
        add("guzhenren.item.failed.gu_starving",
                "This Gu is too hungry - feed it first", "蛊已太饿，需先喂食");
        add("guzhenren.item.failed.no_use",                          "This Gu needs no use", "此蛊无需使用");
        add("guzhenren.item.failed.amplify_offhand",
                "Hold right-click to pick a Gu, or hold it in the offhand", "按住右键选蛊，或副手持握配合催动");
        add("guzhenren.item.failed.amplify_no_target",               "No Gu on the hotbar to amplify", "快捷栏没有可增幅的蛊");
        add("guzhenren.item.failed.no_leaves",
                "No Vitality Leaf left - the next grows in %s s", "生机叶已摘光，下一片需 %s 秒");
        add("guzhenren.item.failed.leaves_full",                     "All nine Vitality Leaves have grown", "生机叶已满九片");
        add("guzhenren.item.failed.as_before_recovering",            "This Gu recovers in %s s", "此蛊力竭，需 %s 秒");
        add("guzhenren.item.failed.as_before_no_memory",             "This Gu has not yet known you", "此蛊尚未识你");
        add("guzhenren.item.failed.as_before_too_young",             "This Gu has not yet known a full day", "此蛊尚未满一日");
        add("guzhenren.item.failed.as_before_unknown_land",          "This Gu has not yet known this land", "此蛊未识此地");
        add("guzhenren.item.failed.as_before_not_land",              "This is no mountain", "此地非山");
        add("guzhenren.item.failed.as_before_not_water",             "This is no river", "此地非江");
        add("guzhenren.item.failed.as_before_no_grief",
                "Terrain changes are off in this world (mobGriefing)", "本世界已关闭地形改动（mobGriefing）");
        add("guzhenren.item.gu.invested",                            "Invested %s/%s", "已投入 %s/%s");
        add("guzhenren.item.gu.refine_progress",                     "Refined %s/%s", "炼化 %s/%s");
        add("guzhenren.item.gu.refine_cost",                         "Refine %s", "炼化 %s");
        add("guzhenren.item.gu.hunger_progress",                     "Fed %s/%s", "饱食 %s/%s");
        add("guzhenren.item.gu.stored_stones",                       "Stored %s/%s", "存石 %s/%s");
        add("guzhenren.item.gu.leaves",                              "Vitality Leaves %s/%s", "生机叶 %s/%s");
        add("guzhenren.item.gu.next_leaf",                           "Next leaf in %s s", "下一片需 %s 秒");
        add("guzhenren.item.gu.next_point",                          "Next point in %s s", "下一点需 %s 秒");
        add("guzhenren.item.gu.amplifies",
                "Offhand: some Gu up to its rank, effect ×%s", "副手：本转及以下部分蛊效果 ×%s");
        add("guzhenren.item.gu.as_before_body",                      "Your body is as it was a day ago", "身已如昨");
        add("guzhenren.item.gu.as_before_land",                      "The land is as it was first made", "山已如初");
        add("guzhenren.item.gu.as_before_water",                     "The waters are as they were first made", "江已如初");
        add("guzhenren.item.gu.lifespan_gained",                     "Lifespan +%s years", "寿元 +%s 年");
        add("guzhenren.item.gu.hungry",                              "Your %s is hungry", "%s饿了");
        add("guzhenren.item.gu.starved",                             "Your %s starved to death", "%s饿死了");
        add("guzhenren.item.gu.exhausted",
                "Your %s was forced past its limit and died", "强行催动%s，蛊已死亡");
        add("guzhenren.item.gu.ruined",
                "Your %s was ruined in the refinement", "%s在炼制中失败");
        add("guzhenren.item.gu.health",                              "Health %s / %s", "生命 %s / %s");
        add("guzhenren.item.gu.vital_lost",
                "Your %s is gone -- health, soul and mind all suffer!", "本命%s死亡，你也遭受重创");
        add("guzhenren.item.death_qi_cured",
                "Life Qi dispels the Death Qi -- %s years of lifespan restored", "死气化解 — 回复 %s 年寿元");

        add("effect.guzhenren.vitality_leaf_gu",                     "Vitality Leaf Gu Effect", "生机叶蛊效果");
        add("effect.guzhenren.liquor_worm",                          "Liquor Worm Effect", "酒虫效果");
        add("effect.guzhenren.life_qi",                              "Life Qi Effect", "生气效果");
        add("effect.guzhenren.essence_qi",                           "Essence Qi Effect", "元气效果");
        add("effect.guzhenren.death_qi",                             "Death Qi Effect", "死气效果");
        add("effect.guzhenren.strength_qi",                          "Strength Qi Effect", "力气效果");
        add("effect.guzhenren.flower_boar_gu",                       "Flower Boar Gu Effect", "花豕蛊效果");
        add("effect.guzhenren.dragonpill_cricket_gu",                "Dragonpill Cricket Gu Effect", "龙丸蛐蛐蛊效果");
        add("effect.guzhenren.brute_force_longhorn_beetle_gu",
                "Brute Force Longhorn Beetle Gu Effect", "蛮力天牛蛊效果");
        add("effect.guzhenren.horizontal_crash_gu",                  "Horizontal Crash Gu Effect", "横冲蛊效果");
        add("effect.guzhenren.vertical_crash_gu",                    "Vertical Crash Gu Effect", "直撞蛊效果");
        add("effect.guzhenren.charging_crash_gu",                    "Charging Crash Gu Effect", "横冲直撞蛊效果");
        add("effect.guzhenren.self_reliance_gu",                     "Self Reliance Gu Effect", "自力更生蛊效果");
        add("effect.guzhenren.hardship_strength_gu",                 "Hardship Strength Gu Effect", "苦力蛊效果");
        add("effect.guzhenren.jade_skin_gu",                         "Jade Skin Gu Effect", "玉皮蛊效果");
        add("effect.guzhenren.white_jade_gu",                        "White Jade Gu Effect", "白玉蛊效果");
        add("effect.guzhenren.heavenly_mugwort_gu",                  "Heavenly Mugwort Gu Effect", "天蓬蛊效果");
        add("effect.guzhenren.all_out_effort_gu",                    "All-Out Effort Gu Effect", "全力以赴蛊效果");
        add("effect.guzhenren.temp_zombie",                          "Temp Zombie Effect", "临时僵尸化效果");
        add("effect.guzhenren.second_watch_gu",                      "Second Watch Gu Effect", "两更蛊效果");
        add("effect.guzhenren.third_watch_gu",                       "Third Watch Gu Effect", "三更蛊效果");
        add("effect.guzhenren.malicious_thought_gu",                 "Malicious Thought Gu Effect", "恶念蛊效果");
        add("effect.guzhenren.casual_gu",                            "Casual Gu Effect", "随意蛊效果");
    }
    //endregion

    //region ENTITY
    private void addEntityKeys() {
        add("entity.guzhenren.hope_gu_entity",             "Hope Gu",                "希望蛊");
        add("entity.guzhenren.white_boar_gu_entity",       "White Boar Gu",          "白豕蛊");
        add("entity.guzhenren.black_boar_gu_entity",       "Black Boar Gu",          "黑豕蛊");
        add("entity.guzhenren.flower_boar_gu_entity",      "Flower Boar Gu",         "花豕蛊");
        add("entity.guzhenren.wild_boar",                  "Wild Boar",              "野猪");
        add("entity.guzhenren.brown_bear",                 "Brown Bear",             "棕熊");
        add("entity.guzhenren.asian_black_bear",           "Asian Black Bear",       "亚洲黑熊");
        add("entity.guzhenren.american_black_bear",        "American Black Bear",    "美洲黑熊");
        add("entity.guzhenren.albino_bear",                "Albino Bear",            "白化熊");
        add("entity.guzhenren.tiger",                      "Tiger",                  "老虎");
        add("entity.guzhenren.white_tiger",                "White Tiger",            "白虎");
        add("entity.guzhenren.horizontal_crash_gu_entity", "Horizontal Crash Gu",    "横冲蛊");
        add("entity.guzhenren.vertical_crash_gu_entity",   "Vertical Crash Gu",      "直撞蛊");
        add("entity.guzhenren.charging_crash_gu_4_entity", "Charging Crash Gu IV",   "四转横冲直撞蛊");
        add("entity.guzhenren.charging_crash_gu_5_entity", "Charging Crash Gu V",    "五转横冲直撞蛊");
        add("entity.guzhenren.test_trade_gu_immortal",     "Test Trade Gu Immortal", "测试交易蛊仙");
    }
    //endregion

    //region ADVANCEMENT
    private void addAdvancementKeys() {
        add("advancements.guzhenren.first_awakening.title",       "Awakening!", "开窍！");
        add("advancements.guzhenren.first_awakening.description",
                "Use your first Hope Gu to open your first aperture", "使用你的第一只希望蛊，开出第一窍");
        add("advancements.guzhenren.rank_two.title",              "Rank II!", "二转！");
        add("advancements.guzhenren.rank_two.description",
                "Advance your first aperture to Rank II", "第一窍晋升二转");
        add("advancements.guzhenren.rank_three.title",            "Rank III!", "三转！");
        add("advancements.guzhenren.rank_three.description",
                "Advance your first aperture to Rank III", "第一窍晋升三转");
        add("advancements.guzhenren.rank_three_peak.title",       "I Am Rank III Peak!", "我乃三转巅峰！");
        add("advancements.guzhenren.rank_three_peak.description",
                "Bring your first aperture to Rank III Peak", "第一窍达到三转巅峰");
        add("advancements.guzhenren.rank_four.title",             "Rank IV!", "四转！");
        add("advancements.guzhenren.rank_four.description",
                "Advance your first aperture to Rank IV", "第一窍晋升四转");
        add("advancements.guzhenren.rank_five.title",             "Rank V!", "五转！");
        add("advancements.guzhenren.rank_five.description",
                "Advance your first aperture to Rank V", "第一窍晋升五转");
    }
    //endregion

    //region DEATH
    private void addDeathMessages() {
        add("death.attack.guzhenren.lifespan_exhausted",          "%1$s ran out of lifespan", "%1$s 寿元耗尽而亡");
        add("death.attack.guzhenren.soul_collapse",               "%1$s suffered soul collapse", "%1$s 魂魄衰竭而亡");
        add("death.attack.guzhenren.mind_ocean_shattered",        "%1$s shattered their Mind Ocean", "%1$s 脑海炸裂而亡");
        add("death.attack.guzhenren.aperture_pressure_explosion",
                "%1$s died in an aperture pressure explosion", "%1$s 空窍压力爆炸而亡");
        add("death.attack.guzhenren.ten_extreme_disaster",
                "%1$s was killed by a Ten-Extremes disaster", "%1$s 被十绝天灾波及而亡");
        add("death.attack.guzhenren.vital_gu_lost",               "%1$s lost their Vital Gu", "%1$s 因本命蛊死亡而亡");
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
        addEffectCarrier();
        addPathEffectCategory();
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
        addGuType();
        addSkin();
        addTendon();
        addBone();
        addFlesh();
    }

    private void addGuType() {
        add(GuType.DEFENSIVE,        "Defensive",        "防御");
        add(GuType.RETALIATION,      "Retaliation",      "反击");
        add(GuType.OFFENSIVE,        "Offensive",        "攻击");
        add(GuType.MOVEMENT,         "Movement",         "移动");
        add(GuType.STEED,            "Steed",            "坐骑");
        add(GuType.FLYING,           "Flying",           "飞行");
        add(GuType.INVESTIGATION,    "Investigation",    "调查");
        add(GuType.IDENTITY,         "Identity",         "识别");
        add(GuType.CONCEALMENT,      "Concealment",      "隐匿");
        add(GuType.HEALING,          "Healing",          "治疗");
        add(GuType.STORAGE,          "Storage",          "存储");
        add(GuType.CULTIVATION,      "Cultivation",      "修炼");
        add(GuType.AMPLIFICATION,    "Amplification",    "增幅");
        add(GuType.DAO_MARK_CARVING, "Dao Mark Carving", "道痕蚀刻");
    }

    private void addSkin() {
        add(Skin.NATIVE,          "Original",        "原生");
        add(Skin.JADE_SKIN,       "Jade Skin",       "玉皮");
        add(Skin.WHITE_JADE_SKIN, "White Jade Skin", "白玉皮");
        add(Skin.BRONZE_SKIN_1,   "Bronze Skin I",   "一转铜皮");
        add(Skin.BRONZE_SKIN_2,   "Bronze Skin II",  "二转铜皮");
        add(Skin.BRONZE_SKIN_3,   "Bronze Skin III", "三转铜皮");
        add(Skin.BRONZE_SKIN_4,   "Bronze Skin IV",  "四转铜皮");
        add(Skin.IRON_SKIN_1,     "Iron Skin I",     "一转铁皮");
        add(Skin.IRON_SKIN_2,     "Iron Skin II",    "二转铁皮");
        add(Skin.IRON_SKIN_3,     "Iron Skin III",   "三转铁皮");
        add(Skin.IRON_SKIN_4,     "Iron Skin IV",    "四转铁皮");
    }

    private void addTendon() {
        add(Tendon.NATIVE,         "Original",         "原生");
        add(Tendon.STEEL_TENDON_3, "Steel Tendon III", "三转钢筋");
        add(Tendon.STEEL_TENDON_4, "Steel Tendon IV",  "四转钢筋");
    }

    private void addBone() {
        add(Bone.NATIVE,      "Original",      "原生");
        add(Bone.IRON_BONE_3, "Iron Bone III", "三转铁骨");
        add(Bone.IRON_BONE_4, "Iron Bone IV",  "四转铁骨");
    }

    private void addFlesh() {
        add(Flesh.NATIVE, "Original", "原生");
    }

    private void addBeastStrength() {
        add(BeastStrength.WHITE_BOAR, "White Boar", "白豕");
        add(BeastStrength.BLACK_BOAR, "Black Boar", "黑豕");
        add(BeastStrength.BEAR,       "Bear",       "熊");
    }

    private void addStrengthPathBranch() {
        add(StrengthPathBranch.BEAST_STRENGTH_PHANTOM,       "Beast Strength Phantom Branch",       "兽力虚影流");
        add(StrengthPathBranch.HUMAN_JUN_STRENGTH,           "Human Jun Strength Branch",           "人力钧力流");
        add(StrengthPathBranch.ATMOSPHERIC_HEAVEN_AND_EARTH, "Atmospheric Heaven and Earth Branch", "气象天地流");
        add(StrengthPathBranch.NORMAL,                       "Normal",                              "基础力道");
    }

    private void addHumanStrength() {
        add(HumanStrength.JIN,     "Jin",     "斤");
        add(HumanStrength.TEN_JIN, "Ten Jin", "十斤");
        add(HumanStrength.JUN,     "Jun",     "钧");
        add(HumanStrength.TEN_JUN, "Ten Jun", "十钧");
    }

    private void addTitle() {
        add(Title.MORTAL,      "Mortal",      "凡人");
        add(Title.GU_MASTER,   "Gu Master",   "蛊师");
        add(Title.GU_IMMORTAL, "Gu Immortal", "蛊仙");
    }

    private void addRank() {
        add(Rank.NONE,  "",          "");
        add(Rank.ONE,   "Rank I",    "一转");
        add(Rank.TWO,   "Rank II",   "二转");
        add(Rank.THREE, "Rank III",  "三转");
        add(Rank.FOUR,  "Rank IV",   "四转");
        add(Rank.FIVE,  "Rank V",    "五转");
        add(Rank.SIX,   "Rank VI",   "六转");
        add(Rank.SEVEN, "Rank VII",  "七转");
        add(Rank.EIGHT, "Rank VIII", "八转");
        add(Rank.NINE,  "Rank IX",   "九转");
    }

    private void addStage() {
        add(Stage.NONE,   "",        "");
        add(Stage.INIT,   "Initial", "初阶");
        add(Stage.MIDDLE, "Middle",  "中阶");
        add(Stage.UPPER,  "Upper",   "高阶");
        add(Stage.PEAK,   "Peak",    "巅峰");
    }

    private void addApertureStatus() {
        add(ApertureStatus.NORMAL, "Normal", "正常");
        add(ApertureStatus.DEAD,   "Dead",   "死窍");
    }

    private void addTalent() {
        add(Talent.TEN_EXTREMES, "Ten-Extremes Aptitude", "十绝天资");
        add(Talent.FIRST,        "Grade-A Aptitude",      "甲等资质");
        add(Talent.SECOND,       "Grade-B Aptitude",      "乙等资质");
        add(Talent.THIRD,        "Grade-C Aptitude",      "丙等资质");
        add(Talent.FOURTH,       "Grade-D Aptitude",      "丁等资质");
        add(Talent.NONE,         "Unawakened",            "未觉醒");
    }

    private void addPhysique() {
        add(Physique.ZOMBIE,      "Zombie",      "僵尸");
        add(Physique.TEMP_ZOMBIE, "Temp Zombie", "临时僵尸化");
        add(Physique.EXTREME,     "Extreme",     "十绝体");
    }

    private void addRace() {
        add(Race.HUMAN,       "Human",       "人族");
        add(Race.HAIRY_MEN,   "Hairy Men",   "毛民");
        add(Race.EGGMEN,      "Eggmen",      "蛋人");
        add(Race.ROCKMEN,     "Rockmen",     "石人");
        add(Race.FEATHERMEN,  "Feathermen",  "羽民");
        add(Race.INKMEN,      "Inkmen",      "墨人");
        add(Race.MINIMEN,     "Minimen",     "小人");
        add(Race.MERMEN,      "Mermen",      "鲛人");
        add(Race.BEASTMEN,    "Beastmen",    "兽人");
        add(Race.DRAGONMEN,   "Dragonmen",   "龙人");
        add(Race.MUSHROOMMEN, "Mushroommen", "菇人");
        add(Race.SNOWMEN,     "Snowmen",     "雪人");
    }

    private void addEssenceColor() {
        add(EssenceColor.NONE,           "None",           "无");
        add(EssenceColor.GREEN_COPPER,   "Green Copper",   "青铜色");
        add(EssenceColor.RED_STEEL,      "Red Steel",      "赤铁色");
        add(EssenceColor.WHITE_SILVER,   "White Silver",   "白银色");
        add(EssenceColor.YELLOW_GOLDEN,  "Yellow Gold",    "黄金色");
        add(EssenceColor.PURPLE_CRYSTAL, "Purple Crystal", "紫晶色");
        add(EssenceColor.GREEN_GRAPE,    "Green Grape",    "青提");
        add(EssenceColor.RED_DATE,       "Red Date",       "红枣");
        add(EssenceColor.WHITE_LITCHI,   "White Litchi",   "白荔");
        add(EssenceColor.YELLOW_APRICOT, "Yellow Apricot", "黄杏");
    }

    private void addTenExtreme() {
        add(ExtremePhysique.NONE,                               "",                                   "");
        add(ExtremePhysique.VERDANT_GREAT_SUN,                  "Verdant Great Sun",                  "太日阳莽体");
        add(ExtremePhysique.DESOLATE_ANCIENT_MOON,              "Desolate Ancient Moon",              "古月阴荒体");
        add(ExtremePhysique.NORTHERN_DARK_ICE_SOUL,             "Northern Dark Ice Soul",             "北冥冰魄体");
        add(ExtremePhysique.BOUNDLESS_FOREST_SAMSARA,           "Boundless Forest Samsara",           "森海轮回体");
        add(ExtremePhysique.BLAZING_GLORY_LIGHTNING_BRILLIANCE, "Blazing Glory Lightning Brilliance", "炎煌雷泽体");
        add(ExtremePhysique.MYRIAD_GOLD_WONDROUS_ESSENCE,       "Myriad Gold Wondrous Essence",       "万金妙华体");
        add(ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL,        "Great Strength True Martial",        "大力真武体");
        add(ExtremePhysique.CAREFREE_WISDOM_HEART,              "Carefree Wisdom Heart",              "逍遥智心体");
        add(ExtremePhysique.PROFOUND_EARTH_ORIGIN,              "Profound Earth Origin",              "厚土元央体");
        add(ExtremePhysique.UNIVERSE_GREAT_DERIVATION,          "Universe Great Derivation",          "宇宙大衍体");
        add(ExtremePhysique.PURE_DREAM_REALITY_SEEKER,          "Pure Dream Reality Seeker",          "纯梦求真体");
    }

    private void addPathEffectCategory() {
        add(PathEffectCategory.BENEFICIAL, "Path Buffs",   "流派增益");
        add(PathEffectCategory.HARMFUL,    "Path Debuffs", "流派减益");
    }

    private void addEffectCarrier() {
        add(EffectCarrier.APERTURE, "Aperture", "空窍");
        add(EffectCarrier.BODY,     "Body",     "肉体");
        add(EffectCarrier.SOUL,     "Soul",     "魂魄");
        add(EffectCarrier.MIND,     "Mind",     "脑海");
    }

    private void addBeastStrengthFamily() {
        add(BeastStrengthFamily.BOAR, "Boar", "猪");
        add(BeastStrengthFamily.BEAR, "Bear", "熊");
    }

    private void addQiKind() {
        add(QiKind.SWORD,    "Sword Qi",    "剑气");
        add(QiKind.STRENGTH, "Strength Qi", "力气");
        add(QiKind.LIFE,     "Life Qi",     "生气");
        add(QiKind.ESSENCE,  "Essence Qi",  "元气");
        add(QiKind.DEATH,    "Death Qi",    "死气");
        add(QiKind.HUMAN,    "Human Qi",    "人气");
        add(QiKind.HEAVEN,   "Heaven Qi",   "天气");
        add(QiKind.EARTH,    "Earth Qi",    "地气");
    }

    private void addPath() {
        add(GuPath.HEAVEN,         "Heaven Path",         "天道");
        add(GuPath.RULE,           "Rule Path",           "律道");
        add(GuPath.SPACE,          "Space Path",          "宇道");
        add(GuPath.TIME,           "Time Path",           "宙道");
        add(GuPath.HUMAN,          "Human Path",          "人道");
        add(GuPath.METAL,          "Metal Path",          "金道");
        add(GuPath.WOOD,           "Wood Path",           "木道");
        add(GuPath.WATER,          "Water Path",          "水道");
        add(GuPath.FIRE,           "Fire Path",           "火道");
        add(GuPath.EARTH,          "Earth Path",          "土道");
        add(GuPath.ICE_SNOW,       "Ice-Snow Path",       "冰雪道");
        add(GuPath.LIGHTNING,      "Lightning Path",      "雷道");
        add(GuPath.CLOUD,          "Cloud Path",          "云道");
        add(GuPath.QI,             "Qi Path",             "气道");
        add(GuPath.SOUND,          "Sound Path",          "音道");
        add(GuPath.LIGHT,          "Light Path",          "光道");
        add(GuPath.DARK,           "Dark Path",           "暗道");
        add(GuPath.POISON,         "Poison Path",         "毒道");
        add(GuPath.STRENGTH,       "Strength Path",       "力道");
        add(GuPath.DREAM,          "Dream Path",          "梦道");
        add(GuPath.REFINEMENT,     "Refinement Path",     "炼道");
        add(GuPath.WISDOM,         "Wisdom Path",         "智道");
        add(GuPath.INFORMATION,    "Information Path",    "信道");
        add(GuPath.THEFT,          "Theft Path",          "偷道");
        add(GuPath.LUCK,           "Luck Path",           "运道");
        add(GuPath.KILLING,        "Killing Path",        "杀道");
        add(GuPath.BLOOD,          "Blood Path",          "血道");
        add(GuPath.SOUL,           "Soul Path",           "魂道");
        add(GuPath.ENSLAVEMENT,    "Enslavement Path",    "奴道");
        add(GuPath.FOOD,           "Food Path",           "食道");
        add(GuPath.FORMATION,      "Formation Path",      "阵道");
        add(GuPath.PAINTING,       "Painting Path",       "画道");
        add(GuPath.TRANSFORMATION, "Transformation Path", "变化道");
    }

    private void addAttainment() {
        add(GuAttainment.NONE,                      "None",                      "无");
        add(GuAttainment.ORDINARY,                  "Ordinary",                  "普通");
        add(GuAttainment.QUASI_MASTER,              "Quasi-Master",              "准大师");
        add(GuAttainment.MASTER,                    "Master",                    "大师");
        add(GuAttainment.QUASI_GRANDMASTER,         "Quasi-Grandmaster",         "准宗师");
        add(GuAttainment.GRANDMASTER,               "Grandmaster",               "宗师");
        add(GuAttainment.QUASI_GREAT_GRANDMASTER,   "Quasi-Great Grandmaster",   "准大宗师");
        add(GuAttainment.GREAT_GRANDMASTER,         "Great Grandmaster",         "大宗师");
        add(GuAttainment.QUASI_SUPREME_GRANDMASTER, "Quasi-Supreme Grandmaster", "准无上大宗师");
        add(GuAttainment.SUPREME_GRANDMASTER,       "Supreme Grandmaster",       "无上大宗师");
    }

    private void addSoulTier() {
        add(SoulTier.ONE,              "One-Person Soul",              "一人魂");
        add(SoulTier.TEN,              "Ten-Person Soul",              "十人魂");
        add(SoulTier.HUNDRED,          "Hundred-Person Soul",          "百人魂");
        add(SoulTier.THOUSAND,         "Thousand-Person Soul",         "千人魂");
        add(SoulTier.TEN_THOUSAND,     "Ten-Thousand-Person Soul",     "万人魂");
        add(SoulTier.HUNDRED_THOUSAND, "Hundred-Thousand-Person Soul", "十万人魂");
        add(SoulTier.MILLION,          "Million-Person Soul",          "百万人魂");
        add(SoulTier.TEN_MILLION,      "Ten-Million-Person Soul",      "千万人魂");
        add(SoulTier.HUNDRED_MILLION,  "Hundred-Million-Person Soul",  "亿人魂");
    }

    private void addWisdomType() {
        add(MindPoolType.THOUGHTS, "Thoughts", "念");
        add(MindPoolType.WILLS,    "Wills",    "意");
        add(MindPoolType.EMOTIONS, "Emotions", "情");
    }

    private void addBrilliance() {
        add(Brilliance.ORDINARY,    "Ordinary Brilliance",    "才情普通");
        add(Brilliance.DECENT,      "Decent Brilliance",      "才情尚可");
        add(Brilliance.DISTINCTIVE, "Distinctive Brilliance", "才情不俗");
        add(Brilliance.OUTSTANDING, "Outstanding Brilliance", "才情卓越");
        add(Brilliance.UNRIVALED,   "Unrivaled Brilliance",   "才情旷世");
    }

    private void addThoughtTag() {
        add(ThoughtTag.NATURAL, "Natural",   "自然念");
        add(ThoughtTag.EVIL,    "Malicious", "恶念");
    }
    //endregion
}
