package net.alex.guzhenren;

import io.netty.handler.codec.DecoderException;
import java.util.Set;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.gameplay.body.Anatomy;
import net.alex.guzhenren.gameplay.body.Appendage;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyPart;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Flesh;
import net.alex.guzhenren.gameplay.body.Injury;
import net.alex.guzhenren.gameplay.body.Organ;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.gameplay.body.Race;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.path.GuPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static net.alex.guzhenren.TestBuffers.withBuffer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataStreamCodecTest {

    @Test
    @DisplayName("Aperture stream codec preserves nullable paths, the second flag and all sentinels")
    void apertureRoundTrip() {
        Aperture expected = new Aperture(Rank.THREE, Stage.UPPER, 83, 12_345L, GuPath.TIME, null, 678L, 73,
                987_654L, 42, true, true, true);
        withBuffer(buffer -> {
            Aperture.encode(buffer, expected);
            assertEquals(expected.rank(), ModByteBufs.readEnum(buffer, Rank.class));
            assertEquals(expected.stage(), ModByteBufs.readEnum(buffer, Stage.class));
            assertEquals(expected.baseEssence(), buffer.readVarInt());
            assertEquals(expected.currentEssence(), buffer.readVarLong());
            assertEquals(expected.primaryPath(), ModByteBufs.readNullableEnum(buffer, GuPath.class));
            assertEquals(expected.secondaryPath(), ModByteBufs.readNullableEnum(buffer, GuPath.class));
            assertEquals(expected.distilledEssence(), buffer.readVarLong());
            assertEquals(expected.pressure(), buffer.readVarInt());
            assertEquals(expected.pressureDeadlineTick(), buffer.readVarLong());
            assertEquals(expected.nourishProgress(), buffer.readVarInt());
            assertEquals(expected.petrified(), buffer.readBoolean());
            assertEquals(expected.distilling(), buffer.readBoolean());
            assertEquals(expected.second(), buffer.readBoolean());
            assertEquals(0, buffer.readableBytes());

            ModByteBufs.writeEnum(buffer, expected.rank());
            ModByteBufs.writeEnum(buffer, expected.stage());
            buffer.writeVarInt(expected.baseEssence());
            buffer.writeVarLong(expected.currentEssence());
            ModByteBufs.writeNullableEnum(buffer, expected.primaryPath());
            ModByteBufs.writeNullableEnum(buffer, expected.secondaryPath());
            buffer.writeVarLong(expected.distilledEssence());
            buffer.writeVarInt(expected.pressure());
            buffer.writeVarLong(expected.pressureDeadlineTick());
            buffer.writeVarInt(expected.nourishProgress());
            buffer.writeBoolean(expected.petrified());
            buffer.writeBoolean(expected.distilling());
            buffer.writeBoolean(expected.second());
            assertEquals(expected, Aperture.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        });
    }

    @Test
    @DisplayName("Enum codecs reject out-of-range and negative ordinals as malformed packets")
    void enumCodecsRejectOutOfRangeOrdinals() {
        withBuffer(buffer -> {
            buffer.writeVarInt(Rank.values().length);
            assertThrows(DecoderException.class, () -> ModByteBufs.readEnum(buffer, Rank.class));
            buffer.clear();
            buffer.writeVarInt(-1);
            assertThrows(DecoderException.class, () -> ModByteBufs.readEnum(buffer, Rank.class));
            buffer.clear();
            buffer.writeVarInt(GuPath.values().length + 1);
            assertThrows(DecoderException.class, () -> ModByteBufs.readNullableEnum(buffer, GuPath.class));
            buffer.clear();
            buffer.writeVarInt(-1);
            assertThrows(DecoderException.class, () -> ModByteBufs.readNullableEnum(buffer, GuPath.class));
            buffer.clear();

            GuPath last = GuPath.values()[GuPath.values().length - 1];
            buffer.writeVarInt(GuPath.values().length);
            assertEquals(last, ModByteBufs.readNullableEnum(buffer, GuPath.class));
        });
    }

    @Test
    @DisplayName("BodyData stream codec preserves anchors, zombie tier and anatomy")
    void bodyRoundTrip() {
        Anatomy anatomy = Anatomy.DEFAULT.withInjury(BodyPart.TORSO, Injury.MODERATE)
                .withInjury(Organ.MOUTH, Injury.SEVERE).withAppendage(Appendage.HORNS, true);
        BodyData expected = new BodyData(Set.of(Physique.TEMP_ZOMBIE, Physique.EXTREME),
                ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL, Race.DRAGONMEN, 123L, 456L, 789L, 654L, 4, 987L,
                Skin.JADE_SKIN, 1_234L, Tendon.STEEL_TENDON_4, Bone.IRON_BONE_3, Flesh.NATIVE, anatomy);
        withBuffer(buffer -> {
            BodyData.encode(buffer, expected);
            assertEquals(expected.physiques(), ModByteBufs.readEnumSet(buffer, Physique.class));
            assertEquals(expected.extremePhysique(), ModByteBufs.readEnum(buffer, ExtremePhysique.class));
            assertEquals(expected.race(), ModByteBufs.readEnum(buffer, Race.class));
            assertEquals(expected.ageParts(), buffer.readVarLong());
            assertEquals(expected.lifespanParts(), buffer.readVarLong());
            assertEquals(expected.lastDayIndex(), buffer.readVarLong());
            assertEquals(expected.tempZombieEndTick(), buffer.readVarLong());
            assertEquals(expected.zombieTier(), buffer.readVarInt());
            assertEquals(expected.lastBilledTick(), buffer.readVarLong());
            assertEquals(expected.skin(), ModByteBufs.readEnum(buffer, Skin.class));
            assertEquals(expected.skinEndTick(), buffer.readVarLong());
            assertEquals(expected.tendon(), ModByteBufs.readEnum(buffer, Tendon.class));
            assertEquals(expected.bone(), ModByteBufs.readEnum(buffer, Bone.class));
            assertEquals(expected.flesh(), ModByteBufs.readEnum(buffer, Flesh.class));
            for (BodyPart part : BodyPart.values()) {
                assertEquals(anatomy.getInjury(part), ModByteBufs.readEnum(buffer, Injury.class));
            }
            for (Organ organ : Organ.values()) {
                assertEquals(anatomy.getInjury(organ), ModByteBufs.readEnum(buffer, Injury.class));
            }
            assertEquals(anatomy.appendages(), ModByteBufs.readEnumSet(buffer, Appendage.class));
            assertEquals(0, buffer.readableBytes());

            ModByteBufs.writeEnumSet(buffer, expected.physiques());
            ModByteBufs.writeEnum(buffer, expected.extremePhysique());
            ModByteBufs.writeEnum(buffer, expected.race());
            buffer.writeVarLong(expected.ageParts());
            buffer.writeVarLong(expected.lifespanParts());
            buffer.writeVarLong(expected.lastDayIndex());
            buffer.writeVarLong(expected.tempZombieEndTick());
            buffer.writeVarInt(expected.zombieTier());
            buffer.writeVarLong(expected.lastBilledTick());
            ModByteBufs.writeEnum(buffer, expected.skin());
            buffer.writeVarLong(expected.skinEndTick());
            ModByteBufs.writeEnum(buffer, expected.tendon());
            ModByteBufs.writeEnum(buffer, expected.bone());
            ModByteBufs.writeEnum(buffer, expected.flesh());
            Anatomy.encode(buffer, anatomy);
            assertEquals(expected, BodyData.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        });
    }
}
