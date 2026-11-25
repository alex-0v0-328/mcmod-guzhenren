package net.alex.guzhenren.gameplay.dimension;

import net.alex.guzhenren.gameplay.dimension.DimensionReturnData.ReturnPoint;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 仅服务端的维度返回附件的 NBT 编解码往返。
 */

class DimensionReturnDataTest {

    @Test
    @DisplayName("DimensionReturnData NBT codec roundtrips a present return point")
    void presentRoundTrip() {
        ResourceKey<Level> level = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("guworld", "treasure_yellow_heaven"));
        ReturnPoint point = new ReturnPoint(level, 1.5, 64.0, -2.5, 90.0F, 10.0F, true);
        DimensionReturnData expected = DimensionReturnData.of(point);

        DimensionReturnData decoded = nbtRoundTrip(expected);
        assertTrue(decoded.isPresent());
        ReturnPoint actual = decoded.point().orElseThrow();
        assertEquals(point.level(), actual.level());
        assertEquals(point.x(), actual.x(), 1.0E-6);
        assertEquals(point.y(), actual.y(), 1.0E-6);
        assertEquals(point.z(), actual.z(), 1.0E-6);
        assertEquals(point.yaw(), actual.yaw(), 1.0E-6);
        assertEquals(point.pitch(), actual.pitch(), 1.0E-6);
        assertEquals(point.flying(), actual.flying());
    }

    @Test
    @DisplayName("A return point saved with the retired mayfly key still decodes")
    void legacyMayflyKeyIgnored() {
        CompoundTag point = new CompoundTag();
        point.putString("level", "guworld:treasure_yellow_heaven");
        point.putDouble("x", 1.5);
        point.putDouble("y", 64.0);
        point.putDouble("z", -2.5);
        point.putFloat("yaw", 90.0F);
        point.putFloat("pitch", 10.0F);
        point.putBoolean("mayfly", true);
        point.putBoolean("flying", false);
        CompoundTag root = new CompoundTag();
        root.put("point", point);

        DimensionReturnData decoded = DimensionReturnData.CODEC.parse(NbtOps.INSTANCE, root).getOrThrow();
        assertTrue(decoded.isPresent());
        assertFalse(decoded.point().orElseThrow().flying());
    }

    @Test
    @DisplayName("DimensionReturnData default (empty) roundtrips through its codec")
    void emptyRoundTrip() {
        DimensionReturnData decoded = nbtRoundTrip(DimensionReturnData.DEFAULT);
        assertFalse(decoded.isPresent());
        assertEquals(DimensionReturnData.DEFAULT, decoded);
    }

    private static DimensionReturnData nbtRoundTrip(DimensionReturnData data) {
        Tag tag = DimensionReturnData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        return DimensionReturnData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }
}
