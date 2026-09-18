package xzeroair.trinkets.traits.abilities.elements;

import javax.annotation.Nonnull;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.entity.BreathProjectile;
import xzeroair.trinkets.entity.BreathTerrain;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.ManaCost;
import xzeroair.trinkets.traits.abilities.base.AbilityRaceSpecific;
import xzeroair.trinkets.util.KeyNames;
import xzeroair.trinkets.util.config.abilities.BreathAbilityConfig;

/**
 * 吐息：按住种族技能键时每隔若干 tick 从头部向视线方向喷出一枚龙息（每枚耗魔），元素由 requiredElement 决定，
 * 颜色取种族的副特征色。对应 1.12 AbilityBreathBase 与四个子类。
 *
 * 移植说明：1.12 的 AbilityFireBreath / IceBreath / LightningBreath / DragonBreath 只差注册名，
 * 合并为本类并以工厂方法区分（注册名不变）。
 */
public class AbilityBreath extends AbilityRaceSpecific {

    public static final String BREATH_DRAGON = "breath_dragon";
    public static final String BREATH_FIRE = "breath_fire";
    public static final String BREATH_ICE = "breath_ice";
    public static final String BREATH_LIGHTNING = "breath_lightning";

    private static final int DEFAULT_COLOR = 16711680;

    private final BreathAbilityConfig config;
    private int breathStage;

    protected AbilityBreath(String name, @Nonnull BreathAbilityConfig config) {
        super(name);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    public static AbilityBreath dragon(BreathAbilityConfig config) {
        return new AbilityBreath(BREATH_DRAGON, config);
    }

    public static AbilityBreath fire(BreathAbilityConfig config) {
        return new AbilityBreath(BREATH_FIRE, config);
    }

    public static AbilityBreath ice(BreathAbilityConfig config) {
        return new AbilityBreath(BREATH_ICE, config);
    }

    public static AbilityBreath lightning(BreathAbilityConfig config) {
        return new AbilityBreath(BREATH_LIGHTNING, config);
    }

    @Override
    public boolean onKeyPress(Entity entity, boolean aux) {
        final MagicStats magic = entity instanceof LivingEntity living ? MagicStats.get(living) : null;
        return magic == null || magic.canSpendMana(this.cost());
    }

    @Override
    public boolean onKeyDown(Entity entity, boolean aux) {
        return this.breathe(entity);
    }

    @Override
    public boolean onKeyRelease(Entity entity, boolean aux) {
        this.breathStage = 0;
        return true;
    }

    private boolean breathe(Entity entity) {
        if (this.isSpectator(entity) || !(entity instanceof LivingEntity living)) {
            return false;
        }
        if (this.breathStage > this.config.frequency.get()) {
            this.breathStage = 0;
        }
        if (this.breathStage == 0) {
            final MagicStats magic = MagicStats.get(living);
            if (magic != null && !magic.spendMana(this.cost())) {
                return false;
            }
            final Level level = living.level();
            level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS,
                    0.5F, 0.4F / (living.getRandom().nextFloat() * 0.4F + 0.8F));
            if (!level.isClientSide) {
                this.shoot(living, level);
            }
        }
        this.breathStage++;
        return true;
    }

    private void shoot(LivingEntity entity, Level level) {
        final int color = this.breathColor(entity);
        final double yawRad = Math.toRadians(entity.getYRot() + 90.0F);
        final double headX = entity.getX() + 1.8F * 0.3F * Math.cos(yawRad);
        final double headZ = entity.getZ() + 1.8F * 0.3F * Math.sin(yawRad);
        final double headY = entity.getY() + entity.getEyeHeight() * 0.8D;
        final BreathProjectile breath = new BreathProjectile(level, entity, color)
                .setElement(this.getRequiredElement())
                .setEffects(this.config.effects.get())
                .setAllowTerrainInteraction(this.config.terrain.get())
                .setRenderImpactAreaCircle(false)
                .setDamage(this.config.damage.get().floatValue());
        breath.setPos(headX, headY, headZ);
        breath.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), 0.0F, 1.5F, 0.0F);
        level.addFreshEntity(breath);
    }

    private int breathColor(LivingEntity entity) {
        final EntityProperties properties = EntityProperties.get(entity);
        return properties != null ? properties.getRaceHandler().getSecondaryTraitColor() : DEFAULT_COLOR;
    }

    private float cost() {
        return this.config.cost.get().floatValue();
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final String name = this.getRegistryName().getPath();
        final boolean terrain = this.config.terrain.get();
        final boolean ice = BREATH_ICE.equals(name);
        final double damage = this.config.damage.get();
        variables.keybind("key", KeyNames.RACE_ABILITY)
                .seconds("interval", true, this.config.frequency.get() + 1)
                .number("cost", this.config.cost.get())
                .number("damage", damage)
                .number("burn", !ice, BREATH_LIGHTNING.equals(name) ? BreathProjectile.LIGHTNING_BURN_SECONDS : BreathProjectile.FIRE_BURN_SECONDS)
                .number("area", BREATH_DRAGON.equals(name), damage * BreathTerrain.NEUTRAL_AREA_DAMAGE_RATIO)
                .effects("effects", true, this.config.effects.get(), true)
                .number("fireblocks", terrain && BREATH_FIRE.equals(name), BreathTerrain.FIRE_IMPACT_INITIAL_MAX_BLOCKS)
                .seconds("fireinterval", true, BreathTerrain.FIRE_IMPACT_DELAYED_ATTEMPT_INTERVAL)
                .percent("firechance", true, BreathTerrain.FIRE_IMPACT_DELAYED_ATTEMPT_CHANCE)
                .number("freeze", terrain && ice, BreathTerrain.FREEZE_RADIUS);
    }

    @Override
    public ManaCost getManaCost() {
        return ManaCost.of(this.config.cost.get(), ManaCost.Unit.USE);
    }
}
