package xzeroair.trinkets.races.elf;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import xzeroair.trinkets.attributes.UpdatingAttribute;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.init.ModAttributes;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.traits.abilities.AbilitySkilledArcher;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.RaceConfig;
import xzeroair.trinkets.util.config.server.race.ElfAbilitiesConfig;

/**
 * 精灵种族的行为处理器：熟练弓手；身处森林类生物群系时移速 +20%、攻速 +50%、跳跃 +20%。
 *
 * 移植说明：
 * - 「效果增删 / 药水免疫 / 坐骑黑白名单」三段与其余 8 族相同的逻辑已上移至基类。
 * - 1.12 的 BiomeDictionary.Type.FOREST 对应 1.20.1 的 #minecraft:is_forest 标签。
 */
public class RaceElf extends EntityRacePropertiesHandler {

    public static final double FOREST_SPEED_BONUS = 0.2D;
    public static final double FOREST_ATTACK_SPEED_BONUS = 0.5D;
    public static final double FOREST_JUMP_BONUS = 0.2D;
    private static final UUID FOREST_BONUS_UUID = UUID.fromString("628dedc0-5f63-4b45-bccb-ecb0fe881b49");

    private final UpdatingAttribute bonusSpeed = new UpdatingAttribute("xat.elf.forest_speed", FOREST_BONUS_UUID,
            () -> Attributes.MOVEMENT_SPEED);
    private final UpdatingAttribute bonusAttackSpeed = new UpdatingAttribute("xat.elf.forest_attack_speed", FOREST_BONUS_UUID,
            () -> Attributes.ATTACK_SPEED);
    private final UpdatingAttribute bonusJump = new UpdatingAttribute("xat.elf.forest_jump", FOREST_BONUS_UUID, ModAttributes.JUMP);

    public RaceElf(@Nullable LivingEntity entity, @Nullable EntityProperties properties, @Nonnull RaceCache cache) {
        super(entity, properties, cache);
    }

    @Override
    public RaceConfig<ElfAbilitiesConfig> getConfig() {
        return TrinketsConfig.SERVER.races.elf;
    }

    @Override
    public void registerRaceAbilities() {
        final ElfAbilitiesConfig abilities = this.getConfig().abilities;
        if (abilities == null) {
            return;
        }
        this.addAbility(new AbilitySkilledArcher(abilities.skilledArcher));
    }

    @Override
    public void describeTraits(DescriptionVariables variables) {
        variables.percent("speed", true, FOREST_SPEED_BONUS)
                .percent("attack", true, FOREST_ATTACK_SPEED_BONUS)
                .percent("jump", true, FOREST_JUMP_BONUS);
    }

    @Override
    public void whileTransformed() {
        super.whileTransformed();
        final LivingEntity entity = this.getEntity();
        if (entity == null || entity.level().isClientSide) {
            return;
        }
        // 实体自身所在位置必在已加载区块内
        if (entity.level().getBiome(entity.blockPosition()).is(BiomeTags.IS_FOREST)) {
            this.bonusSpeed.addModifier(entity, FOREST_SPEED_BONUS, 2);
            this.bonusAttackSpeed.addModifier(entity, FOREST_ATTACK_SPEED_BONUS, 2);
            this.bonusJump.addModifier(entity, FOREST_JUMP_BONUS, 2);
        } else {
            this.removeForestBonus();
        }
    }

    @Override
    public void endTransformation() {
        super.endTransformation();
        this.removeForestBonus();
    }

    private void removeForestBonus() {
        this.bonusSpeed.removeModifier(this.getEntity());
        this.bonusAttackSpeed.removeModifier(this.getEntity());
        this.bonusJump.removeModifier(this.getEntity());
    }
}
