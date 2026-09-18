package xzeroair.trinkets.traits.abilities;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IPotionAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.config.abilities.SafeGuardAbilityConfig;

/**
 * 安全守护：常驻一个效果（默认抗性提升）；每累计 maxHits 次受击后，下一次足够强的攻击被完全挡下；
 * 爆炸伤害按倍率削减；再次获得同种效果时可在常驻等级上叠加一级。
 *
 * 移植说明：
 * - 间接伤害（投射物/爆炸/魔法）在 LivingAttack 阶段计数并取消，直接伤害在 LivingHurt 阶段计数并归零，与 1.12 一致。
 * - 1.12 挡下攻击时的提示语来自 VIP 名单（联网拉取贡献者语录），该名单不移植，统一提示 "Ow!"。
 * - 移除能力时只清除本能力给的短时效果（≤ 200 tick），不误删玩家自己喝的药水。
 */
public class AbilitySafeGuard extends Ability implements ITickableAbility, IAttackAbility, IPotionAbility {

    private static final String COUNT_TAG = "COUNT";
    private static final int EFFECT_DURATION = 200;
    private static final int SOUND_COOLDOWN_TICKS = 20;

    private final SafeGuardAbilityConfig config;
    private int hitCount;
    private int lastSoundTick = -SOUND_COOLDOWN_TICKS;

    public AbilitySafeGuard(@Nonnull SafeGuardAbilityConfig config) {
        super(AbilityNames.SAFE_GUARD);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    public int getHitCount() {
        return this.hitCount;
    }

    public void setHitCount(int hitCount) {
        if (this.hitCount != hitCount) {
            this.hitCount = hitCount;
            this.setChanged(true);
        }
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }
        final MobEffect effect = this.effect();
        if (!entity.hasEffect(effect)) {
            entity.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, this.config.effectLevel.get(), false, false));
        }
    }

    @Override
    public boolean attacked(LivingEntity attacked, DamageSource source, float dmg, boolean cancel) {
        if (!attacked.level().isClientSide && dmg >= this.config.minDamageToCount.get() && this.isIndirectDamage(source)) {
            this.setHitCount(this.hitCount + 1);
            if (dmg >= this.config.minDamageToTrigger.get() && this.trigger(attacked)) {
                return true;
            }
        }
        return cancel;
    }

    @Override
    public float hurt(LivingEntity attacked, DamageSource source, float dmg) {
        if (dmg >= this.config.minDamageToCount.get() && !this.isIndirectDamage(source)) {
            this.setHitCount(this.hitCount + 1);
            if (dmg >= this.config.minDamageToTrigger.get() && this.trigger(attacked)) {
                dmg = 0;
            }
        }
        final float explosionMultiplier = this.config.explosionReducedAmount.get().floatValue();
        if (explosionMultiplier < 1 && dmg > 0 && source.is(DamageTypeTags.IS_EXPLOSION)) {
            return dmg * explosionMultiplier;
        }
        return dmg;
    }

    private boolean trigger(LivingEntity attacked) {
        if (this.hitCount <= this.config.maxHits.get()) {
            return false;
        }
        if (attacked instanceof Player player) {
            player.displayClientMessage(Component.literal("Ow!").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), true);
            final float volume = this.config.soundVolume.get().floatValue();
            if (volume > 0 && player.tickCount - this.lastSoundTick >= SOUND_COOLDOWN_TICKS) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_PLACE,
                        SoundSource.PLAYERS, volume, this.config.soundPitch.get().floatValue());
                this.lastSoundTick = player.tickCount;
            }
        }
        this.setHitCount(0);
        return true;
    }

    /** 同种效果再次施加时在当前等级上叠一级（不超过上限）；比当前等级低的直接拒绝 */
    @Override
    public boolean potionApplied(LivingEntity entity, MobEffectInstance effect, boolean cancel) {
        final MobEffect target = this.effect();
        if (effect.getEffect() != target || entity.level().isClientSide || !this.config.effectStacks.get()) {
            return cancel;
        }
        final MobEffectInstance active = entity.getEffect(target);
        if (active == null) {
            return cancel;
        }
        final int amplifier = effect.getAmplifier() + 1;
        if (active.getAmplifier() > amplifier) {
            return true;
        }
        if (amplifier <= this.config.effectStackLimit.get()) {
            // 就地改写即将被添加的实例，事件放行后原版会用它覆盖当前效果
            effect.update(new MobEffectInstance(target, effect.getDuration(), amplifier, false, false));
        }
        return false;
    }

    @Override
    public void onAbilityRemoved(LivingEntity entity) {
        final MobEffect target = this.effect();
        final MobEffectInstance active = entity.getEffect(target);
        if (active != null && !active.isInfiniteDuration() && active.getDuration() <= EFFECT_DURATION) {
            entity.removeEffect(target);
        }
    }

    private MobEffect effect() {
        final ResourceLocation id = ResourceLocation.tryParse(this.config.effect.get());
        final MobEffect effect = id != null ? ForgeRegistries.MOB_EFFECTS.getValue(id) : null;
        return effect != null ? effect : MobEffects.DAMAGE_RESISTANCE;
    }

    @Override
    public void loadStorage(CompoundTag compound) {
        if (compound.contains(COUNT_TAG)) {
            this.hitCount = compound.getInt(COUNT_TAG);
        }
    }

    @Override
    public CompoundTag saveStorage(CompoundTag compound) {
        compound.putInt(COUNT_TAG, this.hitCount);
        return compound;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final int maxHits = this.config.maxHits.get();
        final double explosion = this.config.explosionReducedAmount.get();
        final boolean stacks = this.config.effectStacks.get();
        variables.number("hits", maxHits)
                .number("mincount", this.config.minDamageToCount.get())
                .number("mintrigger", this.config.minDamageToTrigger.get())
                .number("explosion", explosion < 1, explosion)
                .effects("effect", true, List.of(this.config.effect.get() + ":" + EFFECT_DURATION + ":" + this.config.effectLevel.get()), false)
                .number("maxlevel", stacks, this.config.effectStackLimit.get() + 1);
    }

    @Override
    public void describeStatus(DescriptionVariables variables, ItemStack source) {
        variables.number("count", this.hitCount).number("hits", this.config.maxHits.get());
    }
}
