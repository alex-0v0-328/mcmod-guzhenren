package net.alex.guzhenren.entity;

/**
 * The four bear species. Each registers its own entity type under {@link #getId()} (boar Gu precedent:
 * one variant, one entity type); they share the {@code bear} geometry and animation set and differ in
 * texture, temperament and stats. Only the Asian black bear hunts on its own; the rest retaliate.
 *
 * <p>{@link #getId()} is the entity-type id and texture base name ({@code textures/entity/<id>.png}).
 * {@link #isHostile()} says whether the species seeks targets on sight instead of only retaliating.
 * {@link #getSwipeDamage()} is the flat swipe damage, before armor, and {@link #getRearDamage()} the flat
 * rear-up slam damage, before armor.
 */

public enum BearSpecies {

    BROWN("brown_bear", false, 54.0D, 9.0F, 16.0F),
    ASIAN_BLACK("asian_black_bear", true, 42.0D, 8.0F, 13.0F),
    AMERICAN_BLACK("american_black_bear", false, 40.0D, 8.0F, 13.0F),
    ALBINO("albino_bear", false, 40.0D, 8.0F, 13.0F);

    private final String id;
    private final boolean hostile;
    private final double maxHealth;
    private final float swipeDamage;
    private final float rearDamage;

    BearSpecies(String id, boolean hostile, double maxHealth, float swipeDamage, float rearDamage) {
        this.id = id;
        this.hostile = hostile;
        this.maxHealth = maxHealth;
        this.swipeDamage = swipeDamage;
        this.rearDamage = rearDamage;
    }

    public String getId() { return this.id; }

    public boolean isHostile() { return this.hostile; }

    public double getMaxHealth() { return this.maxHealth; }

    public float getSwipeDamage() { return this.swipeDamage; }

    public float getRearDamage() { return this.rearDamage; }
}
