package xzeroair.trinkets.traits.abilities;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.entity.ai.EnderQueens;
import xzeroair.trinkets.events.EnderQueenHandler;
import xzeroair.trinkets.init.ModDamageTypes;
import xzeroair.trinkets.init.ModItems;
import xzeroair.trinkets.network.StatusMessagePacket;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.ManaCost;
import xzeroair.trinkets.traits.abilities.base.AbilityItemToggle;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IKeyBindInterface;
import xzeroair.trinkets.traits.abilities.interfaces.IPotionAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.KeyNames;
import xzeroair.trinkets.util.config.abilities.EnderQueenAbilityConfig;
import xzeroair.trinkets.util.helpers.EntityChecks;
import xzeroair.trinkets.util.helpers.TrinketHelper;

/**
 * 末影女王（末影王冠）：受击时概率召唤末影骑士、概率无视伤害、受远程伤害时耗魔瞬移；
 * 按键随机瞬移，辅助键打开末影箱；可选怕水。末影人不再仇视女王、也不掉落（由 events/EnderQueenHandler 配合）。
 * 对应 1.12 AbilityEnderQueen。
 *
 * 移植说明：1.12 召唤与无视伤害的提示是硬编码英文，这里改为语言键。VIP 语录不移植。
 */
public class AbilityEnderQueen extends AbilityItemToggle implements ITickableAbility, IPotionAbility, IAttackAbility,
        IKeyBindInterface {

    private static final float ACTIVE_COST = 0.5F;
    private static final float HURT_COST = 0.25F;
    /** 随机传送的水平范围（±格） */
    private static final double TELEPORT_RANGE = 16.0D;
    private static final float WATER_MANA_COST = 5F;
    private static final float WATER_DAMAGE = 2F;
    private static final float WATER_DAMAGE_WITH_DRAGONS_EYE = 4F;
    private static final ResourceLocation INSTABILITY = new ResourceLocation("lycanitesmobs", "instability");

    private final EnderQueenAbilityConfig config;

    public AbilityEnderQueen(@Nonnull EnderQueenAbilityConfig config) {
        super(AbilityNames.ENDER_QUEEN, Flag.MAIN, true);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    protected boolean isSourceItem(ItemStack stack) {
        return stack.is(ModItems.ENDER_TIARA.get());
    }

    @Override
    public void onAbilityAdded(LivingEntity entity) {
        if (!entity.level().isClientSide) {
            this.syncFromStack(this.sourceStack(entity), entity);
        }
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }
        this.syncFromStack(this.sourceStack(entity), entity);
        if (this.config.waterHurts.get() && entity.tickCount % 20 == 0 && entity.isInWaterOrRain()) {
            this.hurtByWater(entity);
        }
    }

    /** 水中先扣魔力（并加倍回复暂停），魔力不足则受水伤；同时戴着龙之眼时伤害翻倍 */
    private void hurtByWater(LivingEntity entity) {
        final MagicStats magic = MagicStats.get(entity);
        if (magic != null && magic.spendMana(WATER_MANA_COST)) {
            magic.setManaRegenTimeout(2.0D);
            return;
        }
        final boolean dragonsEye = TrinketHelper.isEquipped(entity, stack -> stack.is(ModItems.DRAGONS_EYE.get()));
        entity.hurt(ModDamageTypes.source(entity.level(), ModDamageTypes.WATER), dragonsEye ? WATER_DAMAGE_WITH_DRAGONS_EYE : WATER_DAMAGE);
    }

    @Override
    public boolean potionApplied(LivingEntity entity, MobEffectInstance effect, boolean cancel) {
        return INSTABILITY.equals(ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect())) || cancel;
    }

    @Override
    public boolean attacked(LivingEntity attacked, DamageSource source, float dmg, boolean cancel) {
        if (attacked.level().isClientSide || !(source.getEntity() instanceof LivingEntity attacker) || attacker == attacked
                || dmg <= 0) {
            return cancel;
        }
        this.summonKnight(attacked, attacker);
        this.teleportOnHurt(attacked, source);
        return this.ignoreDamage(attacked) || cancel;
    }

    private boolean roll(int chance) {
        return chance > 0 && this.random.nextInt(chance) == 0;
    }

    private void summonKnight(LivingEntity queen, LivingEntity attacker) {
        if (!this.roll(this.config.spawnChance.get())) {
            return;
        }
        final EnderMan knight = new EnderMan(EntityType.ENDERMAN, queen.level());
        knight.moveTo(queen.blockPosition(), 0.0F, 0.0F);
        knight.getPersistentData().putBoolean(EnderQueens.SUMMONED_TAG, true);
        knight.getPersistentData().putString(EnderQueens.QUEEN_UUID_TAG, queen.getStringUUID());
        knight.setCanPickUpLoot(false);
        queen.level().addFreshEntity(knight);
        knight.setTarget(attacker);
        new StatusMessagePacket(this.getTranslationKey() + ".summon", false).send(queen);
    }

    private boolean ignoreDamage(LivingEntity queen) {
        if (!this.roll(this.config.ignoreChance.get())) {
            return false;
        }
        new StatusMessagePacket(this.getTranslationKey() + ".protect", true).send(queen);
        return true;
    }

    /** 远程（间接）伤害时概率耗魔瞬移；Boss 的攻击与举盾时不触发 */
    private void teleportOnHurt(LivingEntity queen, DamageSource source) {
        if (!this.isAbilityToggled() || source.getDirectEntity() == source.getEntity() || !this.roll(this.config.teleportChance.get())
                || EntityChecks.isBoss(source.getEntity()) || queen.isBlocking()) {
            return;
        }
        final MagicStats magic = MagicStats.get(queen);
        if (magic != null && magic.canSpendMana(magic.getMaxMana() * HURT_COST)) {
            this.teleportRandomly(queen, magic, magic.getMaxMana() * HURT_COST);
        }
    }

    /** 最多尝试 32 个随机落点，成功才扣魔力；全部失败提示被阻止 */
    private boolean teleportRandomly(LivingEntity entity, MagicStats magic, float cost) {
        for (int i = 0; i < 32; i++) {
            final double x = entity.getX() + (this.random.nextDouble() - 0.5D) * TELEPORT_RANGE * 2;
            final double y = entity.getY() + (this.random.nextInt(16) - 8);
            final double z = entity.getZ() + (this.random.nextDouble() - 0.5D) * TELEPORT_RANGE * 2;
            if (entity.isPassenger()) {
                entity.stopRiding();
            }
            if (EnderQueens.teleport(entity, x, y, z)) {
                magic.spendMana(cost);
                entity.fallDistance = 0.0F;
                if (entity instanceof Mob mob) {
                    mob.getNavigation().stop();
                }
                return true;
            }
        }
        new StatusMessagePacket(this.getTranslationKey() + ".teleport.failed", true).send(entity);
        return false;
    }

    @Override
    public boolean targetedByEnemy(LivingEntity enemy, boolean cancel) {
        // 1.20.1 的换目标事件在赋值前触发，事件内 setTarget(null) 会被覆盖，须取消事件
        return cancel || (!this.config.endermanRetaliate.get() && enemy instanceof EnderMan);
    }

    @Override
    public int killedEntityExpDrop(LivingEntity target, int originalExp, int droppedExp) {
        return !this.config.endermanDropExp.get() && target instanceof EnderMan ? 0 : droppedExp;
    }

    @Override
    public void killedEntityItemDrops(LivingEntity target, DamageSource source, int lootingLevel, Collection<ItemEntity> drops) {
        if (!this.config.endermanDropItems.get() && target instanceof EnderMan) {
            drops.clear();
        }
    }

    @Override
    public float damageEntity(LivingEntity target, DamageSource source, float dmg) {
        if (target instanceof EnderMan && dmg > 0) {
            target.getPersistentData().putBoolean(EnderQueens.SUMMONED_TAG, true);
        }
        return dmg;
    }

    @Override
    protected void sendToggleMessage(LivingEntity entity) {
        final StatusMessagePacket message = new StatusMessagePacket(this.getTranslationKey() + ".teleport", true);
        if (this.config.teleportChance.get() > 0) {
            message.withKey("toggle", this.isAbilityToggled() ? "xat.tooltip.on" : "xat.tooltip.off");
        }
        message.send(entity);
    }

    /** 主按键：随机瞬移（半管魔力）；辅助键：打开末影箱（整管魔力） */
    @Override
    public boolean onKeyPress(Entity entity, boolean aux) {
        if (!(entity instanceof LivingEntity living) || living.level().isClientSide) {
            return true;
        }
        final MagicStats magic = MagicStats.get(living);
        if (magic == null) {
            return false;
        }
        if (!aux) {
            final float cost = magic.getMaxMana() * ACTIVE_COST;
            if (this.config.teleportChance.get() > 0 && magic.canSpendMana(cost)) {
                return this.teleportRandomly(living, magic, cost);
            }
            return false;
        }
        if (this.config.enderChest.get() && living instanceof Player player && magic.spendMana(magic.getMaxMana())) {
            player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> ChestMenu.threeRows(id, inventory,
                    owner.getEnderChestInventory()), Component.translatable("container.enderchest")));
            player.awardStat(Stats.OPEN_ENDERCHEST);
            return true;
        }
        return false;
    }

    @Override
    public String getKey() {
        return KeyNames.ENDER_CROWN;
    }

    @Override
    public String getAuxKey() {
        return KeyNames.AUX_KEY;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final int ignore = this.config.ignoreChance.get();
        final int spawn = this.config.spawnChance.get();
        final int teleport = this.config.teleportChance.get();
        variables.oneIn("ignore", ignore > 0, ignore)
                .oneIn("spawn", spawn > 0, spawn)
                .flag("follow", spawn > 0 && this.config.endermanFollow.get())
                .oneIn("teleport", teleport > 0, teleport)
                .number("range", TELEPORT_RANGE)
                .percent("hurtcost", teleport > 0, HURT_COST)
                .percent("activecost", teleport > 0, ACTIVE_COST)
                .keybind("key", KeyNames.ENDER_CROWN)
                .keybind("aux", KeyNames.AUX_KEY)
                .flag("chest", this.config.enderChest.get())
                .flag("peace", !this.config.endermanRetaliate.get())
                .flag("noexp", !this.config.endermanDropExp.get())
                .flag("noitems", !this.config.endermanDropItems.get())
                .number("watercost", this.config.waterHurts.get(), WATER_MANA_COST)
                .number("waterdamage", WATER_DAMAGE)
                .number("waterdamageeye", WATER_DAMAGE_WITH_DRAGONS_EYE)
                .number("blockradius", this.config.blockTeleportation.get(), EnderQueenHandler.BLOCK_RADIUS)
                .number("blockheight", EnderQueenHandler.BLOCK_HEIGHT);
    }

    @Override
    public void describeStatus(DescriptionVariables variables, ItemStack source) {
        variables.toggle("teleport", this.config.teleportChance.get() > 0, this.isAbilityToggled());
    }

    @Override
    public ManaCost getManaCost() {
        return this.config.teleportChance.get() > 0 ? ManaCost.of(ACTIVE_COST, ManaCost.Unit.MAX_PERCENT) : null;
    }
}
