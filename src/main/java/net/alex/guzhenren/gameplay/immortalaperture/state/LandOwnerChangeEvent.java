package net.alex.guzhenren.gameplay.immortalaperture.state;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地易主事件：{@link LandMembership} 认主（{@link #isClaimed} 为真）或失主时，在改完主人与白名单之后同步发布，由组装层转给各
 * 功能（认主重挂强加载；失主关托管、撤强加载）。
 * <ul>
 *   <li>{@link #getLevel} 是福地维度，没装 Gu World 时为空：只改存档的事照做，碰世界的跳过。</li>
 * </ul>
 */

public class LandOwnerChangeEvent extends Event {

    private final @Nullable ServerLevel level;
    private final BlissfulLandData data;
    private final boolean claimed;

    public LandOwnerChangeEvent(@Nullable ServerLevel level, @NotNull BlissfulLandData data, boolean claimed) {
        this.level = level;
        this.data = data;
        this.claimed = claimed;
    }

    public @Nullable ServerLevel getLevel() { return level; }

    public @NotNull BlissfulLandData getData() { return data; }

    public boolean isClaimed() { return claimed; }
}
