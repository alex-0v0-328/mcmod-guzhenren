package net.alex.guzhenren.entity.beast;

/**
 * 三种牛：共用 {@code buffalo} 几何与动画集，纹理与冲撞、角挑伤害不同；均与原版牛同群系，青牛稀有；每种以
 * {@link #getId()} 注册独立实体类型（沿用熊先例），id 兼作纹理基名（{@code textures/entity/<id>.png}）；伤害皆为护甲前
 * 固定值。
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
