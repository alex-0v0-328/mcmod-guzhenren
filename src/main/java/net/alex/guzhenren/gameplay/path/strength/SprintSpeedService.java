package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.attribute.TransientModifiers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

/**
 * The speed the held beasts add, the galloping horse's [驰马] among them, as one transient movement speed modifier.
 * A beast's {@code getSprintBonus} is a multiple of the walking speed: while the player sprints, every held beast's
 * adds to the sprint's own multiple (vanilla's {@link #VANILLA_SPRINT_FACTOR}, so the horse's 0.5 makes it 1.8);
 * while an attack round fully releases [全面解放] its family ({@code StrengthRound.getFullyReleased}), it adds to the
 * walk and the sprint alike, so a released horse walks at 1.5 and sprints at 2.3. {@link #getModifier} multiplies the
 * total, so vanilla's sprint and ours compose to exactly that multiple, and potions still scale the whole.
 *
 * <p>Server side, refreshed every tick by {@code PlayerTickEvents} beside the attack; the client gets it with the
 * attribute sync. It reads the sprint and the round, never the hand.
 *
 * <p>⚠ The modifier MUST stay transient ({@link TransientModifiers}).
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.path.strength.BeastStrength#getSprintBonus
 * @since 1.0.0
 */

public final class SprintSpeedService {

    private SprintSpeedService() {}

    public static final double VANILLA_SPRINT_FACTOR = 1.3D;
    private static final ResourceLocation MODIFIER_ID = Guzhenren.id("beast_sprint_speed");

    public static void refresh(@NotNull ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        PathStrengthData data = PathStrengthService.get(player);
        double release = data.getBeastSprintBonus(StrengthRound.getFullyReleased(player));
        TransientModifiers.swap(speed, MODIFIER_ID,
                getModifier(player.isSprinting(), data.getBeastSprintBonus(), release),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static double getModifier(boolean sprinting, double sprintBonus, double releaseBonus) {
        if (!sprinting) return Math.max(0.0D, releaseBonus);
        double bonus = Math.max(0.0D, sprintBonus) + Math.max(0.0D, releaseBonus);
        if (bonus <= 0.0D) return 0.0D;
        return (VANILLA_SPRINT_FACTOR + bonus) / VANILLA_SPRINT_FACTOR - 1.0D;
    }
}
