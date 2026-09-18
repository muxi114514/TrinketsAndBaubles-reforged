package xzeroair.trinkets.client.curios;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.client.race.EarTraits;
import xzeroair.trinkets.client.race.ModelTraits;
import xzeroair.trinkets.client.race.TraitRenderContext;
import xzeroair.trinkets.client.race.WingTraits;
import xzeroair.trinkets.items.trinkets.TrinketCosmetic.CosmeticFeature;

/**
 * 装饰品各特征的绘制映射。对应 1.12 TrinketCosmetic#playerRenderLayer 的 switch。
 * 种族特征在玩家已是该种族时跳过（种族层已画过）；角类与尾巴等通用特征总是绘制。
 * 1.12 的人类、矮人胡子（空实现）、泰坦特征本就不画。
 */
@OnlyIn(Dist.CLIENT)
final class CosmeticTraits {

    static void render(CosmeticFeature feature, TraitRenderContext ctx) {
        final TraitRenderContext.Colors colors = ctx.colors();
        switch (feature) {
            case FAIRY -> {
                if (!AccessoryRenderers.isRace(ctx.player(), "fairy")) {
                    WingTraits.fairy(ctx);
                }
            }
            case ELF -> {
                if (!AccessoryRenderers.isRace(ctx.player(), "elf")) {
                    EarTraits.elf(ctx);
                }
            }
            case GOBLIN -> {
                if (!AccessoryRenderers.isRace(ctx.player(), "goblin")) {
                    EarTraits.goblin(ctx);
                }
            }
            case FAELIS -> {
                if (!AccessoryRenderers.isRace(ctx.player(), "faelis")) {
                    EarTraits.faelis(ctx);
                    ModelTraits.faelisClaws(ctx, true, true);
                }
            }
            case DRAGON -> {
                if (!AccessoryRenderers.isRace(ctx.player(), "dragon")) {
                    WingTraits.dragon(ctx);
                }
            }
            case TAURUS, TAURUS_BELL, TAURUS_F, TAURUS_F_BELL, TAURIAN_BELL -> taurus(feature, ctx, colors);
            case SUCCUBUS -> ModelTraits.succubusHorns(ctx, colors.primary(), colors.secondary(), colors.aux());
            case GENERIC_HORNS -> ModelTraits.horns(ctx, colors.aux());
            case GENERIC_HORNS_INVERTED -> ModelTraits.hornsInverted(ctx, colors.aux());
            case DRAGON_HORNS -> ModelTraits.dragonHorns(ctx, colors.aux());
            case FAELIS_TAIL -> ModelTraits.faelisTail(ctx, colors.aux(), colors.aux());
            default -> {
            }
        }
    }

    private static void taurus(CosmeticFeature feature, TraitRenderContext ctx, TraitRenderContext.Colors colors) {
        if (AccessoryRenderers.isRace(ctx.player(), "taurus")) {
            return;
        }
        switch (feature) {
            case TAURUS -> ModelTraits.taurusHorns(ctx, false, colors.primary(), colors.secondary(), colors.aux());
            case TAURUS_BELL -> {
                ModelTraits.taurusHorns(ctx, false, colors.primary(), colors.secondary(), colors.aux());
                ModelTraits.taurusBell(ctx, colors.aux());
            }
            case TAURUS_F -> ModelTraits.taurusHorns(ctx, true, colors.primary(), colors.secondary(), colors.aux());
            case TAURUS_F_BELL -> {
                ModelTraits.taurusHorns(ctx, true, colors.primary(), colors.secondary(), colors.aux());
                ModelTraits.taurusBell(ctx, colors.aux());
            }
            case TAURIAN_BELL -> ModelTraits.taurusBell(ctx, colors.aux());
            default -> {
            }
        }
    }

    private CosmeticTraits() {
    }
}
