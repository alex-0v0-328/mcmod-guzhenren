package net.alex.guzhenren.registry.capability;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The capabilities this mod attaches: {@link #PLAYER_DATA} on every player.
 *
 * <p>Registers the capability type on the mod bus and attaches a fresh {@link PlayerData} to each {@link
 * Player} as it is constructed, on both sides and for fake players too, so {@link PlayerData#get} never
 * misses on a real or simulated player. The provider saves through {@link PlayerData#save}.
 *
 * <p>⚠ The provider's {@link LazyOptional} is invalidated with the entity; a {@code PlayerEvent.Clone}
 * handler must {@code reviveCaps()} the original before reading it ({@code PlayerDataEvents}).
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModCapabilities {

    private ModCapabilities() {}

    public static final Capability<PlayerData> PLAYER_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation PLAYER_DATA_ID = Guzhenren.id("player_data");

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModCapabilities::onRegister);
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, ModCapabilities::onAttach);
    }

    private static void onRegister(RegisterCapabilitiesEvent event) {
        event.register(PlayerData.class);
    }

    private static void onAttach(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player player)) return;
        PlayerDataProvider provider = new PlayerDataProvider(new PlayerData(player));
        event.addCapability(PLAYER_DATA_ID, provider);
        event.addListener(provider.holder::invalidate);
    }

    private static final class PlayerDataProvider implements ICapabilitySerializable<CompoundTag> {

        private final PlayerData data;
        private final LazyOptional<PlayerData> holder;

        private PlayerDataProvider(PlayerData data) {
            this.data = data;
            this.holder = LazyOptional.of(() -> data);
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return PLAYER_DATA.orEmpty(cap, holder);
        }

        @Override
        public CompoundTag serializeNBT() { return data.save(); }

        @Override
        public void deserializeNBT(CompoundTag tag) { data.load(tag); }
    }
}
