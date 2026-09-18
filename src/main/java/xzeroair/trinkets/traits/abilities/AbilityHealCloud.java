package xzeroair.trinkets.traits.abilities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.entity.area.AreaEffectEntity;
import xzeroair.trinkets.entity.area.action.GrowBlockAreaAction;
import xzeroair.trinkets.entity.area.action.PotionAreaAction;
import xzeroair.trinkets.entity.area.action.RepairItemAreaAction;
import xzeroair.trinkets.init.ModEntities;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.ManaCost;
import xzeroair.trinkets.traits.abilities.base.AbilityRaceSpecific;
import xzeroair.trinkets.util.config.abilities.HealCloudAbilityConfig;
import xzeroair.trinkets.util.helpers.PotionHelper;
import xzeroair.trinkets.util.helpers.RayTraceHelper;

/**
 * 复苏领域（妖精）：松开种族技能键时在视线落点生成一片治愈区域——给范围内生物配置的效果、修复掉落的物品、催熟作物。
 * 魔力花费 = 持续秒数 × 每秒花费 + 每个效果的花费 + 效果等级花费。同一时间只保留一片，重新施放会替换旧的。
 * 对应 1.12 AbilityHealCloud。
 */
public class AbilityHealCloud extends AbilityRaceSpecific {

    private static final String ACTIVE_FIELD_TAG = "ActiveField";
    private static final int DEFAULT_FIELD_COLOR = 12514535;

    private final HealCloudAbilityConfig config;
    @Nullable
    private UUID activeFieldId;

    public AbilityHealCloud(@Nonnull HealCloudAbilityConfig config) {
        super(AbilityNames.RESTORATION_FIELD);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public boolean onKeyRelease(Entity entity, boolean aux) {
        if (entity.level().isClientSide) {
            return true;
        }
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        final Vec3 target = this.findTarget(living);
        if (target == null) {
            return false;
        }
        final MagicStats magic = MagicStats.get(living);
        if (magic != null && !magic.spendMana(this.manaCost())) {
            return false;
        }
        return this.spawnField(living, target);
    }

    /** 魔力花费：持续秒数（向上取整、至少 1）× 每秒花费 + Σ(每效果花费 + 等级 × 每级花费) */
    public float manaCost() {
        final int seconds = Math.max(1, (int) Math.ceil(this.config.duration.get() / 20.0D));
        float cost = seconds * this.config.costPerSecond.get().floatValue();
        for (MobEffectInstance effect : this.effects()) {
            cost += this.config.costPerPotionEffect.get().floatValue();
            cost += Math.max(0, effect.getAmplifier()) * this.config.costPerEffectLevel.get().floatValue();
        }
        return cost;
    }

    @Nullable
    private Vec3 findTarget(LivingEntity entity) {
        final double range = Math.max(1.0D, this.config.castRange.get());
        final HitResult result = RayTraceHelper.rayTrace(entity, range);
        if (result instanceof EntityHitResult entityHit) {
            return entityHit.getLocation().add(0.0D, entityHit.getEntity().getBbHeight() * 0.5F, 0.0D);
        }
        if (result instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            return centered(blockHit);
        }
        // 视线未命中：从最远点向下探地面
        final Vec3 limit = entity.getEyePosition().add(entity.getViewVector(1.0F).scale(range));
        final Vec3 probeEnd = limit.add(0.0D, -Math.max(8.0D, this.config.verticalRadius.get() + 3.0D), 0.0D);
        final BlockHitResult ground = entity.level().clip(new ClipContext(limit, probeEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        return ground.getType() == HitResult.Type.BLOCK ? centered(ground) : null;
    }

    private static Vec3 centered(BlockHitResult hit) {
        final BlockPos pos = hit.getBlockPos();
        return new Vec3(pos.getX() + 0.5D, hit.getLocation().y, pos.getZ() + 0.5D);
    }

    private boolean spawnField(LivingEntity owner, Vec3 position) {
        final ServerLevel level = (ServerLevel) owner.level();
        final AreaEffectEntity oldField = this.activeFieldId == null ? null
                : level.getEntity(this.activeFieldId) instanceof AreaEffectEntity area ? area : null;
        final AreaEffectEntity field = this.createField(owner, position);
        if (!level.addFreshEntity(field)) {
            return false;
        }
        if (oldField != null && oldField.isAlive()) {
            oldField.discard();
        }
        this.activeFieldId = field.getUUID();
        this.setChanged(true);
        return true;
    }

    public AreaEffectEntity createField(LivingEntity owner, Vec3 position) {
        final AreaEffectEntity area = new AreaEffectEntity(ModEntities.AREA_EFFECT.get(), owner.level(), position.x, position.y, position.z);
        area.setOwner(owner);
        area.setRadius(this.config.radius.get().floatValue());
        area.setVerticalRadius(this.config.verticalRadius.get().floatValue());
        area.setWaitTime(this.config.waitTime.get());
        area.setDuration(this.config.duration.get());
        area.setPulseInterval(this.config.pulseInterval.get());
        area.setReapplicationDelay(this.config.reapplicationDelay.get());
        area.setColor(fieldColor(owner));
        area.blacklistEntityRegistry("minecraft:item_frame", "minecraft:glow_item_frame", "minecraft:painting");
        for (MobEffectInstance effect : this.effects()) {
            area.addAction(new PotionAreaAction(effect));
        }
        if (this.config.itemRepairAmount.get() > 0) {
            area.addAction(new RepairItemAreaAction(this.config.itemRepairAmount.get()));
        }
        if (this.config.growthAttemptsPerPulse.get() > 0) {
            area.addAction(new GrowBlockAreaAction(this.config.growthAttemptsPerPulse.get()));
        }
        return area;
    }

    private List<MobEffectInstance> effects() {
        final List<MobEffectInstance> effects = new ArrayList<>();
        for (String entry : this.config.effects.get()) {
            final PotionHelper.ParsedEffect parsed = PotionHelper.parse(entry);
            if (parsed != null) {
                effects.add(parsed.toInstance());
            }
        }
        return effects;
    }

    private static int fieldColor(LivingEntity entity) {
        final EntityProperties properties = EntityProperties.get(entity);
        return properties == null ? DEFAULT_FIELD_COLOR : properties.getRaceHandler().getPrimaryTraitColor();
    }

    @Override
    public void loadStorage(CompoundTag compound) {
        this.activeFieldId = compound.hasUUID(ACTIVE_FIELD_TAG) ? compound.getUUID(ACTIVE_FIELD_TAG) : null;
    }

    @Override
    public CompoundTag saveStorage(CompoundTag compound) {
        if (this.activeFieldId != null) {
            compound.putUUID(ACTIVE_FIELD_TAG, this.activeFieldId);
        }
        return compound;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final boolean effects = !this.config.effects.get().isEmpty();
        final int repair = this.config.itemRepairAmount.get();
        final int growth = this.config.growthAttemptsPerPulse.get();
        variables.keybind("key", this.getKey())
                .number("range", Math.max(1.0D, this.config.castRange.get()))
                .number("radius", this.config.radius.get())
                .number("height", this.config.verticalRadius.get())
                .seconds("wait", true, this.config.waitTime.get())
                .seconds("duration", true, this.config.duration.get())
                .seconds("pulse", true, this.config.pulseInterval.get())
                .effects("effects", effects, this.config.effects.get(), false)
                .seconds("reapply", effects, this.config.reapplicationDelay.get())
                .number("repair", repair > 0, repair)
                .number("growth", growth > 0, growth)
                .number("total", this.manaCost())
                .number("persecond", this.config.costPerSecond.get())
                .number("pereffect", this.config.costPerPotionEffect.get())
                .number("perlevel", this.config.costPerEffectLevel.get());
    }

    @Override
    public ManaCost getManaCost() {
        return ManaCost.of(this.manaCost(), ManaCost.Unit.USE);
    }
}
