package net.alex.guzhenren.client.renderer;

import java.util.HashMap;
import java.util.Map;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.network.payload.SkinFlashPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

/**
 * 本客户端正在播放的皮肤闪现状态表。
 * <ul>
 *   <li>每个实体至多保留一条闪现记录：由 {@link SkinFlashPayload} 触发写入，仅由 {@link SkinFlashLayer} 读取；
 *       {@link Flash} 保存所穿皮肤类型与开始时的客户端游戏 tick，染色按皮族区分（玉皮呈翠绿、白玉皮呈暖白、铜皮呈铜
 *       色、铁皮呈钢灰，原生皮肤不触发闪现）。</li>
 *   <li>Alpha 在 {@link #FADE_TICKS}（1 秒）内从 {@link #START_ALPHA} 线性衰减至 0；已淡尽的记录在下次读取或客户端
 *       tick 的 {@link #prune} 清理，离开世界时清空整张状态表。</li>
 * </ul>
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
