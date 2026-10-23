package net.alex.guzhenren.core;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

/**
 * A value written to and read from NBT through its {@link Codec}, a failure logged rather than thrown.
 *
 * <p>Each call names what it does and to which key ({@code "save player data"}, {@code "aperture_data"}), and a
 * failure logs {@code Cannot <action> <key>: <error>} and yields nothing; the caller decides the fallback.
 *
 * <p>⚠ Forge 1.20.1 only: the port keeps player data and per-stack data as NBT where the NeoForge 1.21.1 mainline
 * has attachments and data components, so the mainline has no counterpart of this file.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class NbtCodecs {

    private NbtCodecs() {}

    public static <T> @NotNull Optional<Tag> encode(@NotNull Codec<T> codec, @NotNull T value,
                                                    @NotNull String action, @NotNull String key) {
        return codec.encodeStart(NbtOps.INSTANCE, value)
                .resultOrPartial(error -> Guzhenren.LOGGER.error("Cannot {} {}: {}", action, key, error));
    }

    public static <T> @NotNull Optional<T> decode(@NotNull Codec<T> codec, @NotNull Tag encoded,
                                                  @NotNull String action, @NotNull String key) {
        return codec.parse(NbtOps.INSTANCE, encoded)
                .resultOrPartial(error -> Guzhenren.LOGGER.error("Cannot {} {}: {}", action, key, error));
    }
}
