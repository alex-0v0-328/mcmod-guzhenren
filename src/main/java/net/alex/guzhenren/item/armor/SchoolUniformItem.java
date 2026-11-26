package net.alex.guzhenren.item.armor;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 学堂制服[School Uniform]三槽（胸甲、护腿、布鞋）GeckoLib 静态护甲物品。
 * <ul>
 *   <li>胸甲与护腿支持 16 色古风重映射换色（{@link #isDyeable} 为 {@code true}），通过工作台配原版染料切色或在水
 *       炼药锅洗回默认玄墨炭黑；布鞋固定黑面白底，不可染色。</li>
 *   <li>静态盔甲无独立骨骼动画，仅随穿戴者原版人形骨骼联动；客户端渲染器经 {@link #renderOnClient} 在客户端初始化
 *       时注入，避免公共物品包反向依赖客户端类。</li>
 * </ul>
 */

public class SchoolUniformItem extends ArmorItem implements GeoItem {

    private static final ResourceLocation LAYER_TEXTURE = Guzhenren.id("textures/armor/school_uniform_layer.png");
    private static final ResourceLocation BASE_TEXTURE = Guzhenren.id("textures/armor/school_uniform_base.png");
    private static Supplier<GeoRenderProvider> clientRenderProvider = () -> GeoRenderProvider.DEFAULT;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final boolean dyeable;

    public SchoolUniformItem(Holder<ArmorMaterial> material, Type type, Properties properties, boolean dyeable) {
        super(material, type, properties);
        this.dyeable = dyeable;
    }

    public static void renderOnClient(Supplier<GeoRenderProvider> provider) {
        clientRenderProvider = Objects.requireNonNull(provider);
    }

    public boolean isDyeable() {
        return this.dyeable;
    }

    @Override
    public @NotNull ResourceLocation getArmorTexture(@NotNull ItemStack stack, @NotNull Entity entity,
                                                     @NotNull EquipmentSlot slot, ArmorMaterial.@NotNull Layer layer,
                                                     boolean innerModel) {
        return layer.dyeable() ? LAYER_TEXTURE : BASE_TEXTURE;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!this.dyeable) return;
        DyeColor color = UniformColor.getColor(stack);
        tooltip.add(Component.translatable("tooltip.guzhenren.uniform_color",
                Component.translatable(UniformColor.translationKey(color))
                        .withStyle(style -> style.withColor(UniformColor.rgb(color))))
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(clientRenderProvider.get());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
