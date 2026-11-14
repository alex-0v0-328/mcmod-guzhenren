package net.alex.guzhenren.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * 模组按键：仅 {@code OPEN_INFO}（B）打开 {@link net.alex.guzhenren.client.screen.PlayerInfoScreen}；绑 {@code IN_GAME}
 * 上下文，他模组界面同键不报冲突，信息界面以不分上下文的 {@code KeyMapping#matches} 用它关闭。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.screen.PlayerInfoScreen
 * @since 1.0.0
 */

public final class ModKeyMappings {

    private ModKeyMappings() {}

    public static final String CATEGORY = "key.categories.guzhenren";
    public static final KeyMapping OPEN_INFO =
            new KeyMapping("key.guzhenren.open_info", KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
}
