package net.alex.guzhenren.gameplay.aperture.awakening;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 希望蛊[Hope Gu]单击起的开窍[Awakening]仪式，附件 {@code awakening_ritual}。
 * <ul>
 *   <li>运行时仅 {@link AwakeningRitualService} 写；{@code startTick} 取各维度共用游戏时间，两端一致。</li>
 *   <li>⚠ 有附件即在仪式中；{@link #DEFAULT} 仅是构建器占位，不当仪式读。</li>
 *   <li>十绝体（非十绝为 {@code NONE}）随仪式同步：肉身数据只发本人，旁人也画牌匾。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningRitualService
 * @see AwakeningTimeline
 * @since 1.0.0
 */

public record AwakeningRitual(long startTick, int base, @NotNull ExtremePhysique physique, @NotNull Vec3 anchor,
                              boolean revealed) {

    public static final AwakeningRitual DEFAULT =
            new AwakeningRitual(0L, 0, ExtremePhysique.NONE, Vec3.ZERO, false);
    public static final Codec<AwakeningRitual> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("start_tick", 0L).forGetter(AwakeningRitual::startTick),
            Codec.INT.optionalFieldOf("base", 0).forGetter(AwakeningRitual::base),
            ExtremePhysique.CODEC.optionalFieldOf("physique", ExtremePhysique.NONE)
                    .forGetter(AwakeningRitual::physique),
            Vec3.CODEC.optionalFieldOf("anchor", Vec3.ZERO).forGetter(AwakeningRitual::anchor),
            Codec.BOOL.optionalFieldOf("revealed", false).forGetter(AwakeningRitual::revealed)
    ).apply(instance, AwakeningRitual::new));

    public static @NotNull AwakeningRitual decode(@NotNull FriendlyByteBuf buf) {
        return new AwakeningRitual(buf.readVarLong(), buf.readVarInt(), buf.readEnum(ExtremePhysique.class),
                buf.readVec3(), buf.readBoolean());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull AwakeningRitual value) {
        buf.writeVarLong(value.startTick());
        buf.writeVarInt(value.base());
        buf.writeEnum(value.physique());
        buf.writeVec3(value.anchor());
        buf.writeBoolean(value.revealed());
    }

    public @NotNull AwakeningRitual reveal() { return new AwakeningRitual(startTick, base, physique, anchor, true); }

    public float getElapsed(long gameTime, float partialTick) { return (float) (gameTime - startTick) + partialTick; }
}
