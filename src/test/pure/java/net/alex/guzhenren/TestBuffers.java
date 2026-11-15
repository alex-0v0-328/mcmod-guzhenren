package net.alex.guzhenren;

import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;

/** 流编解码测试的数据包缓冲区：每个新建、不池化，主体结束即释放。 */
public final class TestBuffers {

    private TestBuffers() {}

    public static void withBuffer(Consumer<FriendlyByteBuf> body) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            body.accept(buffer);
        } finally {
            buffer.release();
        }
    }
}
