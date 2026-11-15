package net.alex.guzhenren.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.gameplay.body.Anatomy;
import net.alex.guzhenren.gameplay.body.BodyPart;
import net.alex.guzhenren.gameplay.body.Injury;
import net.alex.guzhenren.gameplay.body.Organ;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BodyFigureTest {

    @Test
    @DisplayName("an unhurt body gives no tooltip on any limb, so hovering the figure shows nothing")
    void unhurtBodyHasNoTooltip() {
        for (BodyPart part : BodyPart.values()) {
            assertEquals(List.of(), describe(Anatomy.DEFAULT, part), part.name());
        }
    }

    @Test
    @DisplayName("only the injured limb gives a tooltip, listing just itself")
    void onlyInjuredLimbHasTooltip() {
        Anatomy anatomy = Anatomy.DEFAULT.withInjury(BodyPart.LEFT_ARM, Injury.SEVERE);
        for (BodyPart part : BodyPart.values()) {
            List<String> expected = part == BodyPart.LEFT_ARM ? List.of(entry(part, Injury.SEVERE)) : List.of();
            assertEquals(expected, describe(anatomy, part), part.name());
        }
    }

    @Test
    @DisplayName("an unhurt head with an injured orifice lists only that orifice")
    void unhurtHeadListsOnlyInjuredOrifice() {
        Anatomy anatomy = Anatomy.DEFAULT.withInjury(Organ.EYES, Injury.LIGHT);
        assertEquals(List.of(entry(Organ.EYES, Injury.LIGHT)), describe(anatomy, BodyPart.HEAD));
    }

    @Test
    @DisplayName("an injured head lists itself first, then only its injured orifices in enum order")
    void injuredHeadListsItselfThenInjuredOrifices() {
        Anatomy anatomy = Anatomy.DEFAULT.withInjury(BodyPart.HEAD, Injury.MODERATE)
                .withInjury(Organ.MOUTH, Injury.CRIPPLED).withInjury(Organ.EARS, Injury.LIGHT);
        assertEquals(List.of(entry(BodyPart.HEAD, Injury.MODERATE), entry(Organ.EARS, Injury.LIGHT),
                entry(Organ.MOUTH, Injury.CRIPPLED)), describe(anatomy, BodyPart.HEAD));
    }

    private static String entry(NamedEnum name, Injury injury) {
        return name.getTranslationKey() + "=" + injury.getTranslationKey();
    }

    private static List<String> describe(Anatomy anatomy, BodyPart part) {
        return BodyFigure.getTooltip(anatomy, part).stream().map(BodyFigureTest::describe).toList();
    }

    private static String describe(Component line) {
        Object[] args = ((TranslatableContents) line.getContents()).getArgs();
        return getKey(args[0]) + "=" + getKey(args[1]);
    }

    private static String getKey(Object arg) {
        return ((TranslatableContents) ((Component) arg).getContents()).getKey();
    }
}
