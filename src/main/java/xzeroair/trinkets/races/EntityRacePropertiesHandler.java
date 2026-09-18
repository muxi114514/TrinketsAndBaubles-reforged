package xzeroair.trinkets.races;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.compat.SurvivalAbilities;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;
import xzeroair.trinkets.util.config.server.RaceConfig;
import xzeroair.trinkets.util.helpers.PotionHelper;
import xzeroair.trinkets.util.helpers.damage.DamageTypeRules;
import xzeroair.trinkets.util.helpers.damage.DamageTypeRules.Stage;

/**
 * 种族行为处理器基类：持有一份种族状态，驱动变身过程、体型渐变与该种族的能力集合。
 *
 * 移植说明（体型是本类最大的改动点）：
 * 1.12 在 applyAdjustedSize() 里直接写 entity.width / entity.height 字段并手工
 * setEntityBoundingBox，还要在 modifyEyeHeight() 里按睡觉/潜行/鞘翅姿势分别算 eyeHeight。
 * 1.20.1 实体尺寸是不可变的 EntityDimensions，正确入口是 EntityEvent.Size 事件——
 * 事件自带 Pose，姿势判断由原版完成，本类只需对外提供「当前体型百分比」，
 * 由 events/RaceSizeHandler 换算成 EntityDimensions 与 eyeHeight。
 * 因此 applyAdjustedSize / modifyEyeHeight / resetEyeHeight / getAdjustedBoundingBox 均不移植。
 */
public abstract class EntityRacePropertiesHandler implements IRaceHandler {

    protected static final float MIN_PLAYER_WIDTH = 0.3F;
    protected static final float MIN_WIDTH = 0.252F;
    protected static final float MIN_HEIGHT = 0.45F;
    protected static final float MAX_SIZE = 5.4F;
    protected static final float SIZE_PRECISION = 1000.0F;

    protected boolean firstUpdate;
    protected boolean firstTransformUpdate;

    private final EntityProperties properties;
    private final LivingEntity entity;

    protected int targetWidth = 100;
    protected int targetHeight = 100;

    protected RaceCache raceCache;
    protected final RaceAppearance appearance;

    protected float healthBeforeTransformation;
    protected float maxHealthBeforeTransformation;
    protected double transformationProgress;

    protected Map<Integer, IAbilityInterface> raceAbilities;
    protected Map<String, IAbilityInterface> activeAbilities;
    private int index = 0;

    /** 上一形态的属性条目（变身渐变用） */
    protected List<? extends String> previousAttributes = List.of();
    protected float cooldown = 0;

    protected EntityRacePropertiesHandler(@Nullable LivingEntity entity, @Nullable EntityProperties parentProperties,
            @Nonnull RaceCache cache) {
        this.entity = entity;
        this.properties = parentProperties;
        this.firstUpdate = true;
        this.firstTransformUpdate = true;
        this.raceCache = cache;
        this.transformationProgress = 0D;
        this.appearance = new RaceAppearance(cache.getPrimaryColor(), cache.getSecondaryColor(), cache.getRace().getInformation().getOptionalColor());
        this.targetHeight = cache.getRace().getRaceHeight();
        this.targetWidth = cache.getRace().getRaceWidth();
        this.raceAbilities = new TreeMap<>();
        this.activeAbilities = new TreeMap<>();
    }

    /**
     * 本族的配置。
     * 移植说明：1.12 在每个种族类里各存一份 CONFIG 字段，并把「效果增删 / 药水免疫 / 坐骑黑白名单 /
     * 伤害类型」四段完全相同的逻辑复制了 9 遍。这里只要求子类交出配置，共用实现放在本基类，
     * 行为与原版一致但不再有重复代码。None 种族没有配置，返回 null。
     */
    @Nullable
    public RaceConfig<?> getConfig() {
        return null;
    }

    public EntityProperties getProperties() {
        return this.properties;
    }

    public LivingEntity getEntity() {
        return this.entity;
    }

    public EntityRace getRace() {
        return this.raceCache.getRace();
    }

    public RaceCache getRaceCache() {
        return this.raceCache;
    }

    // ── 能力集合 ──

    public void addAbility(@Nonnull IAbilityInterface ability) {
        this.raceAbilities.put(this.index++, ability);
    }

    @Nullable
    public IAbilityInterface getAbility(String ability) {
        return this.activeAbilities.get(ability);
    }

    public void removeAbility(String ability) {
        this.activeAbilities.remove(ability);
    }

    public Map<Integer, IAbilityInterface> getRaceAbilities() {
        return this.raceAbilities;
    }

    protected Map<String, IAbilityInterface> getActiveAbilities() {
        return this.activeAbilities;
    }

    /** 本族能力 + 生存类联动能力（物品说明展示变身戒指的能力时也走这里） */
    public void registerAllRaceAbilities() {
        this.registerRaceAbilities();
        this.registerSurvivalAbilities();
    }

    /** 生存类联动能力（1.12 各族 addSurvivalAbilities）；巨龙按元素分支覆盖 */
    protected void registerSurvivalAbilities() {
        final RaceConfig<?> config = this.getConfig();
        SurvivalAbilities.addTo(this::addAbility, config == null ? null : config.survival, null);
    }

    /** 把本种族登记的能力交给 AbilityHandler，来源标为种族注册名 */
    private void registerActiveRaceAbilities() {
        if (this.entity == null || this.properties == null) {
            return;
        }
        final String source = String.valueOf(this.getRace().getRegistryName());
        for (IAbilityInterface ability : this.raceAbilities.values()) {
            this.properties.getAbilityHandler().registerRaceAbility(this.entity, source, ability);
            this.activeAbilities.put(String.valueOf(ability.getRegistryName()), ability);
        }
    }

    // ── 变身状态机 ──

    public void setFirstUpdate() {
        this.setFirstUpdate(true);
    }

    public void setFirstUpdate(boolean firstUpdate) {
        this.firstUpdate = firstUpdate;
    }

    public void initializeState() {
        this.firstTransformUpdate = true;
        if (this.entity != null) {
            this.healthBeforeTransformation = this.entity.getHealth();
            this.maxHealthBeforeTransformation = this.entity.getMaxHealth();
        }
        this.index = 0;
        this.raceAbilities.clear();
        this.activeAbilities.clear();
        this.registerAllRaceAbilities();
        this.registerActiveRaceAbilities();
    }

    public void onTransform() {
        this.startTransformation();
        this.initializeState();
        this.previousAttributes = this.resolvePreviousAttributes();
    }

    /** 上一形态的属性条目：开始变身时取一次，渐变期间按 (1 - 进度) 逐步撤出 */
    private List<? extends String> resolvePreviousAttributes() {
        if (this.properties == null) {
            return List.of();
        }
        final RaceCache previous = this.properties.getPreviousRaceCache();
        return previous.getRace().getRaceHandler(null, null, previous).getAttributes();
    }

    public void onTransformEnd() {
        this.endTransformation();
        if (this.entity != null && !this.entity.level().isClientSide && this.properties != null) {
            this.savedNBTData(this.properties.getTag());
        }
        this.raceAbilities.clear();
        this.activeAbilities.clear();
    }

    public void onTick() {
        this.updateSize();
        if (this.isTransforming()) {
            this.applyTransitionAttributes();
            this.whileTranforming();
        } else if (this.isTransformed()) {
            RaceAttributes.apply(this.entity, this.getRace(), this.getAttributes(), 1.0D);
            if (this.firstTransformUpdate) {
                this.restoreHealthAfterTransform();
            }
            this.whileTransformed();
            this.firstTransformUpdate = false;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        } else {
            this.cooldown = 0;
        }
    }

    /** 对应 1.12 addNewAttributes：旧形态属性按 (1 - 进度) 保留，新形态按进度生效（进度取三位小数） */
    private void applyTransitionAttributes() {
        if (this.entity == null || this.properties == null) {
            return;
        }
        final double progress = Math.round(this.transformationProgress() * 1000.0D) / 1000.0D;
        RaceAttributes.apply(this.entity, this.properties.getPreviousRaceCache().getRace(), this.previousAttributes, 1.0D - progress);
        if (progress != 0) {
            RaceAttributes.apply(this.entity, this.getRace(), this.getAttributes(), progress);
        }
    }

    /**
     * 变身会改变最大生命上限，这里把「变身前的生命差额」补回去，避免换种族后凭空掉血。
     * 只在服务端执行；1.20.1 若装有 First Aid 等接管血量的模组，heal 走 LivingHealEvent 是安全的。
     */
    private void restoreHealthAfterTransform() {
        if (this.entity == null || this.entity.level().isClientSide) {
            return;
        }
        final float newMaxHealth = this.entity.getMaxHealth();
        final float difference = this.healthBeforeTransformation - this.maxHealthBeforeTransformation;
        final float healAmount = (this.maxHealthBeforeTransformation - newMaxHealth) + difference;
        if (healAmount > 0) {
            this.entity.heal(healAmount);
        }
    }

    /** 体型百分比每 tick 朝目标走一格，形成渐变；同时刷新变身进度 */
    public void updateSize() {
        if (this.properties == null) {
            return;
        }
        final int targetHeight = this.getTargetHeight();
        final int targetWidth = this.getTargetWidth();
        final int height = this.properties.getHeightValue();
        final int width = this.properties.getWidthValue();
        if (height == targetHeight && width == targetWidth) {
            return;
        }
        this.properties.setHeightValue(stepTowards(height, targetHeight));
        this.properties.setWidthValue(stepTowards(width, targetWidth));

        final EntityRace previous = this.properties.getPreviousRaceCache().getRace();
        final double heightProgress = this.transformProgress(previous.getRaceHeight(), targetHeight, height);
        final double widthProgress = this.transformProgress(previous.getRaceWidth(), targetWidth, width);
        this.transformationProgress = Mth.clamp((heightProgress + widthProgress) / 2.0D, 0.0D, 1.0D);

        // 尺寸变了就让原版重算包围盒与眼高（触发 EntityEvent.Size）
        if (this.entity != null) {
            this.entity.refreshDimensions();
        }
    }

    private static int stepTowards(int current, int target) {
        if (current < target) {
            return current + 1;
        }
        if (current > target) {
            return current - 1;
        }
        return current;
    }

    protected double transformProgress(int previousTarget, int currentTarget, int currentValue) {
        if (previousTarget == currentTarget) {
            return 1.0D;
        }
        final double pct = (currentValue - previousTarget) / (double) (currentTarget - previousTarget);
        return pct < 0.01D ? 0.0D : Math.min(pct, 1.0D);
    }

    public boolean isTransforming() {
        return this.properties != null
                && (this.properties.getHeightValue() != this.getTargetHeight()
                        || this.properties.getWidthValue() != this.getTargetWidth());
    }

    public boolean isTransformed() {
        return this.properties != null
                && this.properties.getHeightValue() == this.getTargetHeight()
                && this.properties.getWidthValue() == this.getTargetWidth();
    }

    public double transformationProgress() {
        return this.isTransformed() ? 1.0D : this.transformationProgress;
    }

    // ── 体型 ──

    public int getTargetHeight() {
        return this.targetHeight;
    }

    public void setTargetHeight(int targetHeight) {
        this.targetHeight = targetHeight;
    }

    public int getTargetWidth() {
        return this.targetWidth;
    }

    public void setTargetWidth(int targetWidth) {
        this.targetWidth = targetWidth;
    }

    /** 当前高度缩放系数，1.0 为原始体型 */
    public float getHeightScale() {
        return this.properties == null ? 1.0F : this.properties.getHeightValue() / 100.0F;
    }

    /** 当前宽度缩放系数，1.0 为原始体型 */
    public float getWidthScale() {
        return this.properties == null ? 1.0F : this.properties.getWidthValue() / 100.0F;
    }

    public float clampHeight(float height) {
        return Mth.clamp(height, MIN_HEIGHT, MAX_SIZE);
    }

    public float clampWidth(float width) {
        final float min = this.entity instanceof Player ? MIN_PLAYER_WIDTH : MIN_WIDTH;
        return Mth.clamp(width, min, MAX_SIZE);
    }

    public boolean isSwimming() {
        return this.entity != null && this.entity.isSwimming();
    }

    /** 本族当前是否允许飞行（创造飞行能力据此给予或收回飞行许可）；子类可追加条件 */
    public boolean canFly() {
        return this.getRace().canFly();
    }

    // ── 配置驱动的共用行为（1.12 中这四段在 9 个种族类里各复制一份）──

    /** 本族自带的效果；子类可按元素等条件替换（巨龙） */
    public List<? extends String> getEffectsToAdd() {
        final RaceConfig<?> config = this.getConfig();
        return config == null ? List.of() : config.effectsToAdd.get();
    }

    /** 本族免疫的效果 */
    public List<? extends String> getEffectsToRemove() {
        final RaceConfig<?> config = this.getConfig();
        return config == null ? List.of() : config.effectsToRemove.get();
    }

    /** 本族的伤害类型规则 */
    public List<? extends String> getDamageTypesToIgnore() {
        final RaceConfig<?> config = this.getConfig();
        return config == null ? List.of() : config.damageTypesToIgnore.get();
    }

    /** 本族的属性修饰条目 */
    public List<? extends String> getAttributes() {
        final RaceConfig<?> config = this.getConfig();
        return config == null ? List.of() : config.attributes.get();
    }

    /** 本族特性说明（语言键 xat.race.种族.trait1~10）里占位符的取值；没有独有特性的种族不必覆写 */
    public void describeTraits(DescriptionVariables variables) {
    }

    /** 每 tick：清掉本族免疫的效果、续上本族自带的效果、检查坐骑是否仍被允许 */
    @Override
    public void whileTransformed() {
        if (this.getConfig() == null || this.entity == null) {
            return;
        }
        PotionHelper.removeAllFromConfig(this.entity, this.getEffectsToRemove());
        PotionHelper.addAllFromConfig(this.entity, true, this.getEffectsToAdd());
        if (!this.entity.level().isClientSide && this.entity.isPassenger()) {
            final Entity mount = this.entity.getVehicle();
            if (mount != null && !this.mountEntity(mount)) {
                this.entity.stopRiding();
            }
        }
    }

    /** 变身结束：撤掉本族施加的效果 */
    @Override
    public void endTransformation() {
        if (this.getConfig() != null && this.entity != null) {
            PotionHelper.removeAllFromConfig(this.entity, this.getEffectsToAdd());
        }
    }

    /** 本族免疫名单中的效果一律拒绝施加 */
    @Override
    public boolean potionBeingApplied(MobEffectInstance effect) {
        return this.getConfig() != null && PotionHelper.isEffect(effect, this.getEffectsToRemove());
    }

    /** 攻击阶段：命中本族「忽略的伤害类型」规则则取消（仅服务端，与 1.12 一致） */
    @Override
    public boolean isAttacked(DamageSource source, float dmg) {
        if (this.getConfig() == null || this.entity == null || this.entity.level().isClientSide) {
            return true;
        }
        return !DamageTypeRules.shouldCancel(source, dmg, this.raceCache.getPrimaryElement(), this.getDamageTypesToIgnore());
    }

    /** 护甲前：按规则倍率缩放 */
    @Override
    public float isHurt(DamageSource source, float dmg) {
        return this.getConfig() == null ? dmg
                : DamageTypeRules.scale(Stage.HURT, source, dmg, this.raceCache.getPrimaryElement(), this.getDamageTypesToIgnore());
    }

    /** 护甲后：按规则倍率缩放 */
    @Override
    public float isDamaged(DamageSource source, float dmg) {
        return this.getConfig() == null ? dmg
                : DamageTypeRules.scale(Stage.DAMAGED, source, dmg, this.raceCache.getPrimaryElement(), this.getDamageTypesToIgnore());
    }

    /** 坐骑准入判定（规则见 RaceMountRules） */
    @Override
    public boolean mountEntity(Entity mount) {
        final RaceConfig<?> config = this.getConfig();
        return config == null || this.entity == null || RaceMountRules.canMount(this.entity, mount, config);
    }

    // ── 外观特征 ──

    /** 性别、颜色、变种与是否显示特征（详见 RaceAppearance） */
    public RaceAppearance getAppearance() {
        return this.appearance;
    }

    public boolean showTraits() {
        return this.appearance.showTraits();
    }

    public int getPrimaryTraitColor() {
        return this.appearance.getPrimaryColor();
    }

    public int getSecondaryTraitColor() {
        return this.appearance.getSecondaryColor();
    }

    // ── 存取 ──

    public CompoundTag savedNBTData(CompoundTag compound) {
        return this.appearance.save(compound);
    }

    public void loadNBTData(CompoundTag compound) {
        this.appearance.load(compound);
    }

    public void copyFrom(EntityRacePropertiesHandler source, boolean wasDeath, boolean keepInv) {
        this.appearance.copyFrom(source.appearance);
    }
}
