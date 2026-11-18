package net.alex.guzhenren.gameplay.aperture.awakening;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 希望蛊[Hope Gu]开窍[Awakening]仪式状态记录（附件 {@code awakening_ritual}）。
 * <ul>
 *   <li>运行时仅由 {@link AwakeningRitualService} 写入；{@code startTick} 取世界共享 {@code gameTime}，保证服务端与客
 *       户端按同一时间轴（{@link AwakeningTimeline}）推进。</li>
 *   <li>⚠ 玩家身上存在此附件即视为正在进行开窍仪式；{@link #DEFAULT} 仅供 NeoForge 附件构建器作初始化占位，业务读取以
 *       {@code hasData} 为准。</li>
 *   <li>预掷的十绝体[Ten Extreme Physiques]（非十绝为 {@code NONE}）随此附件同步给所有追踪客户端，以便周围玩家在肉身附
 *       件仅同步本人的情况下也能渲染出正确的十绝牌匾。</li>
 * </ul>
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
