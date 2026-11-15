package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestApertures.mortal;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;

/**
 * 空手攻击加成在主手变化后的下一服务端 tick 跟进，由装备变更事件驱动，无需显式刷新或每 tick 调用。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AttackDamageGameTests {

    private static final double DELTA = 0.001D;

    private AttackDamageGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void aHandSwapMovesTheFistBonusOnTheNextTick(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        double fist = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        helper.assertTrue(fist > base, "the bear grant added no fist attack");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        GameTestPlayers.serverTicks(player, 1);
        assertEquals(base, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        GameTestPlayers.serverTicks(player, 1);
        assertEquals(fist, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);
        helper.succeed();
    }
}
