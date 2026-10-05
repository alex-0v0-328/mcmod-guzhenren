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
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
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
 * The English strings, written as an aligned table.
 *
 * <p>Extends {@link net.neoforged.neoforge.common.data.LanguageProvider} for {@code en_us}. Every
 * entry takes the registered object or the enum constant, never a raw key string, so a renamed
 * registration cannot leave a key behind pointing at nothing. The value column aligns per
 * {@code add*()} method to that method's longest key.
 *
 * <p>In {@link #addItemKeys}, one {@code addBlock} entry names the Spirit Spring block, its
 * BlockItem and the fluid type together, since they share one description id.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.datagen.lang.ZhCnLanguageProvider
 * @since 1.0.0
 */

public class EnUsLanguageProvider extends LanguageProvider {

    public EnUsLanguageProvider(PackOutput output) {
        super(output, Guzhenren.MOD_ID, "en_us");
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
        add("guzhenren.display.realm", "%s %s");
        add("guzhenren.display.realm_title", "%s %s");
        add("guzhenren.display.aptitude_line", "%s [%s]");
        add("guzhenren.display.gu_line", "%s %s %s");
        add("guzhenren.display.gu", "Gu");
        add("guzhenren.display.gu_material", "Gu Material");
        add("guzhenren.display.aperture_1", "First Aperture");
        add("guzhenren.display.aperture_2", "Second Aperture");
        add("guzhenren.display.physique", "[%s]");
        add("guzhenren.display.physique_line", "Physique: %s");
        add("guzhenren.display.lifespan", "%s [age %s]");
        add("guzhenren.display.base_fraction", "%s %s");
        add("guzhenren.display.base_round", "%s");
        add("guzhenren.display.base_full", "One Hundred");
        add("guzhenren.display.base_tens.2", "Twenty");
        add("guzhenren.display.base_tens.3", "Thirty");
        add("guzhenren.display.base_tens.4", "Forty");
        add("guzhenren.display.base_tens.5", "Fifty");
        add("guzhenren.display.base_tens.6", "Sixty");
        add("guzhenren.display.base_tens.7", "Seventy");
        add("guzhenren.display.base_tens.8", "Eighty");
        add("guzhenren.display.base_tens.9", "Ninety");
        add("guzhenren.display.base_units.1", "One");
        add("guzhenren.display.base_units.2", "Two");
        add("guzhenren.display.base_units.3", "Three");
        add("guzhenren.display.base_units.4", "Four");
        add("guzhenren.display.base_units.5", "Five");
        add("guzhenren.display.base_units.6", "Six");
        add("guzhenren.display.base_units.7", "Seven");
        add("guzhenren.display.base_units.8", "Eight");
        add("guzhenren.display.base_units.9", "Nine");
        add("guzhenren.display.none", "[NONE]");
        add("guzhenren.display.time_rate_up", "x%s");
        add("guzhenren.display.wild", "Wild %s");
        add("guzhenren.display.vital", "Vital %s");
        add("guzhenren.display.strength.beast_reading", "[%s %s Strength]");
        add("guzhenren.display.strength.beast_number.1", "One");
        add("guzhenren.display.strength.beast_number.2", "Two");
        add("guzhenren.display.strength.beast_number.10", "Ten");
        add("guzhenren.display.strength.beast_number.100", "Hundred");
        add("guzhenren.display.strength.beast_number.1000", "Thousand");
        add("guzhenren.display.strength.beast_number.10000", "Ten Thousand");
        add("guzhenren.display.strength.num_join", "%s %s");
        add("guzhenren.display.strength.num_join_zero", "%s %s");
        add("guzhenren.display.strength.num_hundreds.1", "One Hundred");
        add("guzhenren.display.strength.num_hundreds.2", "Two Hundred");
        add("guzhenren.display.strength.num_hundreds.3", "Three Hundred");
        add("guzhenren.display.strength.num_tens_led.1", "Ten");
        add("guzhenren.display.strength.num_tens.1", "Ten");
        add("guzhenren.display.strength.num_tens.2", "Twenty");
        add("guzhenren.display.strength.num_tens.3", "Thirty");
        add("guzhenren.display.strength.num_tens.4", "Forty");
        add("guzhenren.display.strength.num_tens.5", "Fifty");
        add("guzhenren.display.strength.num_tens.6", "Sixty");
        add("guzhenren.display.strength.num_tens.7", "Seventy");
        add("guzhenren.display.strength.num_tens.8", "Eighty");
        add("guzhenren.display.strength.num_tens.9", "Ninety");
        add("guzhenren.display.strength.num_units.1", "One");
        add("guzhenren.display.strength.num_units.2", "Two");
        add("guzhenren.display.strength.num_units.3", "Three");
        add("guzhenren.display.strength.num_units.4", "Four");
        add("guzhenren.display.strength.num_units.5", "Five");
        add("guzhenren.display.strength.num_units.6", "Six");
        add("guzhenren.display.strength.num_units.7", "Seven");
        add("guzhenren.display.strength.num_units.8", "Eight");
        add("guzhenren.display.strength.num_units.9", "Nine");
        add("guzhenren.display.strength.jin_total", "[%s Jin]");
        add("guzhenren.display.strength.jin_reading", "[%s Jin Strength]");
        add("guzhenren.display.strength.jun_reading", "[%s Jun Strength]");

        add("guzhenren.hud.lifespan", "Lifespan %s");
        add("guzhenren.display.tenth.2", "20%%");
        add("guzhenren.display.tenth.3", "30%%");
        add("guzhenren.display.tenth.4", "40%%");
        add("guzhenren.display.tenth.5", "50%%");
        add("guzhenren.display.tenth.6", "60%%");
        add("guzhenren.display.tenth.7", "70%%");
        add("guzhenren.display.tenth.8", "80%%");
        add("guzhenren.display.tenth.9", "90%%");
        add("guzhenren.display.tenth.10", "100%%");

        add("guzhenren.hud.refining", "Refine %s/%s");
        add("guzhenren.hud.refining_plain", "Refining");
        add("guzhenren.hud.using_plain", "Using");
        add("guzhenren.hud.using", "Use %s/%s");
        add("guzhenren.hud.nourishing", "Nourishing %s%%");
        add("guzhenren.hud.nourish_starving", "Essence running dry %s%%");
        add("guzhenren.hud.aperture_pressure", "Aperture Pressure %s%%");
        add("guzhenren.hud.aperture_pressure_cd", "Aperture Pressure %s%% [%s]");
    }
    //endregion

    //region DIMENSION
    private void addDimensionKeys() {
        add("guzhenren.dimension.treasure_yellow_heaven.inventory_full",
                "Inventory full: what does not fit floats beside you");
    }
    //endregion

    //region COMMAND
    private void addCommandKeys() {
        add("guzhenren.command.header", "[GZR]");
        add("guzhenren.command.tagged", "[GZR] %s");
        add("guzhenren.command.updated", "Updated %s player(s)");
        add("guzhenren.command.unknown_value", "Unknown value: %s");

        add("guzhenren.command.failed.awakened", "%s has already awakened");
        add("guzhenren.command.failed.unawakened", "%s has not awakened");
        add("guzhenren.command.failed.aperture_index", "%s has no aperture there");
        add("guzhenren.command.failed.extreme_physique_required", "%s must use a concrete Extreme physique");

        add("guzhenren.command.info.realm", "Realm: %s");
        add("guzhenren.command.info.aperture_status", "Aperture Status: %s");
        add("guzhenren.command.info.talent", "Aptitude:    %s");
        add("guzhenren.command.info.essence", "Essence:     %s / %s");
        add("guzhenren.command.info.distilled", "Distilled:   %s / %s");
        add("guzhenren.command.info.pressure", "Pressure:    %s / %s%%");
        add("guzhenren.command.info.primary_path", "Primary:     %s");
        add("guzhenren.command.info.secondary_path", "Secondary:   %s");
        add("guzhenren.command.info.soul", "Soul:        %s / %s");
        add("guzhenren.command.info.lifespan", "Lifespan:    %s");
        add("guzhenren.command.info.physique", "%s");
        add("guzhenren.command.info.race", "Race:        %s");
        add("guzhenren.command.info.wisdom_path_achieve", "Wisdom Path Attainment:");
        add("guzhenren.command.info.wisdom_path_achieve_entry", "  %s  %s");
        add("guzhenren.command.info.qi_path_achieve", "Qi Path Attainment:");
        add("guzhenren.command.info.qi_path_achieve_entry", "  %s  %s");
        add("guzhenren.command.info.paths", "Paths:");
        add("guzhenren.command.info.strength_path_achieve", "Strength Path Attainment:");
        add("guzhenren.command.info.strength_path_achieve_entry", "  %s  %s");
        add("guzhenren.command.info.time_path_achieve", "Time Path Attainment:");
        add("guzhenren.command.info.time_rate_up_entry", "  Time Rate Up  %s");
        add("guzhenren.command.info.capacity", "Bearing:     %s / %s jin");
        add("guzhenren.command.info.attack", "Base Attack: %s");
        add("guzhenren.command.info.brilliance", "Brilliance:  %s");
        add("guzhenren.command.info.brilliance_rate", "%s thoughts/s");
        add("guzhenren.command.info.mind", "Mind Ocean:");
        add("guzhenren.command.info.mind_entry", "  %s  %s / %s");

        add("guzhenren.command.info.detail", " [%s]");

        add("guzhenren.command.travel.entered", "Sent %s into %s");
        add("guzhenren.command.travel.exited", "Returned %s from the anchored dimension");
        add("guzhenren.command.travel.already_inside", "%s is already inside %s");
        add("guzhenren.command.travel.not_inside", "%s is not inside an anchored dimension");
        add("guzhenren.command.travel.unknown_dimension", "%s is not an anchored dimension");
    }
    //endregion

    //region SCREEN
    private void addScreenKeys() {
        add("key.categories.guzhenren", "Guzhenren");
        add("key.guzhenren.open_info", "Open Info Panel");

        add("guzhenren.screen.info.title", "Info");
        add("guzhenren.screen.tab.aperture", "Aperture");
        add("guzhenren.screen.tab.body", "Body");
        add("guzhenren.screen.tab.soul", "Soul");
        add("guzhenren.screen.tab.path", "Path Attainment");
        add("guzhenren.screen.tab.mind", "Mind");
        add("guzhenren.screen.tab.refinement", "Refinement");
        add("guzhenren.menu.aperture_storage", "Aperture Storage");
        add("guzhenren.menu.vital", "Vital");
        add("guzhenren.menu.refinement", "Gu Refinement");
        add("guzhenren.menu.soul_trade.trade", "Trade");
        add("guzhenren.menu.soul_trade.short", "Not enough to pay for this trade");
        add("guzhenren.menu.soul_trade.no_room", "Not enough inventory space: make room before trading");
        add("guzhenren.menu.refinement.essence", "Refining needs %s essence");
        add("guzhenren.menu.refinement.craft", "Refine");
        add("guzhenren.menu.refinement.no_room", "No output space");
        add("guzhenren.menu.refinement.not_awakened", "No aperture for refining");
        add("guzhenren.menu.refinement.rings", "Outer ring: material  ·  Inner ring: Gu");
        add("guzhenren.menu.refinement.window", "Window %s/%s  ·  %ss left  ·  stones %s/%s");
        add("guzhenren.menu.refinement.gap", "Refining");
        add("guzhenren.menu.refinement.lost_stones", "Failed: Not enough Primeval Stone");
        add("guzhenren.menu.refinement.lost_essence", "Failed: Your essence ran dry");
        add("guzhenren.menu.refinement.lost_roll", "The refinement failed");
        add("guzhenren.menu.refinement.elder_spent", "The Primeval Elder Gu is spent, and is back in your bag");
        add("guzhenren.menu.refinement.recipes", "Recipes");
        add("guzhenren.menu.refinement.selected", "Chosen  %s");
        add("guzhenren.menu.refinement.extra", "The grid holds more than this recipe wants");
        add("guzhenren.menu.refinement.pick.title", "Choose a Gu Recipe");
        add("guzhenren.menu.refinement.pick.auto", "Auto choose");
        add("guzhenren.menu.refinement.pick.empty", "You know no Gu Recipe");
        add("guzhenren.menu.refinement.pick.needs", "Needs");
        add("guzhenren.menu.refinement.pick.item", "  %s × %s");
        add("guzhenren.menu.refinement.pick.windows", "%s windows  ·  %ss");
        add("guzhenren.menu.refinement.pick.stones", "Stones a window  %s");
        add("guzhenren.menu.refinement.pick.cost", "%s essence a second");
        add("guzhenren.menu.refinement.pick.chance", "%s%%");
        add("guzhenren.menu.refinement.pick.success", "Success  %s%%");
        add("guzhenren.menu.refinement.stop", "Stop");
        add("guzhenren.menu.refinement.stopped", "You broke off the refinement");
        add("guzhenren.menu.refinement.pool", "%s / %s");
        add("guzhenren.menu.refinement.pick.soul", "%s soul a second");
        add("guzhenren.screen.label.primary_path", "Primary Path");
        add("guzhenren.screen.label.secondary_path", "Secondary Path");
        add("guzhenren.screen.pick.title", "Choose a Secondary Path");
        add("guzhenren.screen.pick.hint", "click to set");
        add("guzhenren.screen.nourish", "Nourish Aperture");
        add("guzhenren.screen.nourish_stop", "Stop Nourishing");
        add("guzhenren.screen.nourish_second", "Nourish Second Aperture");
        add("guzhenren.screen.impact", "Flush Aperture Wall");
        add("guzhenren.screen.button.storage", "Aperture Storage");
        add("guzhenren.nourish.starved", "The essence ran out");
        add("guzhenren.nourish.stage_up", "Aperture Stage Up");
        add("guzhenren.impact.poor", "Flushing the wall needs %s essence");
        add("guzhenren.impact.success", "Aperture Rank Up");
        add("guzhenren.impact.hold", "Flush Failed, No change");
        add("guzhenren.impact.drop_stage", "Flush Failed, Stage dropped");
        add("guzhenren.impact.drop_base", "Flush Failed, Aptitude dropped");
        add("guzhenren.screen.label.realm", "Realm");
        add("guzhenren.screen.label.aperture_status", "Aperture Status");
        add("guzhenren.screen.label.talent", "Aptitude");
        add("guzhenren.screen.label.essence", "Essence");
        add("guzhenren.screen.label.distilled", "Distilled");
        add("guzhenren.screen.label.physique", "Physique");
        add("guzhenren.screen.label.race", "Race");
        add("guzhenren.screen.label.wisdom_path_achieve", "Wisdom Path Attainment");
        add("guzhenren.screen.label.soul", "Soul");
        add("guzhenren.screen.label.lifespan", "Lifespan");
        add("guzhenren.screen.label.qi_path_achieve", "Qi Path Attainment");
        add("guzhenren.screen.label.paths", "Paths");
        add("guzhenren.screen.label.strength_path_achieve", "Strength Path Attainment");
        add("guzhenren.screen.label.time_path_achieve", "Time Path Attainment");
        add("guzhenren.screen.label.time_rate_up", "Time Rate Up");
        add("guzhenren.screen.label.body_capacity", "Capacity");
        add("guzhenren.screen.label.attack", "Base Attack");
        add("guzhenren.screen.label.aperture_pressure", "Aperture Pressure");
        add("guzhenren.screen.capacity", "%s / %s jin");
        add("guzhenren.menu.aperture_load", "Load %s / %s");
        add("guzhenren.screen.label.brilliance", "Brilliance");
        add("guzhenren.display.path_marks", " Marks %s");
    }
    //endregion

    //region ITEM
    private void addItemKeys() {
        addItem(ModItems.HOPE_GU, "Hope Gu");
        addItem(ModItems.COPPER_RELICS_GU, "Green Copper Relics Gu");
        addItem(ModItems.STEEL_RELICS_GU, "Red Steel Relics Gu");
        addItem(ModItems.SILVER_RELICS_GU, "White Silver Relics Gu");
        addItem(ModItems.GOLD_RELICS_GU, "Yellow Gold Relics Gu");
        addItem(ModItems.CRYSTAL_RELICS_GU, "Purple Crystal Relics Gu");
        addItem(ModItems.WHITE_BOAR_GU, "White Boar Gu");
        addItem(ModItems.BLACK_BOAR_GU, "Black Boar Gu");
        addItem(ModItems.FLOWER_BOAR_GU, "Flower Boar Gu");
        addItem(ModItems.BEAR_STRENGTH_GU, "Bear Strength Gu");
        addItem(ModItems.DRAGONPILL_CRICKET_GU, "Dragonpill Cricket Gu");
        addItem(ModItems.BRUTE_FORCE_LONGHORN_BEETLE_GU, "Brute Force Longhorn Beetle Gu");
        addItem(ModItems.HORIZONTAL_CRASH_GU, "Horizontal Crash Gu");
        addItem(ModItems.VERTICAL_CRASH_GU, "Vertical Crash Gu");
        addItem(ModItems.CHARGING_CRASH_GU_4, "Charging Crash Gu IV");
        addItem(ModItems.CHARGING_CRASH_GU_5, "Charging Crash Gu V");
        addItem(ModItems.SELF_RELIANCE_GU_2, "Self Reliance Gu II");
        addItem(ModItems.SELF_RELIANCE_GU_3, "Self Reliance Gu III");
        addItem(ModItems.SELF_RELIANCE_GU_4, "Self Reliance Gu IV");
        addItem(ModItems.HARDSHIP_STRENGTH_GU, "Hardship Strength Gu");
        addItem(ModItems.ALL_OUT_EFFORT_GU_3, "All-Out Effort Gu III");
        addItem(ModItems.ALL_OUT_EFFORT_GU_4, "All-Out Effort Gu IV");
        addItem(ModItems.ALL_OUT_EFFORT_GU_5, "All-Out Effort Gu V");
        addItem(ModItems.JIN_STRENGTH_GU, "Jin Strength Gu");
        addItem(ModItems.TENS_JIN_STRENGTH_GU, "Ten Jin Strength Gu");
        addItem(ModItems.JUN_STRENGTH_GU, "Jun Strength Gu");
        addItem(ModItems.TENS_JUN_STRENGTH_GU, "Ten Jun Strength Gu");
        addItem(ModItems.VITALITY_LEAF_GU, "Vitality Leaf Gu");
        addItem(ModItems.LIFESPAN_GU, "Lifespan Gu");
        addItem(ModItems.TENS_LIFESPAN_GU, "Ten-Year Lifespan Gu");
        addItem(ModItems.HUNDREDS_LIFESPAN_GU, "Hundred-Year Lifespan Gu");
        addItem(ModItems.THOUSANDS_LIFESPAN_GU, "Thousand-Year Lifespan Gu");
        addItem(ModItems.LIQUOR_WORM, "Liquor Worm");
        addItem(ModItems.FOUR_FLAVORS_LIQUOR_WORM, "Four Flavors Liquor Worm");
        addItem(ModItems.SEVEN_FRAGRANCES_LIQUOR_WORM, "Seven Fragrances Liquor Worm");
        addItem(ModItems.NINE_EYES_LIQUOR_WORM, "Nine Eyes Liquor Worm");
        addItem(ModItems.ROAMING_ZOMBIE_GU, "Roaming Zombie Gu");
        addItem(ModItems.HAIRY_ZOMBIE_GU, "Hairy Zombie Gu");
        addItem(ModItems.HOPPING_ZOMBIE_GU, "Hopping Zombie Gu");
        addItem(ModItems.HEAVENLY_DEMON_ZOMBIE_GU, "Heavenly Demon Zombie Gu");
        addItem(ModItems.NIGHTMARE_ZOMBIE_GU, "Nightmare Zombie Gu");
        addItem(ModItems.ASURA_ZOMBIE_GU, "Asura Zombie Gu");
        addItem(ModItems.EARTH_CHIEF_ZOMBIE_GU, "Earth Chief Zombie Gu");
        addItem(ModItems.PLAGUE_ZOMBIE_GU, "Plague Zombie Gu");
        addItem(ModItems.BLOOD_WIGHT_GU, "Blood Wight Gu");
        addItem(ModItems.PRIMEVAL_ELDER_GU_1, "Primeval Elder Gu I");
        addItem(ModItems.PRIMEVAL_ELDER_GU_2, "Primeval Elder Gu II");
        addItem(ModItems.PRIMEVAL_ELDER_GU_3, "Primeval Elder Gu III");
        addItem(ModItems.PRIMEVAL_ELDER_GU_4, "Primeval Elder Gu IV");
        addItem(ModItems.PRIMEVAL_ELDER_GU_5, "Primeval Elder Gu V");
        addItem(ModItems.HEAVENLY_ESSENCE_TREASURE_LOTUS_GU, "Heavenly Essence Treasure Lotus Gu");
        addItem(ModItems.HEAVENLY_ESSENCE_TREASURE_MONARCH_LOTUS_GU, "Heavenly Essence Treasure Monarch Lotus Gu");
        addItem(ModItems.HEAVENLY_ESSENCE_TREASURE_KING_LOTUS_GU, "Heavenly Essence Treasure King Lotus Gu");
        addItem(ModItems.SECOND_WATCH_GU, "Second Watch Gu");
        addItem(ModItems.THIRD_WATCH_GU, "Third Watch Gu");
        addItem(ModItems.MALICIOUS_THOUGHT_GU_2, "Malicious Thought Gu II");
        addItem(ModItems.MALICIOUS_THOUGHT_GU_3, "Malicious Thought Gu III");
        addItem(ModItems.MALICIOUS_THOUGHT_GU_4, "Malicious Thought Gu IV");
        addItem(ModItems.MALICIOUS_THOUGHT_GU_5, "Malicious Thought Gu V");
        addItem(ModItems.GUTS_GU, "Guts Gu");
        addItem(ModItems.CASUAL_GU_1, "Casual Gu I");
        addItem(ModItems.CASUAL_GU_2, "Casual Gu II");
        addItem(ModItems.STONE_APERTURE_GU_3, "Stone Aperture Gu III");
        addItem(ModItems.STONE_APERTURE_GU_4, "Stone Aperture Gu IV");
        addItem(ModItems.STONE_APERTURE_GU_5, "Stone Aperture Gu V");
        addItem(ModItems.SECOND_APERTURE_GU_1, "Second Aperture Gu I");
        addItem(ModItems.SECOND_APERTURE_GU_2, "Second Aperture Gu II");
        addItem(ModItems.SECOND_APERTURE_GU_3, "Second Aperture Gu III");
        addItem(ModItems.SECOND_APERTURE_GU_4, "Second Aperture Gu IV");
        addItem(ModItems.SECOND_APERTURE_GU_5, "Second Aperture Gu V");
        addItem(ModItems.PRIMEVAL_STONE, "Primeval Stone");
        addItem(ModItems.HUMAN_APERTURE_1, "Human Aperture I");
        addItem(ModItems.HUMAN_APERTURE_2, "Human Aperture II");
        addItem(ModItems.HUMAN_APERTURE_3, "Human Aperture III");
        addItem(ModItems.HUMAN_APERTURE_4, "Human Aperture IV");
        addItem(ModItems.HUMAN_APERTURE_5, "Human Aperture V");
        addItem(ModItems.LIQUOR, "Liquor");
        addItem(ModItems.SOUR_LIQUOR, "Sour Liquor");
        addItem(ModItems.SWEET_LIQUOR, "Sweet Liquor");
        addItem(ModItems.BITTER_LIQUOR, "Bitter Liquor");
        addItem(ModItems.SPICY_LIQUOR, "Spicy Liquor");
        addBlock(ModBlocks.SPIRIT_SPRING, "Spirit Spring");

        addItem(ModItems.SWORD_QI_1, "Sword Qi I");
        addItem(ModItems.SWORD_QI_2, "Sword Qi II");
        addItem(ModItems.SWORD_QI_3, "Sword Qi III");
        addItem(ModItems.SWORD_QI_4, "Sword Qi IV");
        addItem(ModItems.SWORD_QI_5, "Sword Qi V");
        addItem(ModItems.STRENGTH_QI_1, "Strength Qi I");
        addItem(ModItems.STRENGTH_QI_2, "Strength Qi II");
        addItem(ModItems.STRENGTH_QI_3, "Strength Qi III");
        addItem(ModItems.STRENGTH_QI_4, "Strength Qi IV");
        addItem(ModItems.STRENGTH_QI_5, "Strength Qi V");
        addItem(ModItems.LIFE_QI_1, "Life Qi I");
        addItem(ModItems.LIFE_QI_2, "Life Qi II");
        addItem(ModItems.LIFE_QI_3, "Life Qi III");
        addItem(ModItems.LIFE_QI_4, "Life Qi IV");
        addItem(ModItems.LIFE_QI_5, "Life Qi V");
        addItem(ModItems.ESSENCE_QI_1, "Essence Qi I");
        addItem(ModItems.ESSENCE_QI_2, "Essence Qi II");
        addItem(ModItems.ESSENCE_QI_3, "Essence Qi III");
        addItem(ModItems.ESSENCE_QI_4, "Essence Qi IV");
        addItem(ModItems.ESSENCE_QI_5, "Essence Qi V");
        addItem(ModItems.DEATH_QI_5, "Death Qi V");

        add("itemGroup.guzhenren.mortal_gu", "Mortal Gu");
        add("itemGroup.guzhenren.gu_material", "Gu Material");
        add("itemGroup.guzhenren.strength_mortal_gu", "Strength Mortal Gu");

        add("guzhenren.item.failed.awakened", "You have already awakened");
        add("guzhenren.item.failed.unawakened", "You have NOT awakened");
        add("guzhenren.item.failed.essence_full", "Essence is already FULL");
        add("guzhenren.item.failed.rank_mismatch", "Wrong realm - this Gu needs %s");
        add("guzhenren.item.failed.stage_peak", "You are at the Stage Peak");
        add("guzhenren.item.failed.second_aperture_rank", "The second aperture already stands at or above this rank");
        add("guzhenren.item.failed.aperture_unavailable", "No usable aperture remains");
        add("guzhenren.item.failed.beast_strength_held", "You already hold the %s's strength");
        add("guzhenren.item.failed.human_strength_full", "This strength is already at %s layers");
        add("guzhenren.item.failed.vitality_active", "Vitality Leaf is still working");
        add("guzhenren.item.failed.refine_essence", "NOT enough essence to refine");
        add("guzhenren.item.failed.essence", "NOT enough essence");
        add("guzhenren.item.failed.liquor_rank", "Only a %s Gu Master can use this");
        add("guzhenren.item.failed.liquor_distilling", "You are already distilling");
        add("guzhenren.item.failed.elder_gu_empty", "This Gu holds no Primeval Stones");
        add("guzhenren.item.failed.elder_gu_full", "This Gu is full of Primeval Stones");
        add("guzhenren.item.failed.elder_gu_no_stones", "You carry no Primeval Stones to store");
        add("guzhenren.item.failed.gu_cooldown", "This Gu needs another %s seconds");
        add("guzhenren.item.failed.all_out_active", "The all-out effort has not yet passed");
        add("guzhenren.item.failed.zombie_already", "Already a zombie, nothing left to turn");
        add("guzhenren.item.failed.gu_starving", "This Gu is too hungry - feed it first");
        add("guzhenren.item.failed.no_use", "This Gu needs no use");
        add("guzhenren.item.gu.invested", "Invested %s/%s");
        add("guzhenren.item.gu.refine_progress", "Refined %s/%s");
        add("guzhenren.item.gu.refine_cost", "Refine %s");
        add("guzhenren.item.gu.hunger_progress", "Fed %s/%s");
        add("guzhenren.item.gu.stored_stones", "Stored %s/%s");
        add("guzhenren.item.gu.lifespan_gained", "Lifespan +%s years");
        add("guzhenren.item.gu.hungry", "Your %s is hungry");
        add("guzhenren.item.gu.starved", "Your %s starved to death");
        add("guzhenren.item.gu.exhausted", "Your %s was forced past its limit and died");
        add("guzhenren.item.gu.ruined", "Your %s was ruined in the refinement");
        add("guzhenren.item.gu.health", "Health %s / %s");
        add("guzhenren.item.gu.vital_lost", "Your %s is gone -- health, soul and mind all suffer!");
        add("guzhenren.item.death_qi_cured", "Life Qi dispels the Death Qi -- %s years of lifespan restored");

        add("effect.guzhenren.vitality_leaf", "Vitality Leaf Gu Effect");
        add("effect.guzhenren.liquor_worm", "Liquor Worm Effect");
        add("effect.guzhenren.life_qi", "Life Qi Effect");
        add("effect.guzhenren.essence_qi", "Essence Qi Effect");
        add("effect.guzhenren.death_qi", "Death Qi Effect");
        add("effect.guzhenren.strength_qi", "Strength Qi Effect");
        add("effect.guzhenren.flower_boar_gu", "Flower Boar Gu Effect");
        add("effect.guzhenren.dragonpill_cricket_gu", "Dragonpill Cricket Gu Effect");
        add("effect.guzhenren.brute_force_longhorn_beetle_gu", "Brute Force Longhorn Beetle Gu Effect");
        add("effect.guzhenren.horizontal_crash_gu", "Horizontal Crash Gu Effect");
        add("effect.guzhenren.vertical_crash_gu", "Vertical Crash Gu Effect");
        add("effect.guzhenren.charging_crash_gu", "Charging Crash Gu Effect");
        add("effect.guzhenren.self_reliance_gu", "Self Reliance Gu Effect");
        add("effect.guzhenren.hardship_strength_gu", "Hardship Strength Gu Effect");
        add("effect.guzhenren.all_out_effort", "All-Out Effort Gu Effect");
        add("effect.guzhenren.half_zombie", "Half-Zombie Effect");
        add("effect.guzhenren.second_watch_gu", "Second Watch Gu Effect");
        add("effect.guzhenren.third_watch_gu", "Third Watch Gu Effect");
        add("effect.guzhenren.malicious_thought_gu", "Malicious Thought Gu Effect");
        add("effect.guzhenren.casual_gu", "Casual Gu Effect");
    }
    //endregion

    //region ENTITY
    private void addEntityKeys() {
        add("entity.guzhenren.hope_gu_entity", "Hope Gu");
        add("entity.guzhenren.white_boar_gu_entity", "White Boar Gu");
        add("entity.guzhenren.black_boar_gu_entity", "Black Boar Gu");
        add("entity.guzhenren.flower_boar_gu_entity", "Flower Boar Gu");
        add("entity.guzhenren.wild_boar", "Wild Boar");
        add("entity.guzhenren.brown_bear", "Brown Bear");
        add("entity.guzhenren.asian_black_bear", "Asian Black Bear");
        add("entity.guzhenren.american_black_bear", "American Black Bear");
        add("entity.guzhenren.albino_bear", "Albino Bear");
        add("entity.guzhenren.tiger", "Tiger");
        add("entity.guzhenren.white_tiger", "White Tiger");
        add("entity.guzhenren.horizontal_crash_gu_entity", "Horizontal Crash Gu");
        add("entity.guzhenren.vertical_crash_gu_entity", "Vertical Crash Gu");
        add("entity.guzhenren.charging_crash_gu_4_entity", "Charging Crash Gu IV");
        add("entity.guzhenren.charging_crash_gu_5_entity", "Charging Crash Gu V");
        add("entity.guzhenren.test_trade_gu_immortal", "Test Trade Gu Immortal");
    }
    //endregion

    //region ADVANCEMENT
    private void addAdvancementKeys() {
        add("advancements.guzhenren.first_awakening.title", "Awakening!");
        add("advancements.guzhenren.first_awakening.description", "Use your first Hope Gu to open your first aperture");
    }
    //endregion

    //region DEATH
    private void addDeathMessages() {
        add("death.attack.guzhenren.lifespan_exhausted", "%1$s ran out of lifespan");
        add("death.attack.guzhenren.soul_collapse", "%1$s suffered soul collapse");
        add("death.attack.guzhenren.mind_ocean_shattered", "%1$s shattered their Mind Ocean");
        add("death.attack.guzhenren.aperture_pressure_explosion", "%1$s died in an aperture pressure explosion");
        add("death.attack.guzhenren.ten_extreme_disaster", "%1$s was killed by a Ten-Extremes disaster");
        add("death.attack.guzhenren.vital_gu_lost", "%1$s lost their Vital Gu");
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
        add(BeastStrength.WHITE_BOAR, "White Boar");
        add(BeastStrength.BLACK_BOAR, "Black Boar");
        add(BeastStrength.BEAR, "Bear");
    }

    private void addStrengthPathBranch() {
        add(StrengthPathBranch.BEAST_STRENGTH_PHANTOM, "Beast Strength Phantom Branch");
        add(StrengthPathBranch.HUMAN_JUN_STRENGTH, "Human Jun Strength Branch");
        add(StrengthPathBranch.ATMOSPHERIC_HEAVEN_AND_EARTH, "Atmospheric Heaven and Earth Branch");
        add(StrengthPathBranch.NORMAL, "Normal");
    }

    private void addHumanStrength() {
        add(HumanStrength.JIN, "Jin");
        add(HumanStrength.TEN_JIN, "Ten Jin");
        add(HumanStrength.JUN, "Jun");
        add(HumanStrength.TEN_JUN, "Ten Jun");
    }

    private void addTitle() {
        add(Title.MORTAL, "Mortal");
        add(Title.GU_MASTER, "Gu Master");
        add(Title.GU_IMMORTAL, "Gu Immortal");
    }

    private void addRank() {
        add(Rank.NONE, "");
        add(Rank.ONE, "Rank I");
        add(Rank.TWO, "Rank II");
        add(Rank.THREE, "Rank III");
        add(Rank.FOUR, "Rank IV");
        add(Rank.FIVE, "Rank V");
        add(Rank.SIX, "Rank VI");
        add(Rank.SEVEN, "Rank VII");
        add(Rank.EIGHT, "Rank VIII");
        add(Rank.NINE, "Rank IX");
    }

    private void addStage() {
        add(Stage.NONE, "");
        add(Stage.INIT, "Initial");
        add(Stage.MIDDLE, "Middle");
        add(Stage.UPPER, "Upper");
        add(Stage.PEAK, "Peak");
    }

    private void addApertureStatus() {
        add(ApertureStatus.NORMAL, "Normal");
        add(ApertureStatus.DEAD, "Dead");
    }

    private void addTalent() {
        add(Talent.EXTREME, "Ten-Extremes Aptitude");
        add(Talent.FIRST, "Grade-A Aptitude");
        add(Talent.SECOND, "Grade-B Aptitude");
        add(Talent.THIRD, "Grade-C Aptitude");
        add(Talent.FOURTH, "Grade-D Aptitude");
        add(Talent.NONE, "Unawakened");
    }

    private void addPhysique() {
        add(Physique.ZOMBIE, "Zombie");
        add(Physique.HALF_ZOMBIE, "Half-Zombie");
        add(Physique.EXTREME, "Extreme");
    }

    private void addRace() {
        add(Race.HUMAN, "Human");
        add(Race.HAIRY_MEN, "Hairy Men");
        add(Race.EGGMEN, "Eggmen");
        add(Race.ROCKMEN, "Rockmen");
        add(Race.FEATHERMEN, "Feathermen");
        add(Race.INKMEN, "Inkmen");
        add(Race.MINIMEN, "Minimen");
        add(Race.MERMEN, "Mermen");
        add(Race.BEASTMEN, "Beastmen");
        add(Race.DRAGONMEN, "Dragonmen");
        add(Race.MUSHROOMMEN, "Mushroommen");
        add(Race.SNOWMEN, "Snowmen");
    }

    private void addEssenceColor() {
        add(EssenceColor.NONE, "None");
        add(EssenceColor.GREEN_COPPER, "Green Copper");
        add(EssenceColor.RED_STEEL, "Red Steel");
        add(EssenceColor.WHITE_SILVER, "White Silver");
        add(EssenceColor.YELLOW_GOLDEN, "Yellow Gold");
        add(EssenceColor.PURPLE_CRYSTAL, "Purple Crystal");
        add(EssenceColor.GREEN_GRAPE, "Green Grape");
        add(EssenceColor.RED_DATE, "Red Date");
        add(EssenceColor.WHITE_LITCHI, "White Litchi");
        add(EssenceColor.YELLOW_APRICOT, "Yellow Apricot");
    }

    private void addTenExtreme() {
        add(ExtremePhysique.NONE, "");
        add(ExtremePhysique.VERDANT_GREAT_SUN, "Verdant Great Sun");
        add(ExtremePhysique.DESOLATE_ANCIENT_MOON, "Desolate Ancient Moon");
        add(ExtremePhysique.NORTHERN_DARK_ICE_SOUL, "Northern Dark Ice Soul");
        add(ExtremePhysique.BOUNDLESS_FOREST_SAMSARA, "Boundless Forest Samsara");
        add(ExtremePhysique.BLAZING_GLORY_LIGHTNING_BRILLIANCE, "Blazing Glory Lightning Brilliance");
        add(ExtremePhysique.MYRIAD_GOLD_WONDROUS_ESSENCE, "Myriad Gold Wondrous Essence");
        add(ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL, "Great Strength True Martial");
        add(ExtremePhysique.CAREFREE_WISDOM_HEART, "Carefree Wisdom Heart");
        add(ExtremePhysique.PROFOUND_EARTH_ORIGIN, "Profound Earth Origin");
        add(ExtremePhysique.UNIVERSE_GREAT_DERIVATION, "Universe Great Derivation");
        add(ExtremePhysique.PURE_DREAM_REALITY_SEEKER, "Pure Dream Reality Seeker");
    }

    private void addMarkTag() {
        add(MarkTag.NATURAL, "Natural");
        add(MarkTag.RACE, "Race");
        add(MarkTag.EXTREME_PHYSIQUE, "Ten-Extremes Physique");
    }

    private void addBeastStrengthFamily() {
        add(BeastStrengthFamily.BOAR, "Boar");
        add(BeastStrengthFamily.BEAR, "Bear");
    }

    private void addQiKind() {
        add(QiKind.SWORD, "Sword Qi");
        add(QiKind.STRENGTH, "Strength Qi");
        add(QiKind.LIFE, "Life Qi");
        add(QiKind.ESSENCE, "Essence Qi");
        add(QiKind.DEATH, "Death Qi");
        add(QiKind.HUMAN, "Human Qi");
        add(QiKind.HEAVEN, "Heaven Qi");
        add(QiKind.EARTH, "Earth Qi");
    }

    private void addPath() {
        add(GuPath.HEAVEN, "Heaven Path");
        add(GuPath.RULE, "Rule Path");
        add(GuPath.SPACE, "Space Path");
        add(GuPath.TIME, "Time Path");
        add(GuPath.HUMAN, "Human Path");
        add(GuPath.METAL, "Metal Path");
        add(GuPath.WOOD, "Wood Path");
        add(GuPath.WATER, "Water Path");
        add(GuPath.FIRE, "Fire Path");
        add(GuPath.EARTH, "Earth Path");
        add(GuPath.ICE_SNOW, "Ice-Snow Path");
        add(GuPath.LIGHTNING, "Lightning Path");
        add(GuPath.CLOUD, "Cloud Path");
        add(GuPath.QI, "Qi Path");
        add(GuPath.SOUND, "Sound Path");
        add(GuPath.LIGHT, "Light Path");
        add(GuPath.DARK, "Dark Path");
        add(GuPath.POISON, "Poison Path");
        add(GuPath.STRENGTH, "Strength Path");
        add(GuPath.DREAM, "Dream Path");
        add(GuPath.REFINEMENT, "Refinement Path");
        add(GuPath.WISDOM, "Wisdom Path");
        add(GuPath.INFORMATION, "Information Path");
        add(GuPath.THEFT, "Theft Path");
        add(GuPath.LUCK, "Luck Path");
        add(GuPath.KILLING, "Killing Path");
        add(GuPath.BLOOD, "Blood Path");
        add(GuPath.SOUL, "Soul Path");
        add(GuPath.ENSLAVEMENT, "Enslavement Path");
        add(GuPath.FOOD, "Food Path");
        add(GuPath.FORMATION, "Formation Path");
        add(GuPath.PAINTING, "Painting Path");
        add(GuPath.TRANSFORMATION, "Transformation Path");
    }

    private void addAttainment() {
        add(GuAttainment.NONE, "None");
        add(GuAttainment.ORDINARY, "Ordinary");
        add(GuAttainment.QUASI_MASTER, "Quasi-Master");
        add(GuAttainment.MASTER, "Master");
        add(GuAttainment.QUASI_GRANDMASTER, "Quasi-Grandmaster");
        add(GuAttainment.GRANDMASTER, "Grandmaster");
        add(GuAttainment.QUASI_GREAT_GRANDMASTER, "Quasi-Great Grandmaster");
        add(GuAttainment.GREAT_GRANDMASTER, "Great Grandmaster");
        add(GuAttainment.QUASI_SUPREME_GRANDMASTER, "Quasi-Supreme Grandmaster");
        add(GuAttainment.SUPREME_GRANDMASTER, "Supreme Grandmaster");
    }

    private void addSoulTier() {
        add(SoulTier.ONE, "One-Person Soul");
        add(SoulTier.TEN, "Ten-Person Soul");
        add(SoulTier.HUNDRED, "Hundred-Person Soul");
        add(SoulTier.THOUSAND, "Thousand-Person Soul");
        add(SoulTier.TEN_THOUSAND, "Ten-Thousand-Person Soul");
        add(SoulTier.HUNDRED_THOUSAND, "Hundred-Thousand-Person Soul");
        add(SoulTier.MILLION, "Million-Person Soul");
        add(SoulTier.TEN_MILLION, "Ten-Million-Person Soul");
        add(SoulTier.HUNDRED_MILLION, "Hundred-Million-Person Soul");
    }

    private void addWisdomType() {
        add(MindPoolType.THOUGHTS, "Thoughts");
        add(MindPoolType.WILLS, "Wills");
        add(MindPoolType.EMOTIONS, "Emotions");
    }

    private void addBrilliance() {
        add(Brilliance.ORDINARY, "Ordinary Brilliance");
        add(Brilliance.DECENT, "Decent Brilliance");
        add(Brilliance.DISTINCTIVE, "Distinctive Brilliance");
        add(Brilliance.OUTSTANDING, "Outstanding Brilliance");
        add(Brilliance.UNRIVALED, "Unrivaled Brilliance");
    }

    private void addThoughtTag() {
        add(ThoughtTag.NATURAL, "Natural");
        add(ThoughtTag.EVIL, "Malicious");
    }
    //endregion
}
