package net.alex.guzhenren.gameplay.plunge;

/**
 * What a plunge [坠击] does on landing, as numbers: pure, so the pure JVM tests pin it; {@link PlungeService} applies
 * them.
 *
 * <p>The height is the whole fall: what the player had already fallen when the plunge started, plus the drop from
 * there to the landing ({@link #getHeight}). {@link #getDamage} is vanilla 1.21's fall damage for that height,
 * {@code ceil((height - safe fall distance) * fall damage multiplier)}, times {@link #DAMAGE_MULTIPLIER}; the two
 * attributes come from the player ({@link PlungeLanding#getFallDamage}). {@link #getCraterPower} is the vanilla
 * explosion power of the crater: none below {@link #CRATER_MIN_HEIGHT}, {@link #MIN_POWER} at it, one more every
 * {@link #POWER_STEP} blocks, capped at {@link #MAX_POWER} from 64 blocks up -- a charged creeper's 6, a hole about
 * 5 blocks across in dirt and far less in stone, since vanilla's blast resistance decides what breaks.
 *
 * <p>Between 3 and 4 blocks the fall already hurts (vanilla's safe distance is 3) but leaves no crater.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlungeService
 * @since 1.0.0
 */

public final class PlungeImpact {

    private PlungeImpact() {}

    public static final int DAMAGE_MULTIPLIER = 3;
    public static final double CRATER_MIN_HEIGHT = 4.0D;
    static final float MIN_POWER = 2.0F;
    static final float MAX_POWER = 6.0F;
    static final double POWER_STEP = 15.0D;

    public static double getHeight(double topY, double landingY) { return Math.max(0.0D, topY - landingY); }

    public static int getDamage(double height, double safeFallDistance, double fallDamageMultiplier) {
        int vanilla = (int) Math.ceil((height - safeFallDistance) * fallDamageMultiplier);
        return Math.max(0, vanilla) * DAMAGE_MULTIPLIER;
    }

    public static float getCraterPower(double height) {
        if (height < CRATER_MIN_HEIGHT) return 0.0F;
        return (float) Math.min(MAX_POWER, MIN_POWER + (height - CRATER_MIN_HEIGHT) / POWER_STEP);
    }
}
