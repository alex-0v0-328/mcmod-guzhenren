package net.alex.guzhenren.entity.beast;

/**
 * The three cattle species. Each registers its own entity type under {@link #getId()} (bear precedent: one
 * variant, one entity type); they share the {@code buffalo} geometry and animation set and differ in texture
 * and in the weight of their charge and toss. All three live beside vanilla's cow in its biomes; the azure ox is
 * the rare one.
 *
 * <p>{@link #getId()} is the entity-type id and texture base name ({@code textures/entity/<id>.png}).
 * {@link #getChargeDamage()} and {@link #getTossDamage()} are flat damage before armor.
 */

public enum CattleSpecies {

    YELLOW_OX("yellow_ox", 6.0F, 4.0F),
    WATER_BUFFALO("water_buffalo", 8.0F, 6.0F),
    AZURE_OX("azure_ox", 8.0F, 6.0F);

    private final String id;
    private final float chargeDamage;
    private final float tossDamage;

    CattleSpecies(String id, float chargeDamage, float tossDamage) {
        this.id = id;
        this.chargeDamage = chargeDamage;
        this.tossDamage = tossDamage;
    }

    public String getId() { return this.id; }

    public float getChargeDamage() { return this.chargeDamage; }

    public float getTossDamage() { return this.tossDamage; }
}
