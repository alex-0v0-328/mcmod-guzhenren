package net.alex.guzhenren.client.renderer;

import java.util.HashMap;
import java.util.Map;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.network.payload.SkinFlashPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The skin flashes [皮肤闪现] this client is drawing: one per entity, started by a {@link SkinFlashPayload}
 * and fading over {@link #FADE_TICKS}. {@link SkinFlashLayer} reads them; nothing else does. A {@link Flash}
 * holds the skin that was put on and the client game tick it started.
 *
 * <p>Each skin family has its tint: emerald green for the jade skin [玉皮], warm white for the white jade skin
 * [白玉皮], copper for the bronze skins [铜皮], steel gray for the iron skins [铁皮]; the native skin never
 * flashes. The alpha starts at {@link #START_ALPHA} and falls linearly, so the overlay is gone after one second.
 * A flash that has faded is dropped on the next read or by the client tick's {@link #prune}; leaving the world
 * clears the table.
 *
 * @author Alex
 * @version 1.0.0
 * @see SkinFlashLayer
 * @since 1.0.0
 */

public final class SkinFlashes {

    private SkinFlashes() {}

    public static final int FADE_TICKS = 20;
    public static final float START_ALPHA = 0.6F;
    private static final int JADE_TINT = 0x4FBF8A;
    private static final int WHITE_JADE_TINT = 0xF2F0E6;
    private static final int BRONZE_TINT = 0xC47A3A;
    private static final int IRON_TINT = 0xB8BEC8;
    private static final Map<Integer, Flash> FLASHES = new HashMap<>();

    public record Flash(Skin skin, long startTick) {

        public float getAlpha(long now, float partialTick) {
            return getAlpha(now - startTick + partialTick);
        }

        public static float getAlpha(float elapsedTicks) {
            if (elapsedTicks < 0.0F) return START_ALPHA;
            return Math.max(0.0F, START_ALPHA * (1.0F - elapsedTicks / FADE_TICKS));
        }

        public int getTint() { return getTintOf(skin); }
    }

    public static int getTintOf(Skin skin) {
        return switch (skin.getFamily()) {
            case JADE -> skin == Skin.WHITE_JADE_SKIN ? WHITE_JADE_TINT : JADE_TINT;
            case BRONZE -> BRONZE_TINT;
            case IRON -> IRON_TINT;
            case NATIVE -> 0xFFFFFF;
        };
    }

    public static void flash(SkinFlashPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.skin().isNative()) return;

        FLASHES.put(payload.entityId(), new Flash(payload.skin(), level.getGameTime()));
    }

    public static @Nullable Flash get(int entityId, long now) {
        Flash flash = FLASHES.get(entityId);
        if (flash == null) return null;
        if (flash.getAlpha(now, 1.0F) <= 0.0F) {
            FLASHES.remove(entityId);
            return null;
        }
        return flash;
    }

    public static void prune(long now) {
        FLASHES.values().removeIf(flash -> flash.getAlpha(now, 1.0F) <= 0.0F);
    }

    public static void clear() { FLASHES.clear(); }
}
