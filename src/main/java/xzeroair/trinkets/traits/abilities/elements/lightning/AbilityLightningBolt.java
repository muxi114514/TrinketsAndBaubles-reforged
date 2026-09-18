package xzeroair.trinkets.traits.abilities.elements.lightning;

import javax.annotation.Nonnull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.init.ModDamageTypes;
import xzeroair.trinkets.network.EffectsRenderPacket;
import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.ManaCost;
import xzeroair.trinkets.traits.abilities.interfaces.IKeyBindInterface;
import xzeroair.trinkets.util.KeyNames;
import xzeroair.trinkets.util.config.abilities.LightningBoltAbilityConfig;
import xzeroair.trinkets.util.handlers.Counter;
import xzeroair.trinkets.util.helpers.RayTraceHelper;

/**
 * 雷击：按住电弧宝珠技能键蓄力（魔力越多可蓄越久），松开后向视线方向 15 格内放出闪电，
 * 对落点 1 格范围内的实体造成随蓄力增长的雷电伤害；蓄力超过 1/3 时苦力怕会被充能。
 */
public class AbilityLightningBolt extends Ability implements IKeyBindInterface {

    private static final String COUNTER = "heldCounter";
    private static final double MAX_DISTANCE = 15.0D;
    /** 松开时蓄力不足该比例则不释放 */
    private static final double MIN_CHARGE = 0.10D;
    /** 蓄力超过该比例时命中的苦力怕变为闪电苦力怕 */
    private static final float CHARGE_CREEPER_THRESHOLD = 0.33F;
    private static final int BOLT_COLOR = 2515356;

    private final LightningBoltAbilityConfig config;

    public AbilityLightningBolt(@Nonnull LightningBoltAbilityConfig config) {
        super(AbilityNames.LIGHTNING_BOLT);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public String getKey() {
        return KeyNames.ARCING_ORB_ABILITY;
    }

    @Override
    public boolean onKeyPress(Entity entity, boolean aux) {
        final MagicStats magic = magicOf(entity);
        if (magic == null) {
            return true;
        }
        final int chargeTime = this.config.chargeTime.get();
        final double pct = Math.min(Mth.inverseLerp(magic.getMana(), 0, this.cost()), 1D);
        final int length = Math.min((int) (chargeTime * pct), chargeTime);
        final Counter counter = this.counter(length);
        counter.resetTick();
        counter.setLength(length);
        return pct >= MIN_CHARGE;
    }

    @Override
    public boolean onKeyDown(Entity entity, boolean aux) {
        final MagicStats magic = magicOf(entity);
        if (magic == null) {
            return true;
        }
        final float mana = magic.getMana();
        if (mana <= 1F) {
            return true;
        }
        final Counter counter = this.counter(this.config.chargeTime.get());
        final int tick = counter.getTick();
        final float progress = (float) Mth.inverseLerp(tick, 0, Math.max(counter.getLength(), 1));
        final float realCost = (float) (this.cost() * Mth.inverseLerp(tick, 0, this.config.chargeTime.get()));
        final boolean local = entity instanceof Player && entity.level().isClientSide;
        if (!counter.Tick()) {
            if (local && entity.tickCount % 2 == 0) {
                entity.level().playSound((Player) entity, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.2F, Math.min(0.4F + 0.6F * progress, 1F));
            }
            magic.syncManaCostToHud(realCost);
            return realCost != mana;
        }
        if (local) {
            entity.level().playSound((Player) entity, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.END_GATEWAY_SPAWN, SoundSource.PLAYERS, 0.3F, 0.3F);
        }
        magic.syncManaCostToHud(realCost);
        return false;
    }

    @Override
    public boolean onKeyRelease(Entity entity, boolean aux) {
        final MagicStats magic = magicOf(entity);
        if (magic == null) {
            return true;
        }
        final Counter counter = this.counter(this.config.chargeTime.get());
        final int tick = counter.getTick();
        final float multiplier = (float) Mth.inverseLerp(tick, 0, this.config.chargeTime.get());
        final float realCost = this.cost() * multiplier;
        if (tick > counter.getLength() * MIN_CHARGE && magic.spendMana(realCost)) {
            if (entity.level() instanceof ServerLevel level) {
                this.castBolt((LivingEntity) entity, level, multiplier);
            }
        }
        this.reset(magic);
        return true;
    }

    private void castBolt(LivingEntity caster, ServerLevel level, float multiplier) {
        final Vec3 start = caster.getEyePosition();
        final HitResult result = RayTraceHelper.rayTrace(caster, MAX_DISTANCE);
        Vec3 hitLoc = start.add(caster.getViewVector(1.0F).scale(MAX_DISTANCE));
        if (result instanceof EntityHitResult entityHit) {
            hitLoc = entityHit.getLocation().add(0, entityHit.getEntity().getBbHeight() * 0.5F, 0);
        } else if (result.getType() == HitResult.Type.BLOCK) {
            hitLoc = result.getLocation();
        }
        new EffectsRenderPacket(EffectsRenderPacket.LIGHTNING, start.x, start.y, start.z, hitLoc.x, hitLoc.y, hitLoc.z,
                BOLT_COLOR, 0.9F, 3.0F).sendNear(caster);
        new EffectsRenderPacket(EffectsRenderPacket.LIGHTNING_ORB, hitLoc.x, hitLoc.y, hitLoc.z, hitLoc.x, hitLoc.y, hitLoc.z,
                BOLT_COLOR, 0.9F, 3.0F).sendNear(level);

        final boolean pvp = caster instanceof ServerPlayer player && player.server.isPvpAllowed();
        final float damage = this.config.attackDamage.get().floatValue() * multiplier;
        final DamageSource source = ModDamageTypes.source(level, DamageTypes.LIGHTNING_BOLT, caster);
        final AABB splash = new AABB(Mth.floor(hitLoc.x), Mth.floor(hitLoc.y), Mth.floor(hitLoc.z),
                Mth.floor(hitLoc.x) + 1, Mth.floor(hitLoc.y) + 1, Mth.floor(hitLoc.z) + 1).inflate(1.0D);
        for (Entity target : level.getEntities(caster, splash, e -> !e.isSpectator() && e.isPickable())) {
            if (target instanceof Player && !pvp) {
                continue;
            }
            if (multiplier > CHARGE_CREEPER_THRESHOLD && target instanceof Creeper creeper) {
                final LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(creeper.getX(), creeper.getY(), creeper.getZ());
                    bolt.setVisualOnly(true);
                    creeper.thunderHit(level, bolt);
                    creeper.clearFire();
                }
            }
            target.hurt(source, damage);
        }
    }

    private void reset(MagicStats magic) {
        final Counter counter = this.tickHandler.getCounter(COUNTER);
        if (counter != null) {
            counter.resetTick();
            counter.setLength(this.config.chargeTime.get());
        }
        magic.syncManaCostToHud(0);
    }

    private Counter counter(int length) {
        return this.tickHandler.getCounter(COUNTER, length, false, true, false, true, false);
    }

    private float cost() {
        return this.config.attackCost.get().floatValue();
    }

    private static MagicStats magicOf(Entity entity) {
        return entity instanceof LivingEntity living ? MagicStats.get(living) : null;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        variables.keybind("key", KeyNames.ARCING_ORB_ABILITY)
                .number("range", MAX_DISTANCE)
                .seconds("charge", true, this.config.chargeTime.get())
                .number("damage", this.config.attackDamage.get())
                .number("cost", this.config.attackCost.get())
                .percent("mincharge", true, MIN_CHARGE)
                .percent("creeper", true, CHARGE_CREEPER_THRESHOLD);
    }

    @Override
    public ManaCost getManaCost() {
        return ManaCost.of(this.config.attackCost.get(), ManaCost.Unit.FULL_CHARGE);
    }
}
