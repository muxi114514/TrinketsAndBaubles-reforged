package xzeroair.trinkets.client.race;

import java.util.Map;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 玩家渲染层：按当前种族画外观特征。对应 1.12 renderLayers/TrinketsRenderLayer 的种族部分与各 RaceXxxRenderer#doRenderLayer。
 *
 * 移植说明：1.12 每个种族处理器持有一个渲染器对象（服务端类里引用客户端类）；1.20.1 改为客户端按种族注册名查表，
 * 种族处理器保持两端通用。饰品的佩戴渲染改由 Curios 渲染器负责（P6c）。
 */
@OnlyIn(Dist.CLIENT)
public class RaceTraitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** 种族注册名路径 → 特征绘制；矮人（胡子未实装）、人类、泰坦在 1.12 均无特征 */
    private static final Map<String, Consumer<TraitRenderContext>> TRAITS = Map.of(
            "fairy", WingTraits::fairy,
            "elf", EarTraits::elf,
            "goblin", EarTraits::goblin,
            "faelis", RaceTraitLayer::faelis,
            "dragon", RaceTraitLayer::dragon,
            "taurus", RaceTraitLayer::taurus);

    public RaceTraitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
            float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || player.hasEffect(MobEffects.INVISIBILITY) || !TrinketsConfig.CLIENT.render.rendering.get()) {
            return;
        }
        final EntityProperties properties = EntityProperties.get(player);
        if (properties == null) {
            return;
        }
        final EntityRacePropertiesHandler handler = properties.getRaceHandler();
        final Consumer<TraitRenderContext> traits = TRAITS.get(handler.getRace().getName().toLowerCase());
        if (traits == null || !handler.showTraits()) {
            return;
        }
        traits.accept(new TraitRenderContext(pose, buffers, light, player, this.getParentModel(), "slim".equals(player.getModelName()),
                properties.isFake(), partialTicks, limbSwing, limbSwingAmount, TraitRenderContext.Colors.of(handler.getAppearance())));
    }

    /** 耳朵 + 爪子；尾巴只在辅助变种 0 时显示，颜色规则与 1.12 RaceFaelisRenderer 相同 */
    private static void faelis(TraitRenderContext ctx) {
        EarTraits.faelis(ctx);
        ModelTraits.faelisClaws(ctx, true, true);
        final TraitRenderContext.Colors colors = ctx.colors();
        if (colors.auxVariant() == 0) {
            final int tail = colors.option() == 1 ? colors.primary() : colors.option() > 1 ? colors.secondary() : colors.aux();
            ModelTraits.faelisTail(ctx, tail, colors.secondary());
        }
    }

    /** 翅膀 + 按辅助变种选择的角 */
    private static void dragon(TraitRenderContext ctx) {
        WingTraits.dragon(ctx);
        switch (ctx.colors().auxVariant()) {
            case 0 -> ModelTraits.hornsInverted(ctx, ctx.colors().aux());
            case 1 -> ModelTraits.horns(ctx, ctx.colors().aux());
            case 2 -> ModelTraits.dragonHorns(ctx, ctx.colors().aux());
            default -> {
            }
        }
    }

    /** 变种 0 母角、1 公角；辅助变种 0 显示铃铛 */
    private static void taurus(TraitRenderContext ctx) {
        final TraitRenderContext.Colors colors = ctx.colors();
        if (colors.variant() == 0 || colors.variant() == 1) {
            ModelTraits.taurusHorns(ctx, colors.variant() == 0, colors.primary(), colors.secondary(), colors.aux());
        }
        if (colors.auxVariant() == 0) {
            ModelTraits.taurusBell(ctx, colors.aux());
        }
    }
}
