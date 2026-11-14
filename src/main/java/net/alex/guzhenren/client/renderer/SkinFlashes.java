package net.alex.guzhenren.client.renderer;

import java.util.HashMap;
import java.util.Map;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.network.payload.SkinFlashPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

/**
 * 本客户端正在绘制的皮肤闪现。
 *
 * <ul>
 *   <li>每实体一个，由 {@link SkinFlashPayload} 发起，仅 {@link SkinFlashLayer} 读取；{@link Flash} 存所穿的皮与开始时
 *       的客户端游戏 tick；染色按皮族：玉皮翠绿，白玉皮暖白，铜皮铜色，铁皮钢灰，原生皮不闪现。</li>
 *   <li>alpha 从 {@link #START_ALPHA} 线性降至 0，{@link #FADE_TICKS}（1 秒）内淡尽；淡尽者在下次读取或客户端 tick 的
 *       {@link #prune} 移除，离开世界清空整表。</li>
 * </ul>
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
