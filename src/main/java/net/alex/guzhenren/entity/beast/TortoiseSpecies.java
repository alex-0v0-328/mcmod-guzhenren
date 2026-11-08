package net.alex.guzhenren.entity.beast;

/**
 * The two tortoise species. Each registers its own entity type under {@link #getId()} (bear precedent: one
 * variant, one entity type); they share the {@code tortoise} geometry and animation set and differ in texture
 * and stats. The stone tortoise is the rare, armored variant of the same savanna population.
 *
 * <p>{@link #getId()} is the entity-type id and texture base name ({@code textures/entity/<id>.png}).
 * {@link #getArmor()} is the flat armor and {@link #getKnockbackResistance()} the vanilla 0-1 attribute.
 */

public enum TortoiseSpecies {

    TORTOISE("tortoise", 20.0D, 4.0D, 0.0D, 0.12D),
    STONE_TORTOISE("stone_tortoise", 40.0D, 10.0D, 1.0D, 0.10D);

    private final String id;
    private final double maxHealth;
    private final double armor;
    private final double knockbackResistance;
    private final double movementSpeed;

    TortoiseSpecies(String id, double maxHealth, double armor, double knockbackResistance, double movementSpeed) {
        this.id = id;
        this.maxHealth = maxHealth;
        this.armor = armor;
        this.knockbackResistance = knockbackResistance;
        this.movementSpeed = movementSpeed;
    }

    public String getId() { return this.id; }

    public double getMaxHealth() { return this.maxHealth; }

    public double getArmor() { return this.armor; }

    public double getKnockbackResistance() { return this.knockbackResistance; }

    public double getMovementSpeed() { return this.movementSpeed; }
}
