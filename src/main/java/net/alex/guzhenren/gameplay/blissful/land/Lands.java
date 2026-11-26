package net.alex.guzhenren.gameplay.blissful.land;

import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.gameplay.secretrealm.SecretRealms;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地[Blissful Land]的查询入口：维度、所在福地、落位。各层都从这里查，不经调度类。
 * <ul>
 *   <li>⚠ 福地维度不存在（没装 Gu World）时 {@link #level} 为空，调用方据此整段跳过；存档照存，装回后恢复。</li>
 *   <li>{@link #siteOf} 由秘境山心推出山巅、行宫与入口（{@link LandSite}）；秘境未解出时为空。</li>
 * </ul>
 */

public final class Lands {

    private Lands() {}

    public static @Nullable ServerLevel level(@NotNull MinecraftServer server, @NotNull BlissfulLand land) {
        return server.getLevel(land.dimension());
    }

    public static @Nullable BlissfulLand landOf(@NotNull Level level) {
        for (BlissfulLand land : BlissfulLand.ALL) {
            if (land.dimension() == level.dimension()) return land;
        }
        return null;
    }

    public static @Nullable LandSite siteOf(@NotNull MinecraftServer server, @NotNull BlissfulLand land) {
        RealmSite realm = SecretRealms.siteOf(server, land.realm());
        return realm == null ? null : siteOf(realm, land);
    }

    public static @NotNull LandSite siteOf(@NotNull RealmSite realm, @NotNull BlissfulLand land) {
        return LandSite.of(realm, land.realm().summitRise(), land.entry());
    }
}
