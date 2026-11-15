package net.alex.guzhenren.advancement;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertSame;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HopeGuUsedTriggerGameTests {

    private HopeGuUsedTriggerGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void codecRoundtripsEmptyConditions(GameTestHelper helper) {
        HopeGuUsedTrigger.Instance instance = new HopeGuUsedTrigger.Instance(Optional.empty());
        var encoded = HopeGuUsedTrigger.Instance.CODEC.encodeStart(JsonOps.INSTANCE, instance).getOrThrow();
        assertTrue(encoded instanceof JsonObject json && json.isEmpty(),
                "no business fields, the conditions map must serialize empty");
        HopeGuUsedTrigger.Instance back = HopeGuUsedTrigger.Instance.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(instance, back);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void codecAndCriterionWiring(GameTestHelper helper) {
        HopeGuUsedTrigger trigger = new HopeGuUsedTrigger();
        assertSame(HopeGuUsedTrigger.Instance.CODEC, trigger.codec());
        var criterion = trigger.createCriterion();
        assertSame(trigger, criterion.trigger());
        assertEquals(new HopeGuUsedTrigger.Instance(Optional.empty()), criterion.triggerInstance());
        helper.succeed();
    }
}
