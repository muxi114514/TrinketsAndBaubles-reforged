package xzeroair.trinkets.items.potions;

import java.util.function.Consumer;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import xzeroair.trinkets.init.ModDamageTypes;
import xzeroair.trinkets.init.ModItems;
import xzeroair.trinkets.util.helpers.TrinketHelper;

/**
 * 流血：每秒造成 0.5 × (等级+1) 点伤害。
 * 移植说明：1.12 的 isReady 恒 true、自己在 performEffect 里按 ticksExisted % 20 节流，此处照搬该形状。
 */
public class BleedEffect extends MobEffect {

    /** 每秒伤害 = 本值 × 等级 */
    public static final float DAMAGE_PER_LEVEL = 0.5F;

    public BleedEffect() {
        super(MobEffectCategory.HARMFUL, 8912896);
    }

    /**
     * 佩戴法埃利斯之爪时不结算伤害；效果本身由饰品的免疫名单阻断与清理
     * （1.12 在这里直接 removePotionEffect，但此时正在遍历效果表，1.20.1 会抛并发修改异常）。
     */
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (TrinketHelper.isEquipped(entity, stack -> stack.is(ModItems.FAELIS_CLAW.get()))) {
            return;
        }
        if (!entity.level().isClientSide && (entity.tickCount % 20) == 0) {
            entity.hurt(ModDamageTypes.source(entity.level(), ModDamageTypes.BLEED), DAMAGE_PER_LEVEL * (amplifier + 1));
        }
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
