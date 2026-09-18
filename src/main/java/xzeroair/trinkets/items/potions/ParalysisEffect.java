package xzeroair.trinkets.items.potions;

import java.util.function.Consumer;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import xzeroair.trinkets.init.ModItems;
import xzeroair.trinkets.util.helpers.TrinketHelper;

/**
 * 麻痹：清零水平速度并禁止上升。
 * 移植说明：1.12 直接改 motionX/Y/Z 字段并置 velocityChanged；
 * 1.20.1 改为 setDeltaMovement + hasImpulse。
 */
public class ParalysisEffect extends MobEffect {

    public ParalysisEffect() {
        super(MobEffectCategory.HARMFUL, 12648447);
    }

    /** 佩戴电弧宝珠时不生效；效果本身由宝珠的免疫名单阻断与清理（遍历效果表期间不能移除效果） */
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (TrinketHelper.isEquipped(entity, stack -> stack.is(ModItems.ARCING_ORB.get()))) {
            return;
        }
        final Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(0.0D, Math.min(motion.y, 0.0D), 0.0D);
        entity.hasImpulse = true;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    /** 1.12 shouldRender=false：不在界面与物品栏显示图标 */
    @Override
    public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(HiddenEffectIcon.INSTANCE);
    }
}
