package net.alex.guzhenren.datagen.lang;

import java.util.Locale;
import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.gameplay.aperture.ApertureStatus;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.aperture.Title;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Flesh;
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
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.jetbrains.annotations.Nullable;

/**
 * 三种语言的全部字符串，一张对齐表。
 *
 * <ul>
 *   <li>每行一个键、英文、简中；同一 provider 按 {@link Language} 各跑一次、只写自己那列，不会有语言缺键。</li>
 *   <li>⚠ 两种写法都权威，互不为译文；由一边推另一边，名字会悄悄变成从未有过的意思。</li>
 *   <li>方块、效果、实体、枚举常量经注册对象或常量添加，不写裸键、改名无悬空键；界面、指令、消息无对象，按键加。</li>
 *   <li>物品名字在 {@link ItemRegistrar} 注册链；{@link #addDeclaredItemKeys} 经同一列开关写到其 description id，繁体转
 *       换与重复键检查照样成立。</li>
 *   <li>BlockItem 与方块共用 description id：元泉[Spirit Spring]注册链一次命名方块、BlockItem、流体类型，钢块命名方块与
 *       BlockItem；仅无 BlockItem 的方块在 {@link #addItemKeys} 留 {@code addBlock} 行，纯水那行兼命名其流体类型。</li>
 *   <li>{@code zh_tw} 由简体列经 {@link TraditionalGlyphs} 转换，遇未知字形抛异常，{@code runData} 失败而不发布简体字；
 *       仅措辞不同（信息、資訊）才手写第四列繁体，只差字形的进字形表，一处改处处生效。</li>
 *   <li>值列按 {@code add*()} 方法对齐，过长条目两种写法折行；超 300 行、分区超 40 行有意：数据表一行一个字符串。</li>
 * </ul>
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

    private void addBlock(Supplier<? extends Block> block, String english, String simplified) {
        add(block.get().getDescriptionId(), english, simplified);
    }

    private void addDeclaredItemKeys() {
        for (ItemRegistrar.Entry entry : ItemRegistrar.of(ModItems.ITEMS).getEntries()) {
            add(entry.item().get().getDescriptionId(), entry.english(), entry.simplified(), entry.traditional());
        }
    }

    //region DISPLAY [显示]
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
        add("guzhenren.display.talent_grade.first",          "Grade A",                     "甲等");
        add("guzhenren.display.talent_grade.second",         "Grade B",                     "乙等");
        add("guzhenren.display.talent_grade.third",          "Grade C",                     "丙等");
        add("guzhenren.display.talent_grade.fourth",         "Grade D",                     "丁等");
        add("guzhenren.chat.awakened",                       "%s has awakened, aptitude %s", "%s已开窍，资质%s");
        add("guzhenren.chat.awakened_aptitude",              "%s, %s",                      "%s%s");
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
        add("guzhenren.display.strength.beast_reading",      "%s %s Strength",              "%s%s之力");
        add("guzhenren.display.strength.innate_reading",     "%s Innate Strength",          "%s本力");
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
        add("guzhenren.display.strength.jin_total",          " Total: %s Jin",              " 合计：%s 斤");
        add("guzhenren.display.strength.jin_reading",        "%s Jin Strength",             "%s斤之力");
        add("guzhenren.display.strength.jun_reading",        "%s Jun Strength",             "%s钧之力");

        add("guzhenren.hud.lifespan",                        "Lifespan %s",                 "寿元 %s");

        add("guzhenren.hud.refining",                        "Refine %s/%s",                "炼化 %s/%s");
        add("guzhenren.hud.refining_plain",                  "Refining",                    "炼化中");
        add("guzhenren.hud.using_plain",                     "Using",                       "使用中");
        add("guzhenren.hud.using",                           "Use %s/%s",                   "使用 %s/%s");
        add("guzhenren.hud.store_strength",                  "Charging",                    "蓄力中");
        add("guzhenren.hud.store_strength.full",             "Charged",                     "蓄满");
        add("guzhenren.hud.slider.cancel",                   "Cancel",                      "取消");
        add("guzhenren.hud.elder_gu.withdraw",               "Withdraw %s stacks (%s)",     "取出 %s 组（%s 枚）");
        add("guzhenren.hud.elder_gu.detail",
                "Stored %s · Bag fits %s stacks", "库存 %s · 背包可装 %s 组");
        add("guzhenren.hud.nourishing",                      "Nourishing %s%%",             "温养 %s%%");
        add("guzhenren.hud.nourish_starving",                "Essence running dry %s%%",    "真元将尽 %s%%");
        add("guzhenren.hud.shield",                          "%s %s / %s",                  "%s %s / %s");
        add("guzhenren.hud.aperture_pressure",               "Aperture Pressure %s%%",      "空窍压力 %s%%");
        add("guzhenren.hud.aperture_pressure_cd",            "Aperture Pressure %s%% [%s]", "空窍压力 %s%% [%s]");
    }
    //endregion

    //region DIMENSION [维度]
    private void addDimensionKeys() {
        add("guzhenren.dimension.treasure_yellow_heaven.inventory_full",
                "Bag full: items drop when you leave Treasure Yellow Heaven", "背包已满，物品将在离开宝黄天时掉落");
    }
    //endregion

    //region COMMAND [指令]
    private void addCommandKeys() {
        add("guzhenren.command.tagged",                           "[GZR] %s", "[GZR] %s");
        add("guzhenren.command.updated",                          "Updated %s player(s)", "已更新 %s 名玩家");
        add("guzhenren.command.unknown_value",                    "Unknown value: %s", "未知取值：%s");

        add("guzhenren.command.failed.awakened",                  "%s has already awakened", "%s 已开窍");
        add("guzhenren.command.failed.unawakened",                "%s has not awakened", "%s 尚未开窍");
        add("guzhenren.command.failed.mark_interference",         "%s: Mark Interference", "%s：力道道痕互斥");
        add("guzhenren.command.failed.aperture_index",            "%s has no aperture there", "%s 无此空窍");
        add("guzhenren.command.failed.base_extreme",
                "%s is unawakened or a Ten Extremes physique", "%s 未开窍或为十绝体");

        add("guzhenren.command.travel.entered",                   "Sent %s into %s", "已将 %s 送入%s");
        add("guzhenren.command.travel.exited",
                "Returned %s from the anchored dimension", "已将 %s 送出锚定维度");
        add("guzhenren.command.travel.already_inside",            "%s is already in %s", "%s 已在%s");
        add("guzhenren.command.travel.not_inside",
                "%s is not inside an anchored dimension", "%s 不在锚定维度内");
        add("guzhenren.command.travel.unknown_dimension",         "%s is not an anchored dimension", "%s 不是锚定维度");
        add("guzhenren.command.travel.missing_dimension",
                "%s is not loaded on this server", "本服务器未加载 %s", "本伺服器未載入 %s");
    }
    //endregion

    //region SCREEN [界面]
    private void addScreenKeys() {
        add("key.categories.guzhenren",                     "Guzhenren", "蛊真人");
        add("key.guzhenren.open_info",                      "Open Info Panel", "打开信息面板");

        add("guzhenren.screen.info.title",                  "Info", "信息");
        add("guzhenren.screen.tab.aperture",                "Aperture", "空窍");
        add("guzhenren.screen.tab.body",                    "Body", "肉身");
        add("guzhenren.screen.tab.soul",                    "Soul", "魂魄");
        add("guzhenren.screen.tab.paths",                   "Paths", "流派");
        add("guzhenren.screen.tab.mind",                    "Mind", "脑海");
        add("guzhenren.screen.tab.refinement",              "Refinement", "炼蛊");
        add("guzhenren.menu.aperture_storage",              "Aperture Storage", "空窍存储");
        add("guzhenren.menu.vital",                         "Vital", "本命");
        add("guzhenren.menu.refinement",                    "Gu Refinement", "炼蛊");
        add("guzhenren.menu.soul_trade.trade",              "Trade", "交易");
        add("guzhenren.menu.soul_trade.short",              "Not enough to pay for this trade", "交易材料不足");
        add("guzhenren.menu.refinement.craft",              "Refine", "炼制");
        add("guzhenren.menu.refinement.no_room",            "Output is full", "输出已满");
        add("guzhenren.menu.refinement.rings",
                "Outer ring: material  ·  Inner ring: Gu", "外圈 蛊材  ·  内圈 蛊虫");
        add("guzhenren.menu.refinement.window",
                "Window %s/%s  ·  %ss left  ·  stones %s/%s", "第 %s/%s 窗口  ·  余 %s 秒  ·  元石 %s/%s");
        add("guzhenren.menu.refinement.gap",                "Refining", "炼制中");
        add("guzhenren.menu.refinement.lost_stones",        "Failed: Not enough Primeval Stone", "元石不足，炼制失败");
        add("guzhenren.menu.refinement.lost_essence",       "Failed: Your essence ran dry", "真元耗尽，炼制失败");
        add("guzhenren.menu.refinement.lost_roll",          "The refinement failed", "炼制失败");
        add("guzhenren.menu.refinement.elder_spent",
                "The Primeval Elder Gu's stones are spent", "元老蛊用尽元石");
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
        add("guzhenren.menu.refinement.stopped",            "Refinement interrupted", "炼制中断");
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
        add("guzhenren.nourish.stage_up",                   "Aperture Stage Up", "小境界提升");
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
        add("guzhenren.screen.label.path_marks",             "Path Marks", "道痕");
        add("guzhenren.screen.label.effects",               "Effects", "效果");
        add("guzhenren.screen.label.strength_path_achieve", "Strength Path Attainment", "力道造诣");
        add("guzhenren.screen.label.time_path_achieve",     "Time Path Attainment", "宙道造诣");
        add("guzhenren.screen.label.time_rate_up",          "Time Rate Up", "自身时间加速");
        add("guzhenren.screen.label.body_capacity",         "Capacity", "承受");
        add("guzhenren.screen.label.attack",                "Base Attack", "基础攻击力");
        add("guzhenren.screen.label.aperture_pressure",     "Aperture Pressure", "空窍压力");
        add("guzhenren.screen.capacity",                    "%s / %s pts, carries %s (%s at rest)",
                "%s / %s 点，承受 %s（常态 %s）");
        add("guzhenren.screen.capacity.body_overload",      ", overload %s", "，过载 %s");
        add("guzhenren.menu.aperture_load",                 "Load %s / %s", "空窍负担 %s / %s");
        add("guzhenren.screen.label.brilliance",            "Brilliance", "才情");
    }
    //endregion

    //region ITEM [物品]
    private void addItemKeys() {
        addBlock(ModBlocks.PURE_WATER,                               "Pure Water", "纯水");
        addBlock(ModBlocks.NINE_LEAF_VITALITY_GRASS_3,               "Nine Leaf Vitality Grass III", "三转九叶生机草");
        addBlock(ModBlocks.INTIMATE_GRASS,                           "Intimate Grass", "知心草");

        add("itemGroup.guzhenren.mortal_gu",                         "Mortal Gu", "凡蛊");
        add("itemGroup.guzhenren.gu_material",                       "Gu Material", "蛊材");
        add("itemGroup.guzhenren.strength_mortal_gu",                "Strength Mortal Gu", "力道凡蛊");
        add("itemGroup.guzhenren.wood_mortal_gu",                    "Wood Mortal Gu", "木道凡蛊");
        add("itemGroup.guzhenren.km_mortal",                         "Mortal Killing Moves", "凡道杀招");
        add("itemGroup.guzhenren.booklet",                           "Booklets", "书册");

        add("guzhenren.item.failed.awakened",                        "You have already awakened", "你已开窍");
        add("guzhenren.item.failed.awakening",                       "Awakening in progress", "开窍中");
        add("guzhenren.item.failed.nourishing",                      "Nourishing your aperture", "温养空窍中");
        add("guzhenren.item.failed.awakening_danger",                "Danger nearby, cannot awaken", "周围危险，无法开窍");
        add(Refusal.UNAWAKENED,                                      "Not yet awakened", "尚未开窍");
        add("guzhenren.item.failed.essence_full",                    "Essence is already FULL", "真元已满");
        add(Refusal.RANK_MISMATCH,                                   "Wrong realm", "境界不符");
        add("guzhenren.item.failed.stage_peak",                      "Already at %s Peak", "已至%s巅峰");
        add(Refusal.CANNOT_USE,                                      "Cannot use this Gu", "无法使用该蛊");
        add(Refusal.EFFECT_ACTIVE,                                   "This Gu's effect has not ended", "该蛊效果未结束");
        add("guzhenren.item.failed.beast_strength_held",             "Already holding %s", "已有%s");
        add("guzhenren.item.failed.beast_strength_capacity",         "Not enough capacity", "承受力不足");
        add("guzhenren.item.failed.human_strength_full",             "Already at %s layers", "已叠满%s层");
        add("guzhenren.item.failed.mark_interference",               "Mark Interference", "力道道痕互斥");
        add(Refusal.AUTO_EFFECT,                                     "This Gu works on its own", "该蛊自动生效");
        add(Refusal.ESSENCE,                                         "NOT enough essence", "真元不足");
        add("guzhenren.item.failed.elder_gu_empty",
                "No Primeval Stones stored in the Primeval Elder Gu", "元老蛊无元石储备");
        add("guzhenren.item.failed.elder_gu_full",                   "The Primeval Elder Gu's store is full", "元老蛊元石储备已满");
        add(Refusal.BAG_FULL,                                        "Your bag is full", "背包已满");
        add("guzhenren.item.failed.elder_gu_no_stones",
                "No Primeval Stones to store in the Primeval Elder Gu", "无元石存入元老蛊");
        add("guzhenren.item.failed.gu_cooldown",                     "Cooling down %s s", "冷却中 %s 秒");
        add("guzhenren.item.failed.all_out_needs_branch",
                "Must be paired with other Gu", "需搭配蛊虫使用");
        add("guzhenren.item.failed.tempering_taken",                 "Already %s", "已是%s");
        add(Refusal.GU_HUNGRY,                                       "Gu is hungry", "蛊虫饥饿");
        add("guzhenren.item.failed.forced_use_warning",              "Gu is hungry; forcing it kills it", "蛊虫饥饿，强行催动会死");
        add("guzhenren.item.failed.vital_no_forced_use",             "A Vital Gu cannot be forced", "本命蛊无法强行催动");
        add("guzhenren.item.failed.amplify_needs_hold",              "Hold to choose an amplify target", "需要长按选择增幅目标");
        add("guzhenren.item.failed.amplify_no_target",               "No amplify target on the hotbar", "快捷栏无可增幅目标");
        add("guzhenren.item.failed.no_leaves",                       "No Vitality Leaf yet", "暂无生机叶");
        add("guzhenren.item.failed.as_before_no_record",             "No past record", "无过去记录");
        add(Refusal.WRONG_TARGET,                                    "Wrong target", "作用目标错误");
        add("guzhenren.item.failed.as_before_no_grief",
                "Terrain changes are off in this world - mobGriefing", "本世界关闭了地形改动 - mobGriefing");
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
        add("guzhenren.item.gu.little_light_bright",                 "Shining bright", "强光中");
        add("guzhenren.item.gu.killing_move_hint",
                "Hold right-click to pick a killing move", "长按右键选杀招");
        add("guzhenren.killing_move.km_mortal_little_light_amplify", "Little Light Amplification", "小光蛊增幅");
        add("guzhenren.item.killing_move.learned",                   "Killing move learned: %s", "习得杀招：%s");
        add("guzhenren.item.failed.killing_move_known",              "Killing move already learned: %s", "已习得杀招：%s");
        add("guzhenren.item.killing_move.mains",                     "Main Gu: %s", "主蛊：%s");
        add("guzhenren.item.killing_move.partner",                   "Partner Gu: %s", "配合蛊：%s");
        add("guzhenren.item.killing_move.blade_factor",              "Moon blade damage ×%s", "月刃伤害 ×%s");
        add("guzhenren.item.killing_move.known",                     "Learned", "已学会");
        add("guzhenren.killing_move.started",                        "Killing move: %s", "杀招：%s");
        add("guzhenren.killing_move.stopped",                        "Killing move off: %s", "杀招关闭：%s");
        add("guzhenren.killing_move.stop",                           "Stop %s", "关闭%s");
        add("guzhenren.item.failed.killing_move_partner_missing",
                "Missing the killing move's key Gu: %s", "缺少杀招关键蛊虫：%s");
        add("guzhenren.item.gu.lifespan_gained",                     "Lifespan +%s years", "寿元 +%s 年");
        add("guzhenren.item.gu.hungry",                              "%s is hungry", "%s饥饿");
        add("guzhenren.item.gu.starved",                             "%s starved", "%s饿死");
        add("guzhenren.item.gu.forced_dead",                         "%s died from forced use", "%s强行催动而亡");
        add("guzhenren.item.gu.refine_dead",                         "%s died in refinement", "%s炼制中死亡");
        add("guzhenren.item.gu.health",                              "Health %s / %s", "生命 %s / %s");
        add("guzhenren.item.gu.vital_lost",
                "Your Vital Gu %s died", "本命蛊%s死亡");

        add("effect.guzhenren.vitality_leaf_gu",                     "Vitality Leaf Gu Effect", "生机叶蛊效果");
        add("effect.guzhenren.liquor_worm",                          "Liquor Worm Effect", "酒虫效果");
        add("effect.guzhenren.flower_boar_gu",                       "Flower Boar Gu Effect", "花豕蛊效果");
        add("effect.guzhenren.brute_force_longhorn_beetle_gu",
                "Brute Force Longhorn Beetle Gu Effect", "蛮力天牛蛊效果");
        add("effect.guzhenren.strength_qi_gu",                       "Strength Qi Gu Effect", "力气蛊效果");
        add("effect.guzhenren.horizontal_crash_gu",                  "Horizontal Crash Gu Effect", "横冲蛊效果");
        add("effect.guzhenren.vertical_crash_gu",                    "Vertical Crash Gu Effect", "直撞蛊效果");
        add("effect.guzhenren.charging_crash_gu",                    "Charging Crash Gu Effect", "横冲直撞蛊效果");
        add("effect.guzhenren.self_reliance_gu",                     "Self Reliance Gu Effect", "自力更生蛊效果");
        add("effect.guzhenren.hardship_strength_gu",                 "Hardship Strength Gu Effect", "苦力蛊效果");
        add("effect.guzhenren.jade_skin_gu",                         "Jade Skin Gu Effect", "玉皮蛊效果");
        add("effect.guzhenren.white_jade_gu",                        "White Jade Gu Effect", "白玉蛊效果");
        add("effect.guzhenren.heavenly_mugwort_gu",                  "Heavenly Mugwort Gu Effect", "天蓬蛊效果");
        add("effect.guzhenren.all_out_effort_gu",                    "All-Out Effort Gu Effect", "全力以赴蛊效果");
        add("effect.guzhenren.store_strength_gu",                    "Store Strength Gu Effect", "蓄力蛊效果");
        add("effect.guzhenren.temp_zombie",                          "Temp Zombie Effect", "临时僵尸化效果");
        add("effect.guzhenren.second_watch_gu",                      "Second Watch Gu Effect", "两更蛊效果");
        add("effect.guzhenren.third_watch_gu",                       "Third Watch Gu Effect", "三更蛊效果");
        add("effect.guzhenren.malicious_thought_gu",                 "Malicious Thought Gu Effect", "恶念蛊效果");
        add("effect.guzhenren.casual_gu",                            "Casual Gu Effect", "随意蛊效果");
        add("effect.guzhenren.bleeding",                             "Bleeding", "流血");
    }
    //endregion

    //region ENTITY [实体]
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
        add("entity.guzhenren.tortoise",                   "Tortoise",               "陆龟");
        add("entity.guzhenren.stone_tortoise",             "Stone Tortoise",         "石龟");
        add("entity.guzhenren.yellow_ox",                  "Yellow Ox",              "黄牛");
        add("entity.guzhenren.water_buffalo",              "Water Buffalo",          "水牛");
        add("entity.guzhenren.azure_ox",                   "Azure Ox",               "青牛");
        add("entity.guzhenren.elephant",                   "Elephant",               "大象");
        add("entity.guzhenren.white_elephant",             "White Elephant",         "白象");
        add("entity.guzhenren.beast_phantom",              "Beast Phantom",          "兽力虚影");
        add("entity.guzhenren.horizontal_crash_gu_entity", "Horizontal Crash Gu",    "横冲蛊");
        add("entity.guzhenren.vertical_crash_gu_entity",   "Vertical Crash Gu",      "直撞蛊");
        add("entity.guzhenren.charging_crash_gu_4_entity", "Charging Crash Gu IV",   "四转横冲直撞蛊");
        add("entity.guzhenren.charging_crash_gu_5_entity", "Charging Crash Gu V",    "五转横冲直撞蛊");
        add("entity.guzhenren.test_trade_gu_immortal",     "Test Trade Gu Immortal", "测试交易蛊仙");
        add("entity.guzhenren.moon_blade",                 "Moon Blade",             "月刃");
        add("entity.guzhenren.qi_wave",                    "Qi Wave",                "气波");
        add("entity.guzhenren.beast_charge",               "Beast Charge",           "兽力冲击");
    }
    //endregion

    //region ADVANCEMENT [进度]
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

    //region DEATH [死亡]
    private void addDeathMessages() {
        add("death.attack.guzhenren.lifespan_exhausted",          "%1$s ran out of lifespan", "%1$s 寿元耗尽而亡");
        add("death.attack.guzhenren.soul_collapse",               "%1$s suffered soul collapse", "%1$s 魂魄衰竭而亡");
        add("death.attack.guzhenren.mind_ocean_shattered",        "%1$s shattered their Mind Ocean", "%1$s 脑海炸裂而亡");
        add("death.attack.guzhenren.aperture_pressure_explosion",
                "%1$s died in an aperture pressure explosion", "%1$s 空窍压力爆炸而亡");
        add("death.attack.guzhenren.ten_extreme_disaster",
                "%1$s was killed by a Ten-Extremes disaster", "%1$s 被十绝天灾波及而亡");
        add("death.attack.guzhenren.vital_gu_lost",               "%1$s lost their Vital Gu", "%1$s 因本命蛊死亡而亡");
        add("death.attack.guzhenren.moon_blade",
                "%1$s was slain by %2$s's Moon Blade", "%1$s 被 %2$s 的月刃斩杀");
        add("death.attack.guzhenren.moon_blade.item",
                "%1$s was slain by %2$s's Moon Blade using %3$s", "%1$s 被 %2$s 用 %3$s 的月刃斩杀");
        add("death.attack.guzhenren.bleeding",                    "%1$s bled to death", "%1$s 流血而亡");
        add("death.attack.guzhenren.bleeding.player",
                "%1$s bled to death while fighting %2$s", "%1$s 在与 %2$s 战斗时流血而亡");
        add("death.attack.guzhenren.area_impact",
                "%1$s was struck down by %2$s's Area Impact", "%1$s 被 %2$s 的范围冲击打死了");
        add("death.attack.guzhenren.area_impact.item",
                "%1$s was struck down by %2$s's Area Impact using %3$s", "%1$s 被 %2$s 用 %3$s 的范围冲击打死了");
        add("death.attack.guzhenren.qi_wave",
                "%1$s was struck down by %2$s's Qi Wave", "%1$s 被 %2$s 的气波打死了");
        add("death.attack.guzhenren.qi_wave.item",
                "%1$s was struck down by %2$s's Qi Wave using %3$s", "%1$s 被 %2$s 用 %3$s 的气波打死了");
        add("death.attack.guzhenren.body_overload",               "%1$s died of Body Overload", "%1$s 肉体过载而亡");
        add("death.attack.guzhenren.charged_punch",
                "%1$s was struck down by %2$s's Charged Punch", "%1$s 被 %2$s 的蓄力一拳打死了");
        add("death.attack.guzhenren.charged_punch.item",
                "%1$s was struck down by %2$s's Charged Punch using %3$s", "%1$s 被 %2$s 用 %3$s 的蓄力一拳打死了");
        add("death.attack.guzhenren.impact_backlash",             "%1$s died of Impact Backlash", "%1$s 冲击反噬而亡");
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
        add(GuType.PATH_MARK_CARVING, "Path Mark Carving", "道痕蚀刻");
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

    private void addShield() {
        add(ShieldType.HEAVENLY_MUGWORT, "Heavenly Mugwort", "天蓬");
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
        add(BeastStrength.TIGER,      "Tiger",      "虎");
        add(BeastStrength.STONE_TURTLE,    "Stone Turtle",    "石龟");
        add(BeastStrength.GALLOPING_HORSE, "Galloping Horse", "驰马");
        add(BeastStrength.GREEN_BULL,      "Green Bull",      "青牛");
        add(BeastStrength.BROWN_BEAR,      "Brown Bear",      "棕熊");
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
        add(HumanStrength.HUNDRED_JUN, "Hundred Jun", "百钧");
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
        add(BeastStrengthFamily.OX,   "Ox",   "牛");
        add(BeastStrengthFamily.BEAR, "Bear", "熊");
        add(BeastStrengthFamily.TIGER, "Tiger", "虎");
        add(BeastStrengthFamily.TURTLE, "Turtle", "龟");
        add(BeastStrengthFamily.HORSE, "Horse", "马");
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
        add(GuPath.MOON,           "Moon Path",           "月道");
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
