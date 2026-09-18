package xzeroair.trinkets.client.race;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.races.RaceAppearance;

/**
 * 一次种族特征绘制所需的全部输入。对应 1.12 IRenderModelInterface#render 的一长串参数。
 *
 * @param fake  当前形态来自饰品/药水（1.12 isFake，妖精翅膀据此换样式）
 * @param colors 已按「配色方案」换算好的主/副/辅颜色
 */
@OnlyIn(Dist.CLIENT)
public record TraitRenderContext(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
        PlayerModel<AbstractClientPlayer> model, boolean slim, boolean fake, float partialTicks, float limbSwing,
        float limbSwingAmount, Colors colors) {

    public boolean wearing(EquipmentSlot slot) {
        return !this.player.getItemBySlot(slot).isEmpty();
    }

    /**
     * 特征颜色与变种。换算规则照搬 1.12 各种族渲染器：
     * 配色方案 1 时主色取副色；方案大于 0 时副色取主色。
     */
    public record Colors(int primary, int secondary, int aux, int variant, int auxVariant, int option) {

        public static Colors of(RaceAppearance appearance) {
            final int option = appearance.getColorOption();
            final int primary = option == 1 ? appearance.getSecondaryColor() : appearance.getPrimaryColor();
            final int secondary = option > 0 ? appearance.getPrimaryColor() : appearance.getSecondaryColor();
            return new Colors(primary, secondary, appearance.getAuxColor(), appearance.getVariant(), appearance.getAuxVariant(), option);
        }

        /** 装饰品没有种族外观数据时使用的默认颜色（白色，与 1.12 未初始化能力时一致） */
        public static Colors defaults() {
            return new Colors(0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0, 0, 0);
        }
    }
}
