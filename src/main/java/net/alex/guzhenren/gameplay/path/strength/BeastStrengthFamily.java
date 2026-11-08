package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.alex.guzhenren.core.NamedEnum;

/**
 * The family a beast strength [兽力] belongs to: the two boars share one bracket, the bear owns another, and the ox
 * exists only as the Brute Force Longhorn Beetle Gu's [蛮力天牛蛊] timed strength.
 *
 * <p>Readings group by family rather than by species constant, so a second boar lands in the boar
 * bracket instead of opening a new one. The declaration order is the B panel's reading order; the ordinal is
 * synced to the phantoms, never saved.
 *
 * <p>{@link #canManifest} says whether the family takes shape as a Beast Phantom [兽力虚影] or a Beast Charge
 * [兽力冲击]: every family does, the ox on the buffalo's model -- though only by hand ({@code BeastPhantomEntity.show}),
 * since the rounds and the charged punch draw permanent families alone and the ox is a timed strength. A family
 * that cannot is still drawn and released, it only shows nothing.
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastStrength
 * @see PathStrengthData
 * @since 1.0.0
 */

public enum BeastStrengthFamily implements NamedEnum {

    BOAR(true),
    OX(true),
    BEAR(true),
    TIGER(true),
    TURTLE(true),
    HORSE(true);

    private final boolean manifests;

    BeastStrengthFamily(boolean manifests) { this.manifests = manifests; }

    public boolean canManifest() { return manifests; }

    public static List<BeastStrengthFamily> getManifesting(Collection<BeastStrengthFamily> families) {
        List<BeastStrengthFamily> manifesting = new ArrayList<>();
        for (BeastStrengthFamily family : families) if (family.canManifest()) manifesting.add(family);
        return manifesting;
    }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.strength.beast_family."; }
}
