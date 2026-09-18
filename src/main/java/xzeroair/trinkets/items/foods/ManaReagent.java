package xzeroair.trinkets.items.foods;

import javax.annotation.Nonnull;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.items.base.FoodBase;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 魔力试剂：食用后额外魔力上限点数 −1；配置开启时附带中毒与虚弱。对应 1.12 items/foods/Mana_Reagent。
 * 回复魔力由回复名单处理（默认 100%）。
 */
public class ManaReagent extends FoodBase {

    /** 副作用：中毒 II 与虚弱 II 的持续时间 */
    public static final int POISON_TICKS = 300;
    public static final int WEAKNESS_TICKS = 600;
    public static final int HARM_AMPLIFIER = 1;

    public ManaReagent() {
        super(FoodBase.props(2, 1F, true), 32, UseAnim.EAT);
    }

    @Nonnull
    @Override
    public ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity entity) {
        if (!level.isClientSide) {
            final MagicStats magic = MagicStats.get(entity);
            if (magic != null) {
                magic.setBonusMana(magic.getBonusMana() - 1);
            }
            inflictHarm(entity, POISON_TICKS);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    /** 试剂与恢复血清共用的副作用：只在配置开启且身上尚无对应效果时施加（与 1.12 一致） */
    static void inflictHarm(LivingEntity entity, int poisonDuration) {
        if (!TrinketsConfig.SERVER.magic.reagentHarmful.get()) {
            return;
        }
        addIfAbsent(entity, MobEffects.POISON, poisonDuration);
        addIfAbsent(entity, MobEffects.WEAKNESS, WEAKNESS_TICKS);
    }

    private static void addIfAbsent(LivingEntity entity, MobEffect effect, int duration) {
        if (!entity.hasEffect(effect)) {
            entity.addEffect(new MobEffectInstance(effect, duration, HARM_AMPLIFIER, false, false));
        }
    }
}
