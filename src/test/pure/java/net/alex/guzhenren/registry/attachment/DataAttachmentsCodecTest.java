package net.alex.guzhenren.registry.attachment;

import java.util.Map;
import java.util.Set;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.mind.Brilliance;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.mind.MindPool;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import net.alex.guzhenren.gameplay.path.GuAttainment;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathData;
import net.alex.guzhenren.gameplay.path.PathEntry;
import net.alex.guzhenren.gameplay.path.PathMarks;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static net.alex.guzhenren.TestBuffers.withBuffer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DataStreamCodecTest 未覆盖的记录附件流编解码往返：编码钉住实例，经独立重建编解码器逐字段解码再编码，断言整记录相等、
 * 缓冲区读空；空窍存储与附件默认值见 {@code DataAttachmentsCodecGameTests}（物品堆需注册表）。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

class DataAttachmentsCodecTest {
    @Test
    @DisplayName("ApertureNourishData stream codec preserves the running flag, target and starve anchor")
    void nourishRoundTrip() {
        ApertureNourishData expected = new ApertureNourishData(true, 1, 12345L);
        withBuffer(buffer -> {
            ApertureNourishData.encode(buffer, expected);
            assertEquals(expected.cultivating(), buffer.readBoolean());
            assertEquals(expected.target(), buffer.readVarInt());
            assertEquals(expected.starvedSinceTick(), buffer.readVarLong());
            assertEquals(0, buffer.readableBytes());
            ApertureNourishData.encode(buffer, expected);
            assertEquals(expected, ApertureNourishData.decode(buffer));
            assertEquals(0, buffer.readableBytes());
            ApertureNourishData.encode(buffer, new ApertureNourishData(false, 0, -1L));
            assertFalse(buffer.readBoolean());
            assertEquals(0, buffer.readVarInt());
            assertEquals(-1L, buffer.readVarLong());
            assertEquals(0, buffer.readableBytes());
        });
    }

    @Test
    @DisplayName("SoulData stream codec roundtrips and the compact ctor clamps current into [0, max]")
    void soulRoundTrip() {
        SoulData expected = new SoulData(50L, 999L);
        assertEquals(50L, expected.maxSoul());
        assertEquals(50L, expected.currentSoul(), "current 999 clamps down to max");
        withBuffer(buffer -> {
            SoulData.encode(buffer, expected);
            assertEquals(expected.maxSoul(), buffer.readVarLong());
            assertEquals(expected.currentSoul(), buffer.readVarLong());
            assertEquals(0, buffer.readableBytes());
            SoulData.encode(buffer, expected);
            assertEquals(expected, SoulData.decode(buffer));
            assertEquals(0, buffer.readableBytes());
            SoulData collapsed = new SoulData(0L, 0L);
            SoulData.encode(buffer, collapsed);
            assertEquals(collapsed, SoulData.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        });
    }

    @Test
    @DisplayName("PathData stream codec roundtrips entries and keeps non-default ones only")
    void pathDataRoundTrip() {
        PathData expected = new PathData(Map.of(
                GuPath.TIME, new PathEntry(GuAttainment.GRANDMASTER, new PathMarks(1000L, 3L)),
                GuPath.WISDOM, new PathEntry(GuAttainment.QUASI_MASTER, PathMarks.NONE)));
        withBuffer(buffer -> {
            PathData.encode(buffer, expected);
            Map<GuPath, PathEntry> decoded = ModByteBufs.readEnumMap(buffer, GuPath.class, PathEntry::decode);
            assertEquals(expected.entries(), decoded);
            assertEquals(2, decoded.size());
            assertEquals(1000L, decoded.get(GuPath.TIME).marks().marks());
            assertEquals(GuAttainment.QUASI_MASTER, decoded.get(GuPath.WISDOM).attainment());
            assertEquals(0, buffer.readableBytes());
            PathData.encode(buffer, expected);
            assertEquals(expected, PathData.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        });
    }

    @Test
    @DisplayName("PathStrengthData caps the human layers it is built with")
    void pathStrengthCapsLayers() {
        PathStrengthData clamped = new PathStrengthData(Set.of(), Map.of(HumanStrength.JIN, 999));
        assertEquals(9, clamped.getHumanStrengthCount(HumanStrength.JIN));
    }

    @Test
    @DisplayName("MindPool stream codec roundtrips and latches the overflow flag")
    void mindPoolRoundTrip() {
        MindPool expected = new MindPool(30L, 50000L, false);
        withBuffer(buffer -> {
            MindPool.encode(buffer, expected);
            assertEquals(expected.current(), buffer.readVarLong());
            assertEquals(expected.max(), buffer.readVarLong());
            assertEquals(expected.bufferUsed(), buffer.readBoolean());
            assertEquals(0, buffer.readableBytes());
            MindPool.encode(buffer, expected);
            assertEquals(expected, MindPool.decode(buffer));
            assertEquals(0, buffer.readableBytes());
            MindPool overflowing = new MindPool(65000L, 50000L, false);
            assertTrue(overflowing.bufferUsed());
            MindPool.encode(buffer, overflowing);
            assertEquals(65000L, buffer.readVarLong());
            assertEquals(50000L, buffer.readVarLong());
            assertTrue(buffer.readBoolean());
            assertEquals(0, buffer.readableBytes());
        });
    }

    @Test
    @DisplayName("MindData stream codec roundtrips brilliance, dense pools and tagged thoughts")
    void mindRoundTrip() {
        MindData expected = new MindData(Brilliance.OUTSTANDING,
                Map.of(MindPoolType.THOUGHTS, new MindPool(100L, 50000L, false)), Map.of(ThoughtTag.EVIL, 30L));
        withBuffer(buffer -> {
            MindData.encode(buffer, expected);
            assertEquals(expected.brilliance(), ModByteBufs.readEnum(buffer, Brilliance.class));
            assertEquals(expected.pools(), ModByteBufs.readEnumMap(buffer, MindPoolType.class, MindPool::decode));
            assertEquals(expected.taggedThoughts(),
                    ModByteBufs.readEnumMap(buffer, ThoughtTag.class, FriendlyByteBuf::readVarLong));
            assertEquals(0, buffer.readableBytes());
            MindData.encode(buffer, expected);
            assertEquals(expected, MindData.decode(buffer));
            assertEquals(0, buffer.readableBytes());
            assertEquals(3, expected.pools().size());
            assertEquals(new MindPool(0L, 12L, false), expected.getPool(MindPoolType.WILLS));
        });
    }
}
