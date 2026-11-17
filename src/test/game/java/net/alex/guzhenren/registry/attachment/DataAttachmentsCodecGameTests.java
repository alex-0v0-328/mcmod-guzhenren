package net.alex.guzhenren.registry.attachment;

import io.netty.buffer.Unpooled;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.path.PathData;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 需物品注册表的附件编解码往返：空窍存储经 NBT {@code CODEC} 在普通 {@code NbtOps} 往返（只序列化不同步，无流编解码
 * 器）；各附件默认值经自身编解码器往返；其余记录流往返见 {@code DataAttachmentsCodecTest}。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DataAttachmentsCodecGameTests {

    private DataAttachmentsCodecGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void apertureStorageKeepsInteriorHoles(GameTestHelper helper) {
        ApertureStorage expected = new ApertureStorage(
                List.of(List.of(new ItemStack(Items.DIRT, 3), ItemStack.EMPTY, new ItemStack(Items.STONE)),
                        List.of(new ItemStack(Items.APPLE, 2))),
                List.of(new ItemStack(Items.DIRT)));
        ApertureStorage decoded = nbtRoundTrip(expected);
        assertSameContents(expected, decoded);
        assertEquals(2, decoded.byAperture().size());
        assertEquals(3, decoded.get(0).size());
        assertTrue(decoded.get(0).get(1).isEmpty());
        assertEquals(1, decoded.get(1).size());
        assertEquals(2, decoded.get(1).getFirst().getCount());
        assertEquals(1, decoded.getVital(0).getCount());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void apertureStorageTrimsTrailingHoles(GameTestHelper helper) {
        ApertureStorage trimmed = new ApertureStorage(
                List.of(List.of(new ItemStack(Items.DIRT)), List.of(), List.of(ItemStack.EMPTY)), List.of());
        assertEquals(1, trimmed.byAperture().size());
        ApertureStorage decoded = nbtRoundTrip(trimmed);
        assertSameContents(trimmed, decoded);
        assertEquals(1, decoded.byAperture().size());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void attachmentDefaultsRoundTripThroughTheirCodecs(GameTestHelper helper) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ApertureNourishData.encode(buffer, ApertureNourishData.DEFAULT);
            assertEquals(ApertureNourishData.DEFAULT, ApertureNourishData.decode(buffer));
            SoulData.encode(buffer, SoulData.DEFAULT);
            assertEquals(SoulData.DEFAULT, SoulData.decode(buffer));
            PathData.encode(buffer, PathData.DEFAULT);
            assertEquals(PathData.DEFAULT, PathData.decode(buffer));
            MindData.encode(buffer, MindData.DEFAULT);
            assertEquals(MindData.DEFAULT, MindData.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
        assertEquals(ApertureStorage.DEFAULT, nbtRoundTrip(ApertureStorage.DEFAULT));
        helper.succeed();
    }

    private static ApertureStorage nbtRoundTrip(ApertureStorage storage) {
        Tag tag = ApertureStorage.CODEC.encodeStart(NbtOps.INSTANCE, storage).getOrThrow();
        return ApertureStorage.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    private static void assertSameContents(ApertureStorage expected, ApertureStorage actual) {
        assertEquals(expected.byAperture().size(), actual.byAperture().size());
        for (int i = 0; i < expected.byAperture().size(); i++) {
            int aperture = i;
            List<ItemStack> wanted = expected.byAperture().get(i);
            List<ItemStack> stored = actual.byAperture().get(i);
            assertEquals(wanted.size(), stored.size(), () -> "page size differs at aperture " + aperture);
            for (int j = 0; j < wanted.size(); j++) {
                int slot = j;
                assertTrue(ItemStack.matches(wanted.get(j), stored.get(j)),
                        () -> "stack differs at aperture " + aperture + " slot " + slot);
            }
        }
        assertEquals(expected.vital().size(), actual.vital().size());
        for (int i = 0; i < expected.vital().size(); i++) {
            int aperture = i;
            assertTrue(ItemStack.matches(expected.vital().get(i), actual.vital().get(i)),
                    () -> "vital stack differs at aperture " + aperture);
        }
    }
}
