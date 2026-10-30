package net.alex.guzhenren.gameplay.aperture.awakening;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * The awakening ritual [开窍仪式] a Hope Gu [希望蛊] click starts: when it began, the aptitude [资质] it will open, and
 * where the Hope Gu hovers before the player. Immutable record attachment keyed {@code awakening_ritual};
 * {@link AwakeningRitualService} is the only runtime writer.
 *
 * <p>⚠ It counts by presence: a player who has it is in the ritual, so {@link #DEFAULT} is only the builder's
 * placeholder and is never read as a ritual. {@code startTick} is game time, which every dimension shares, so the
 * clients that draw the hovering Gu read the same elapsed time as the server.
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningRitualService
 * @see AwakeningTimeline
 * @since 1.0.0
 */

public record AwakeningRitual(long startTick, int base, Vec3 anchor) {

    public static final AwakeningRitual DEFAULT = new AwakeningRitual(0L, 0, Vec3.ZERO);
    public static final Codec<AwakeningRitual> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("start_tick", 0L).forGetter(AwakeningRitual::startTick),
            Codec.INT.optionalFieldOf("base", 0).forGetter(AwakeningRitual::base),
            Vec3.CODEC.optionalFieldOf("anchor", Vec3.ZERO).forGetter(AwakeningRitual::anchor)
    ).apply(instance, AwakeningRitual::new));

    public static @NotNull AwakeningRitual decode(@NotNull FriendlyByteBuf buf) {
        return new AwakeningRitual(buf.readVarLong(), buf.readVarInt(), buf.readVec3());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull AwakeningRitual value) {
        buf.writeVarLong(value.startTick());
        buf.writeVarInt(value.base());
        buf.writeVec3(value.anchor());
    }

    public float getElapsed(long gameTime, float partialTick) { return (float) (gameTime - startTick) + partialTick; }
}
