package net.alex.guzhenren.network;

import io.netty.handler.codec.DecoderException;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.network.payload.ImpactShakePayload;
import net.alex.guzhenren.network.payload.NourishAction;
import net.alex.guzhenren.network.payload.NourishAperturePayload;
import net.alex.guzhenren.network.payload.SetSecondaryPathPayload;
import net.alex.guzhenren.network.payload.SkinFlashPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static net.alex.guzhenren.TestBuffers.withBuffer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayloadDecodeTest {

    @Test
    @DisplayName("a forged secondary-path ordinal fails to decode instead of indexing past GuPath")
    void forgedSecondaryPath() {
        withBuffer(buffer -> {
            buffer.writeVarInt(0);
            buffer.writeVarInt(GuPath.values().length + 1);
            assertThrows(DecoderException.class, () -> SetSecondaryPathPayload.STREAM_CODEC.decode(buffer));
            buffer.clear();

            SetSecondaryPathPayload valid = new SetSecondaryPathPayload(1, GuPath.TIME);
            SetSecondaryPathPayload.STREAM_CODEC.encode(buffer, valid);
            assertEquals(valid, SetSecondaryPathPayload.STREAM_CODEC.decode(buffer));
        });
    }

    @Test
    @DisplayName("a forged nourish action ordinal fails to decode")
    void forgedNourishAction() {
        withBuffer(buffer -> {
            buffer.writeVarInt(NourishAction.values().length);
            buffer.writeVarInt(0);
            assertThrows(DecoderException.class, () -> NourishAperturePayload.STREAM_CODEC.decode(buffer));
            buffer.clear();

            NourishAperturePayload valid = new NourishAperturePayload(NourishAction.CANCEL, 0);
            NourishAperturePayload.STREAM_CODEC.encode(buffer, valid);
            assertEquals(valid, NourishAperturePayload.STREAM_CODEC.decode(buffer));
        });
    }

    @Test
    @DisplayName("the skin flash notice round-trips and a forged skin ordinal fails to decode")
    void skinFlashRoundTrip() {
        withBuffer(buffer -> {
            SkinFlashPayload valid = new SkinFlashPayload(42, Skin.WHITE_JADE_SKIN);
            SkinFlashPayload.STREAM_CODEC.encode(buffer, valid);
            assertEquals(valid, SkinFlashPayload.STREAM_CODEC.decode(buffer));
            buffer.clear();
            buffer.writeVarInt(42);
            buffer.writeVarInt(Skin.values().length);
            assertThrows(DecoderException.class, () -> SkinFlashPayload.STREAM_CODEC.decode(buffer));
        });
    }

    @Test
    @DisplayName("the impact shake notice round-trips")
    void impactShakeRoundTrip() {
        withBuffer(buffer -> {
            ImpactShakePayload valid = new ImpactShakePayload(0.45F, 15);
            ImpactShakePayload.STREAM_CODEC.encode(buffer, valid);
            assertEquals(valid, ImpactShakePayload.STREAM_CODEC.decode(buffer));
        });
    }
}
