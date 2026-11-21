package net.alex.guzhenren.gameplay.blissful;

/**
 * 地灵经营面板上行的操作意图；服务端在 {@link EarthSpiritPanel#handle} 里逐个复判主人与参数。
 * <ul>
 *   <li>参数约定：{@code value} 为整数参数，{@code text} 为字符串参数，未用到的传 0 与空串。</li>
 *   <li>{@code text} 是玩家 UUID：{@link #EXPEL}、{@link #WHITELIST_SET}；是在线玩家名：{@link #WHITELIST_ADD}；是实体
 *       类型 ID：{@link #HERD_HOSTING}、{@link #HERD_KEEP}；是 {@link Territory.Side} 名：{@link #CEDE}；是物品 ID：
 *       {@link #TAKE_STACK}、{@link #TAKE_ALL}（服务端用来核对下标是否过时）。</li>
 *   <li>{@code value} 为 1 开 0 关：{@link #WHITELIST_SET}、两个托管开关；是新的保持数：{@link #HERD_KEEP}；是宽度（区
 *       块）：{@link #CEDE}；是快照里的库藏下标：{@link #TAKE_STACK}、{@link #TAKE_ALL}。</li>
 *   <li>⚠ 按序号编码，只在末尾追加；改动同步递增 {@code ModPayloads.VERSION}。</li>
 * </ul>
 */

public enum PanelAction {
    EXPEL_OUTSIDERS,
    EXPEL,
    WHITELIST_ADD,
    WHITELIST_SET,
    MOVE_MOBS,
    DRIVE_GUTS_GU,
    BREED,
    SLAUGHTER,
    HERD_HOSTING,
    HERD_KEEP,
    HARVEST,
    FARM_HOSTING,
    RECLAIM,
    CEDE,
    TAKE_STACK,
    TAKE_ALL,
    DEPOSIT_CARRIED
}
