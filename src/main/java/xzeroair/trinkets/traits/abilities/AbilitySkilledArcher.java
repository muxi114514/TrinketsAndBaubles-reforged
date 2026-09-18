package xzeroair.trinkets.traits.abilities;

import javax.annotation.Nonnull;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.ManaCost;
import xzeroair.trinkets.traits.abilities.archery.BowWeightTable;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IBowAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.config.abilities.SkilledArcherAbilityConfig;
import xzeroair.trinkets.util.config.abilities.SkilledArcherAbilityConfig.ScalingMode;
import xzeroair.trinkets.util.helpers.ChunkSafety;

/**
 * 熟练弓手（精灵）：箭伤按弓的「重量」与拉弓程度缩放；潜行拉弓可额外灌注魔力增伤，
 * 满弓且付满魔力的一箭命中时爆炸（不破坏地形）。
 *
 * 移植说明：
 * - 1.12 的调试日志开关常量恒为 false，相关日志方法不移植。
 * - 爆炸前检查爆炸半径覆盖的区块均已加载，否则跳过爆炸（避免拉起未加载区块）。
 */
public class AbilitySkilledArcher extends Ability implements ITickableAbility, IBowAbility, IAttackAbility {

    private static final int MAX_SHOT_WINDOW = 10;
    /** 满拉弓的法力消耗 = chargeShotCost × 本值 × 拉弓比例 */
    private static final float FULL_COST_SCALE = 10F;
    /** 爆炸威力 = 本值 + 弓重 / 基准弓重 × EXPLOSION_PER_WEIGHT */
    private static final float EXPLOSION_BASE = 1.5F;
    private static final float EXPLOSION_PER_WEIGHT = 2.0F;
    private static final int BOW_USE_DURATION = 72000;

    private final SkilledArcherAbilityConfig config;
    private boolean drawnBow;
    private boolean drawingBow;
    private boolean released;
    private boolean hitPending;
    private boolean crit;
    private boolean usedFullCost;
    private boolean sneaking;
    private int waitTicks;
    private float drawWeight;
    private float damageMultiplier;

    public AbilitySkilledArcher(@Nonnull SkilledArcherAbilityConfig config) {
        super(AbilityNames.SKILLED_ARCHER);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        if (!(entity instanceof Player)) {
            return;
        }
        if (entity.isShiftKeyDown()) {
            this.sneaking = true;
        } else if (this.sneaking) {
            final MagicStats magic = MagicStats.get(entity);
            if (magic != null) {
                magic.syncManaCostToHud(0);
            }
            this.sneaking = false;
        }
        if (this.hitPending && ++this.waitTicks > MAX_SHOT_WINDOW) {
            this.reset();
        }
    }

    @Override
    public void knockArrow(ArrowNockEvent event) {
        this.drawingBow = true;
    }

    @Override
    public int onItemUseTick(LivingEntity entity, ItemStack stack, int duration) {
        if (!this.drawingBow) {
            return duration;
        }
        final float charge = this.chargeCurve(BOW_USE_DURATION - duration);
        if (this.sneaking) {
            final MagicStats magic = MagicStats.get(entity);
            if (magic != null) {
                final float cost = this.config.chargeShotCost.get().floatValue();
                magic.syncManaCostToHud(Mth.clamp(cost * charge * FULL_COST_SCALE, 0, magic.getMana()));
            }
        }
        final Level level = entity.level();
        final boolean local = level.isClientSide && entity instanceof Player;
        if (charge >= 1F && !this.drawnBow) {
            this.drawnBow = true;
            if (local && duration % 5 == 0) {
                level.playSound((Player) entity, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.3F, 0.5F);
            }
        } else if (local) {
            level.playSound((Player) entity, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.NOTE_BLOCK_GUITAR.value(), SoundSource.PLAYERS, 0.1F, charge + 0.5F);
        }
        return duration;
    }

    @Override
    public void onItemUseStop(LivingEntity entity, ItemStack stack, int duration) {
        this.drawingBow = false;
        this.drawnBow = false;
    }

    @Override
    public void looseArrow(@Nonnull ArrowLooseEvent event) {
        final float pounds = BowWeightTable.weightOf(event.getBow(), this.config);
        if (pounds <= 0) {
            return;
        }
        final float charge = this.chargeCurve(event.getCharge());
        this.drawWeight = pounds * charge;
        this.crit = charge >= 1F;
        if (this.released) {
            return;
        }
        final Player player = event.getEntity();
        float scale = this.config.chargeShotDamageMultiplier.get().floatValue();
        final float manaCost = this.config.chargeShotCost.get().floatValue() * charge * FULL_COST_SCALE;
        if (player.isShiftKeyDown() && manaCost > 0F) {
            final MagicStats magic = MagicStats.get(player);
            if (magic != null) {
                magic.syncManaCostToHud(0);
                final float ratio = Math.min(1F, magic.getMana() / manaCost);
                // 按实际付得起的比例加成，付满再追加一档
                scale += ratio;
                if (magic.spendMana(manaCost * ratio)) {
                    this.usedFullCost = ratio >= 1F;
                }
                if (this.usedFullCost) {
                    scale += this.config.chargeShotManaDamageMultiplier.get().floatValue();
                }
            }
        }
        this.damageMultiplier = Math.max(scale, this.minMultiplier());
        this.hitPending = true;
        this.waitTicks = 0;
        this.released = true;
    }

    @Override
    public void arrowImpact(ProjectileImpactEvent event) {
        final Projectile projectile = event.getProjectile();
        if (!(projectile instanceof AbstractArrow) || event.getRayTraceResult().getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (this.explodes()) {
            this.explode(projectile);
        }
        this.reset();
    }

    @Override
    public float hurtEntity(LivingEntity target, @Nonnull DamageSource source, float dmg) {
        if (!(source.getDirectEntity() instanceof AbstractArrow arrow)) {
            return dmg;
        }
        final float multiplier = this.damageScale(this.drawWeight) * Math.max(this.damageMultiplier, this.minMultiplier());
        final float scaled = dmg * multiplier;
        if (scaled > 0 && this.explodes()) {
            this.explode(arrow);
        }
        this.reset();
        return scaled;
    }

    @Override
    public void onAbilityRemoved(LivingEntity entity) {
        final MagicStats magic = MagicStats.get(entity);
        if (magic != null) {
            magic.syncManaCostToHud(0);
        }
    }

    private boolean explodes() {
        return this.drawWeight > 0 && this.crit && this.usedFullCost && this.config.chargeShotExplodes.get();
    }

    private void explode(Projectile projectile) {
        final Level level = projectile.level();
        final float strength = this.explosionStrength(this.drawWeight);
        // 爆炸影响半径约为 2 × 威力
        if (level.isClientSide || !ChunkSafety.isAreaLoaded(level, projectile.getBlockX(), projectile.getBlockZ(), (int) Math.ceil(strength * 2))) {
            return;
        }
        level.explode(projectile, projectile.getX(), projectile.getY(), projectile.getZ(), strength, Level.ExplosionInteraction.NONE);
    }

    private float chargeCurve(int ticks) {
        float charge = ticks / (float) this.config.chargeShotTime.get();
        charge = (charge * charge + charge * 2.0F) / 3.0F;
        return Math.min(charge, 1F);
    }

    private float weightFactor(float drawWeight) {
        final float defaultWeight = this.config.defaultWeight.get().floatValue();
        return defaultWeight <= 0F ? 0F : drawWeight / defaultWeight;
    }

    private float damageScale(float drawWeight) {
        final float factor = this.weightFactor(drawWeight);
        final ScalingMode mode = this.config.scalingMode.get();
        final float scale = switch (mode) {
            case LINEAR -> factor;
            case SOFT -> (float) Math.pow(factor, 0.75D);
            // 动能 ∝ 速度²，速度 ∝ √重量，故与线性等价；保留分支以对应 1.12 的配置语义
            case KINETIC -> (float) Math.pow(Math.sqrt(factor), 2);
        };
        return Math.max(scale, this.minMultiplier());
    }

    private float explosionStrength(float drawWeight) {
        return EXPLOSION_BASE + this.weightFactor(drawWeight) * EXPLOSION_PER_WEIGHT;
    }

    private float minMultiplier() {
        return this.config.chargeShotMinDamageMultiplier.get().floatValue();
    }

    private void reset() {
        this.waitTicks = 0;
        this.drawWeight = 0F;
        this.damageMultiplier = 0F;
        this.drawnBow = false;
        this.drawingBow = false;
        this.released = false;
        this.hitPending = false;
        this.crit = false;
        this.usedFullCost = false;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final double fullCost = this.config.chargeShotCost.get() * FULL_COST_SCALE;
        final double base = this.config.chargeShotDamageMultiplier.get();
        final double mana = this.config.chargeShotManaDamageMultiplier.get();
        variables.number("bows", this.config.bows.get().size())
                .seconds("draw", true, this.config.chargeShotTime.get())
                .number("base", base)
                .number("weight", this.config.defaultWeight.get())
                .number("min", this.config.chargeShotMinDamageMultiplier.get())
                .flag("soft", this.config.scalingMode.get() == ScalingMode.SOFT)
                .number("fullcost", fullCost > 0, fullCost)
                .number("mana", mana)
                .number("max", base + 1 + mana)
                .number("power", fullCost > 0 && this.config.chargeShotExplodes.get(), EXPLOSION_BASE)
                .number("powerscale", EXPLOSION_PER_WEIGHT);
    }

    @Override
    public ManaCost getManaCost() {
        return ManaCost.of(this.config.chargeShotCost.get() * FULL_COST_SCALE, ManaCost.Unit.FULL_CHARGE);
    }
}
