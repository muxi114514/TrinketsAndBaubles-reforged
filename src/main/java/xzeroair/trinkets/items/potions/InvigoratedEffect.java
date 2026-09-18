package xzeroair.trinkets.items.potions;

import java.util.function.Consumer;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

/**
 * 活力：纯标记效果，自身无 tick 逻辑，由其他系统检查其存在与否
 * （1.12 中法埃利斯的重甲惩罚在喝奶后临时解除即查此效果）。
 */
public class InvigoratedEffect extends MobEffect {

    public InvigoratedEffect() {
        super(MobEffectCategory.BENEFICIAL, 16309159);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    /** 1.12 shouldRender=false：不在界面与物品栏显示图标 */
    @Override
    public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(HiddenEffectIcon.INSTANCE);
    }
}
