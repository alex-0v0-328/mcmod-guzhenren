package net.alex.guzhenren.item.gu.stack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 持续照顾型蛊[Tended Gu]的伤势记录：挂在 {@code GU_CONDITION} 数据组件上，组件缺省即正常。
 * <ul>
 *   <li>{@link Stage} 三态各带满额使用次数：受伤 9 次、濒死 3 次；正常不计次，也不落组件（读写只经
 *       {@code GuHealth}）。</li>
 *   <li>{@code mended} 是本命蛊[Vital Gu]借玩家回血累计的生命点，转档后按满额扣回、余数保留。</li>
 * </ul>
 */

public record GuCondition(Stage stage, int usesLeft, float mended) {

    public static final Codec<GuCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Stage.CODEC.fieldOf("stage").forGetter(GuCondition::stage),
            Codec.INT.optionalFieldOf("uses_left", 0).forGetter(GuCondition::usesLeft),
            Codec.FLOAT.optionalFieldOf("mended", 0.0F).forGetter(GuCondition::mended)
    ).apply(instance, GuCondition::new));

    public static @NotNull GuCondition decode(@NotNull FriendlyByteBuf buf) {
        return new GuCondition(buf.readEnum(Stage.class), buf.readVarInt(), buf.readFloat());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull GuCondition value) {
        buf.writeEnum(value.stage());
        buf.writeVarInt(value.usesLeft());
        buf.writeFloat(value.mended());
    }

    public GuCondition {
        usesLeft = Mth.clamp(usesLeft, 0, stage.uses());
        mended = Math.max(0.0F, mended);
    }

    public static GuCondition fresh(Stage stage, float mended) { return new GuCondition(stage, stage.uses(), mended); }

    public GuCondition withUsesLeft(int value) { return new GuCondition(stage, value, mended); }

    public GuCondition withMended(float value) { return new GuCondition(stage, usesLeft, value); }

    /** 三态：正常、受伤、濒死；序列化名供指令与存档共用。 */
    public enum Stage implements StringRepresentable {
        NORMAL("normal", 0),
        INJURED("injured", 9),
        DYING("dying", 3);

        public static final Codec<Stage> CODEC = StringRepresentable.fromEnum(Stage::values);
        private final String id;
        private final int uses;

        Stage(String id, int uses) {
            this.id = id;
            this.uses = uses;
        }

        public int uses() { return uses; }

        public Stage worse() { return this == NORMAL ? INJURED : DYING; }

        public Stage better() { return this == DYING ? INJURED : NORMAL; }

        @Override
        public @NotNull String getSerializedName() { return id; }
    }
}
