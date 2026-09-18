package xzeroair.trinkets.races.goblin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.AbilityClimbing;
import xzeroair.trinkets.traits.abilities.other.AbilityWolfMount;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.RaceConfig;
import xzeroair.trinkets.util.config.server.race.GoblinAbilitiesConfig;

/**
 * 哥布林种族的行为处理器：攀爬、骑狼、爆炸/火焰抗性、苦力怕友好、触碰引爆苦力怕。
 *
 * 移植说明：1.12 的同名类里，「效果增删 / 药水免疫 / 坐骑黑白名单」三段逻辑与其余 8 族逐字相同，
 * 已上移至 EntityRacePropertiesHandler，本类只需交出配置与本族独有内容。
 */
public class RaceGoblin extends EntityRacePropertiesHandler {

    /** 满额抗性对应的生命值：当前生命 / 20 即爆炸伤害倍率 */
    public static final float EXPLOSION_HEALTH_BASE = 20F;
    public static final float MIN_EXPLOSION_MULTIPLIER = 0.01F;
    public static final float FIRE_MULTIPLIER = 0.8F;
    public static final float LAVA_MULTIPLIER = 0.5F;

    public RaceGoblin(@Nullable LivingEntity entity, @Nullable EntityProperties properties, @Nonnull RaceCache cache) {
        super(entity, properties, cache);
    }

    @Override
    public RaceConfig<GoblinAbilitiesConfig> getConfig() {
        return TrinketsConfig.SERVER.races.goblin;
    }

    @Override
    public void registerRaceAbilities() {
        final GoblinAbilitiesConfig abilities = this.getConfig().abilities;
        if (abilities == null) {
            return;
        }
        this.addAbility(new AbilityClimbing(abilities.climbing));
        this.addAbility(new AbilityWolfMount(abilities.wolfRider));
    }

    @Override
    public void describeTraits(DescriptionVariables variables) {
        final GoblinAbilitiesConfig abilities = this.getConfig().abilities;
        if (abilities == null) {
            return;
        }
        variables.flag("resist", abilities.explosiveResistance.get())
                .number("health", EXPLOSION_HEALTH_BASE)
                .percent("min", true, MIN_EXPLOSION_MULTIPLIER)
                .number("fire", FIRE_MULTIPLIER)
                .number("lava", LAVA_MULTIPLIER)
                .flag("friendly", abilities.friendlyCreepers.get())
                .flag("explode", abilities.creepersExplodeOnContact.get());
    }

    @Override
    public boolean targetedByEnemy(LivingEntity enemy) {
        final GoblinAbilitiesConfig abilities = this.getConfig().abilities;
        return abilities == null || !abilities.friendlyCreepers.get() || !(enemy instanceof Creeper);
    }

    /** 与 1.12 一致：先走通用伤害规则，再按伤害类型乘本族抗性 */
    @Override
    public float isHurt(DamageSource source, float dmg) {
        final float scaled = super.isHurt(source, dmg);
        final GoblinAbilitiesConfig abilities = this.getConfig().abilities;
        final LivingEntity entity = this.getEntity();
        if (abilities == null || entity == null || !abilities.explosiveResistance.get()) {
            return scaled;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return scaled * Mth.clamp(entity.getHealth() / EXPLOSION_HEALTH_BASE, MIN_EXPLOSION_MULTIPLIER, 1F);
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return scaled * (source.is(DamageTypes.LAVA) ? LAVA_MULTIPLIER : FIRE_MULTIPLIER);
        }
        return scaled;
    }

    @Override
    public boolean attackedEntity(LivingEntity target, DamageSource source, float dmg) {
        final boolean attack = super.attackedEntity(target, source, dmg);
        final GoblinAbilitiesConfig abilities = this.getConfig().abilities;
        if (attack && abilities != null && abilities.creepersExplodeOnContact.get() && target instanceof Creeper creeper) {
            if (!creeper.isIgnited()) {
                creeper.ignite();
            }
            if (creeper.getSwellDir() == -1) {
                creeper.setSwellDir(1);
            }
        }
        return attack;
    }
}
