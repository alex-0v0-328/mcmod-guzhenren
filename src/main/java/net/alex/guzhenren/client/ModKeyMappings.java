package net.alex.guzhenren.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * 模组客户端按键绑定：当前仅注册 {@code OPEN_INFO}（默认 B 键）用于打开 {@link PlayerInfoScreen}；绑定至
 * {@code IN_GAME} 上下文以避免与其他模组同键冲突，信息界面自身则通过不分上下文的 {@code KeyMapping#matches} 用同一按键
 * 关闭界面。
 */

public final class ModKeyMappings {

    private ModKeyMappings() {}

    public static final String CATEGORY = "key.categories.guzhenren";
    public static final KeyMapping OPEN_INFO =
            new KeyMapping("key.guzhenren.open_info", KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
}
