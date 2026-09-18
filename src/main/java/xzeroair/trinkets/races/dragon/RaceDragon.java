package xzeroair.trinkets.races.dragon;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.traits.abilities.AbilityCreativeFlight;
import xzeroair.trinkets.traits.abilities.AbilityElytraFlight;
import xzeroair.trinkets.traits.abilities.AbilityGreedyEyes;
import xzeroair.trinkets.traits.abilities.AbilityNightVision;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.compat.SurvivalAbilities;
import xzeroair.trinkets.traits.abilities.elements.AbilityBreath;
import xzeroair.trinkets.traits.abilities.elements.fire.AbilityFireImmunity;
import xzeroair.trinkets.traits.abilities.elements.ice.AbilityFrostWalker;
import xzeroair.trinkets.traits.abilities.elements.ice.AbilityIceImmunity;
import xzeroair.trinkets.traits.abilities.elements.lightning.AbilityLightningBolt;
import xzeroair.trinkets.traits.abilities.elements.lightning.AbilityLightningImmunity;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.RaceConfig;
import xzeroair.trinkets.util.config.server.race.DragonAbilitiesConfig;
import xzeroair.trinkets.util.config.server.race.ElementOverrideConfig;

/**
 * 巨龙种族的行为处理器。
 *
 * 能力随主元素分支（与 1.12 一致）：火龙火焰免疫、冰龙冰霜免疫 + 霜行者、雷龙雷电免疫，
 * 无元素的中性龙只有火焰免疫。元素能力都带 requiredElement，元素改变时由 AbilityHandler 移除。
 *
 * 火/冰/雷形态的效果、免疫、伤害规则与属性整体替换通用配置（elementOverrides）。
 *
 * 移植说明：「效果增删 / 药水免疫 / 坐骑黑白名单」三段与其余 8 族相同的逻辑已上移至基类，
 * 1.12 里四处按元素 if-else 的重复分支收敛为一组 getter 覆写。
 */
public class RaceDragon extends EntityRacePropertiesHandler {

    public RaceDragon(@Nullable LivingEntity entity, @Nullable EntityProperties properties, @Nonnull RaceCache cache) {
        super(entity, properties, cache);
    }

    @Override
    public RaceConfig<DragonAbilitiesConfig> getConfig() {
        return TrinketsConfig.SERVER.races.dragon;
    }

    @Override
    public void registerRaceAbilities() {
        final DragonAbilitiesConfig abilities = this.getConfig().abilities;
        if (abilities == null) {
            return;
        }
        this.addAbility(new AbilityCreativeFlight(abilities.flight));
        this.addAbility(new AbilityElytraFlight(abilities.elytraFlight));
        this.addAbility(new AbilityNightVision(abilities.nightVision));
        final Element element = this.getRaceCache().getPrimaryElement();
        if (element == ModElements.FIRE.get()) {
            this.addAbility(new AbilityFireImmunity(abilities.fire.fireImmunity).setRequiredElement(element));
            this.addAbility(AbilityBreath.fire(abilities.fire.fireBreath).setRequiredElement(element));
        } else if (element == ModElements.ICE.get()) {
            this.addAbility(new AbilityIceImmunity(abilities.ice.iceImmunity).setRequiredElement(element));
            this.addAbility(new AbilityFrostWalker(abilities.ice.frostWalker).setRequiredElement(element));
            this.addAbility(AbilityBreath.ice(abilities.ice.iceBreath).setRequiredElement(element));
        } else if (element == ModElements.LIGHTNING.get()) {
            this.addAbility(new AbilityLightningImmunity(abilities.lightning.lightningImmunity).setRequiredElement(element));
            this.addAbility(new AbilityLightningBolt(abilities.lightning.lightningBolt).setRequiredElement(element));
            this.addAbility(AbilityBreath.lightning(abilities.lightning.lightningBreath).setRequiredElement(element));
        } else {
            this.addAbility(new AbilityFireImmunity(abilities.fireImmunity));
            this.addAbility(AbilityBreath.dragon(abilities.dragonBreath));
        }
        this.addAbility(new AbilityGreedyEyes(abilities.greedyEyes));
    }

    /** 火/冰/雷形态用各自的生存联动段并限定元素；中性形态沿用本族通用段（1.12 中性龙不加生存能力，通用段默认全关，行为一致） */
    @Override
    protected void registerSurvivalAbilities() {
        final DragonAbilitiesConfig abilities = this.getConfig().abilities;
        final Element element = this.getRaceCache().getPrimaryElement();
        if (abilities != null && element == ModElements.FIRE.get()) {
            SurvivalAbilities.addTo(this::addAbility, abilities.fire.survival, element);
        } else if (abilities != null && element == ModElements.ICE.get()) {
            SurvivalAbilities.addTo(this::addAbility, abilities.ice.survival, element);
        } else if (abilities != null && element == ModElements.LIGHTNING.get()) {
            SurvivalAbilities.addTo(this::addAbility, abilities.lightning.survival, element);
        } else {
            super.registerSurvivalAbilities();
        }
    }

    /** 火/冰/雷形态的替换配置；中性形态返回 null（沿用本族通用配置） */
    @Nullable
    private ElementOverrideConfig elementOverrides() {
        final DragonAbilitiesConfig abilities = this.getConfig().abilities;
        if (abilities == null) {
            return null;
        }
        final Element element = this.getRaceCache().getPrimaryElement();
        if (element == ModElements.FIRE.get()) {
            return abilities.fire.overrides;
        }
        if (element == ModElements.ICE.get()) {
            return abilities.ice.overrides;
        }
        return element == ModElements.LIGHTNING.get() ? abilities.lightning.overrides : null;
    }

    @Override
    public List<? extends String> getEffectsToAdd() {
        final ElementOverrideConfig overrides = this.elementOverrides();
        return overrides != null ? overrides.effectsToAdd.get() : super.getEffectsToAdd();
    }

    @Override
    public List<? extends String> getEffectsToRemove() {
        final ElementOverrideConfig overrides = this.elementOverrides();
        return overrides != null ? overrides.effectsToRemove.get() : super.getEffectsToRemove();
    }

    @Override
    public List<? extends String> getDamageTypesToIgnore() {
        final ElementOverrideConfig overrides = this.elementOverrides();
        return overrides != null ? overrides.damageTypesToIgnore.get() : super.getDamageTypesToIgnore();
    }

    @Override
    public List<? extends String> getAttributes() {
        final ElementOverrideConfig overrides = this.elementOverrides();
        return overrides != null ? overrides.attributes.get() : super.getAttributes();
    }

    /** 巨龙须显示外观特征（翅膀等）且飞行能力启用时才能飞（与 1.12 一致） */
    @Override
    public void describeTraits(DescriptionVariables variables) {
        final DragonAbilitiesConfig abilities = this.getConfig().abilities;
        variables.flag("wings", abilities != null && abilities.flight.isEnabled());
    }

    @Override
    public boolean canFly() {
        final DragonAbilitiesConfig abilities = this.getConfig().abilities;
        return super.canFly() && this.showTraits() && abilities != null && abilities.flight.isEnabled();
    }
}
