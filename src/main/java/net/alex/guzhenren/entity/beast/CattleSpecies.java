package net.alex.guzhenren.entity.beast;

/**
 * 牛的三个种类枚举：共用 {@code buffalo} 模型几何与动画集，按种类区分贴图及冲撞、角挑的护甲前固定伤害。
 * <ul>
 *   <li>均与原版牛分布在同一生物群系，其中青牛为稀有变种；每个种类以 {@link #getId()} 注册独立实体类型（沿用熊的先
 *       例），ID 兼作贴图文件名（{@code textures/entity/<id>.png}）。</li>
 * </ul>
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
