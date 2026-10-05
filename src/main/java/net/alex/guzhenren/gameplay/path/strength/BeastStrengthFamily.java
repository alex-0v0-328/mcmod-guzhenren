package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.core.NamedEnum;

/**
 * The family a beast strength [兽力] belongs to: the two boars share one bracket, the bear owns the other.
 *
 * <p>Readings group by family rather than by species constant, so a second boar lands in the boar
 * bracket instead of opening a new one.
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastStrength
 * @see PathStrengthData
 * @since 1.0.0
 */

public enum BeastStrengthFamily implements NamedEnum {

    BOAR,
    BEAR;

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.strength.beast_family."; }
}
