package net.alex.guzhenren.gameplay.path.strength;

/**
 * A timed buff that is one beast's strength [一X之力] for as long as it runs: the Flower Boar Gu [花豕蛊] is one
 * boar's, the Brute Force Longhorn Beetle Gu [蛮力天牛蛊] one ox's.
 *
 * <p>Implemented by the buff's vanilla effect. The B panel reads it to word the buff as its strength with a
 * countdown under Strength Attainment [力道造诣], instead of listing it as a Gu effect among the path buffs.
 * {@link #getPoints} is what the buff adds to the body's strength points [力量点] while it runs, read by
 * {@link PathStrengthService#getBeastBuffs} as a {@link StrengthAmplifier} that brings its own carrying capacity, so
 * a timed beast is always brought to bear and never counts toward admitting a permanent one.
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastStrengthFamily
 * @since 1.0.0
 */

public interface BeastStrengthBuff {

    BeastStrengthFamily getBeastFamily();

    int getPoints();
}
