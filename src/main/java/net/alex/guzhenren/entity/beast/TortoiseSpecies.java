package net.alex.guzhenren.entity.beast;

/**
 * 两种龟：共用 {@code tortoise} 几何与动画集，纹理与属性不同；石龟为同片萨瓦纳种群中稀有带甲变种；每种以
 * {@link #getId()} 注册独立实体类型（沿用熊先例），id 兼作纹理基名（{@code textures/entity/<id>.png}）；护甲为固定值，
 * 击退抗性为原版 0～1 属性。
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
