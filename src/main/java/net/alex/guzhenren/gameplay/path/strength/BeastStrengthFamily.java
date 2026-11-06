package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.core.NamedEnum;

/**
 * The family a beast strength [兽力] belongs to: the two boars share one bracket, the bear owns another, and the ox
 * exists only as the Brute Force Longhorn Beetle Gu's [蛮力天牛蛊] timed strength.
 *
 * <p>Readings group by family rather than by species constant, so a second boar lands in the boar
 * bracket instead of opening a new one. The declaration order is the B panel's reading order.
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastStrength
 * @see PathStrengthData
 * @since 1.0.0
 */

public enum BeastStrengthFamily implements NamedEnum {

    BOAR,
    OX,
    BEAR,
    TIGER;

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.strength.beast_family."; }
}
