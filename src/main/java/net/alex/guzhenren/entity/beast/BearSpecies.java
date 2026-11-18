package net.alex.guzhenren.entity.beast;

/**
 * 熊的四个种类枚举：共用 {@code bear} 模型几何与动画集，按种类区分贴图、攻击性与基础属性。
 * <ul>
 *   <li>每个种类以 {@link #getId()} 注册独立实体类型（沿用豕蛊[Boar Gu]实体一变体一类型的先例），ID 兼作贴图文件名
 *       （{@code textures/entity/<id>.png}）。</li>
 *   <li>仅亚洲黑熊会主动索敌（{@link #isHostile()}），其余种类仅在受击后反击；挥击与站立拍击伤害均为护甲前固定值。</li>
 * </ul>
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
