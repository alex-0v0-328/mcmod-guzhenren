package net.alex.guzhenren.core;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;

/**
 * Posts this mod's own events on the game event bus, where a lower layer tells a higher one that something
 * changed (an aperture, a physique, a strength path, a lost Vital Gu), and asks the bus whether an entity may
 * change the terrain.
 *
 * <p>{@link #canGrief} is the one gate every terrain change of this mod goes through: the pressure-explosion crater
 * [空窍压力爆炸] and the Mountain and River As Before Gu [山如故 / 江如故]. It follows vanilla's {@code mobGriefing}
 * through the loader's griefing event, so a server turns them off with {@code /gamerule mobGriefing false} and a
 * protection mod can answer per entity. Damage never asks it.
 *
 * <p>⚠ A version seam: services post through {@link #post} and ask through {@link #canGrief} only, so the calling
 * lines stay identical between this NeoForge 1.21.1 mainline and the Forge 1.20.1 port; the event classes extend
 * each loader's own {@code Event}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModEvents {

    private ModEvents() {}

    public static void post(@NotNull Event event) { NeoForge.EVENT_BUS.post(event); }

    public static boolean canGrief(@NotNull Level level, @NotNull Entity entity) {
        return EventHooks.canEntityGrief(level, entity);
    }
}
