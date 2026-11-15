package net.alex.guzhenren.lifecycle;

import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PendingVitalPenaltiesGameTests {

    private PendingVitalPenaltiesGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void firstInFirstOutPerOwner(GameTestHelper helper) {
        PendingVitalPenalties ledger = new PendingVitalPenalties();
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        ledger.recordPenalty(owner, new ItemStack(Items.STICK));
        ledger.recordPenalty(owner, new ItemStack(Items.BONE));
        ledger.recordPenalty(other, new ItemStack(Items.APPLE));

        assertEquals(2, ledger.count(owner));
        assertTrue(ledger.poll(owner).is(Items.STICK));
        assertTrue(ledger.poll(owner).is(Items.BONE));
        assertNull(ledger.poll(owner));
        assertEquals(0, ledger.count(owner));
        assertEquals(1, ledger.count(other));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void emptyAndUnknown(GameTestHelper helper) {
        PendingVitalPenalties ledger = new PendingVitalPenalties();
        UUID owner = UUID.randomUUID();
        ledger.recordPenalty(owner, ItemStack.EMPTY);

        assertEquals(0, ledger.count(owner));
        assertNull(ledger.poll(owner));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void recordsACopy(GameTestHelper helper) {
        PendingVitalPenalties ledger = new PendingVitalPenalties();
        UUID owner = UUID.randomUUID();
        ItemStack stack = new ItemStack(Items.STICK);
        ledger.recordPenalty(owner, stack);
        stack.setCount(0);

        assertTrue(ledger.poll(owner).is(Items.STICK));
        helper.succeed();
    }
}
