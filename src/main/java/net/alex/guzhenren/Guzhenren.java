package net.alex.guzhenren;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Mod entry point: builds every registry holder and hands them to the mod event bus.
 *
 * <p>Holds the {@code MOD_ID} constant and the {@link #id} helper used across the codebase for
 * {@link ResourceLocation} creation.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@Mod(Guzhenren.MOD_ID)
public class Guzhenren {

    public static final String MOD_ID = "guzhenren";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) { return new ResourceLocation(MOD_ID, path); }

    public Guzhenren() {
        LOGGER.info("Guzhenren loaded");
    }
}
