package net.alex.guzhenren.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * The mod's key bindings.
 *
 * <p>Currently a single binding: {@code OPEN_INFO} (B), which opens the
 * {@link net.alex.guzhenren.client.screen.PlayerInfoScreen}. All keys live under the
 * {@code key.categories.guzhenren} category. It is bound in the {@code IN_GAME} context, so a key another mod binds
 * for its screens does not show as a conflict; the info screen closes on it through the context-free
 * {@code KeyMapping#matches}.
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
