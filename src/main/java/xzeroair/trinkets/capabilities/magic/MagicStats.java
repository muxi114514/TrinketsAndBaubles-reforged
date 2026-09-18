package xzeroair.trinkets.capabilities.magic;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

import xzeroair.trinkets.attributes.UpdatingAttribute;
import xzeroair.trinkets.capabilities.Capabilities;
import xzeroair.trinkets.capabilities.CapabilityEntityBase;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.init.ModAttributes;
import xzeroair.trinkets.init.ModRaces;
import xzeroair.trinkets.network.NetworkHandler;
import xzeroair.trinkets.network.SyncManaCostToHudPacket;
import xzeroair.trinkets.network.SyncManaStatsPacket;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.ManaConfig;

/**
 * 实体魔力（对应 1.12 capabilities/magic/MagicStats）。服务端权威：数值只在服务端变化，变化即下发给本人。
 *
 * 最大魔力 = 属性值 + 基础值 × (亲和% − 100%)；亲和 = 亲和属性 + 当前种族的魔力亲和。
 * 回复：未满时每 manaUpdateTicks × 回复冷却倍率 tick 回复「回复属性值」点；花费魔力后暂停 manaRegenTimeout tick。
 *
 * 移植说明：1.12 在服务端 setMana 时对每次变化都发包，这里保持同样的「变化即同步」语义；
 * 1.12 的 sendInformationToTracking 按区块遍历广播，但全模组无调用点（魔力只需本人可见），不移植。
 */
public class MagicStats extends CapabilityEntityBase<MagicStats, LivingEntity> {

    private static final UUID MANA_BONUS_UUID = UUID.fromString("a3b8802c-e521-45c0-b126-eb45692f68eb");

    private float mana = 100F;
    private double bonusMana = 0;
    private boolean sync = false;
    private double manaUpdateTickRate = 0;
    private double manaRegenTimeout = 0;
    private float lastSyncedManaCost = Float.NaN;

    private final UpdatingAttribute manaBonus =
            new UpdatingAttribute("BonusMax", MANA_BONUS_UUID, ModAttributes.MAX_MANA).setSavedInNBT(true);

    public MagicStats(LivingEntity entity) {
        super(entity);
    }

    @Nullable
    public static MagicStats get(@Nullable LivingEntity entity) {
        return entity == null ? null : entity.getCapability(Capabilities.MAGIC_STATS).orElse(null);
    }

    private static ManaConfig config() {
        return TrinketsConfig.SERVER.magic;
    }

    // ── tick ──

    @Override
    public void onUpdate() {
        final double bonusPerPoint = config().bonusPerPoint.get();
        if (bonusPerPoint > 0) {
            this.manaBonus.addModifier(this.getEntity(), bonusPerPoint * this.getBonusMana(), 0);
        } else {
            this.manaBonus.removeModifier(this.getEntity());
        }
        if (this.getEntity().level().isClientSide) {
            this.sync = false;
            return;
        }
        if (!config().manaEnabled.get() || this.isCreativePlayer()) {
            this.refillMana();
            return;
        }
        if (this.onRegenCooldown()) {
            return;
        }
        if (this.getMana() > this.getMaxMana()) {
            this.setMana(this.getMaxMana());
        } else if (this.getMana() < this.getMaxMana()) {
            this.manaUpdateTickRate++;
            if (this.manaUpdateTickRate > config().manaUpdateTicks.get() * this.attributeValue(ModAttributes.MANA_REGEN_COOLDOWN.get(), 1D)) {
                this.addMana((float) this.attributeValue(ModAttributes.MANA_REGEN.get(), 1D));
                this.manaUpdateTickRate = 0;
            }
        }
        if (this.sync) {
            this.sync = false;
            this.sendInformationToPlayer();
        }
    }

    private boolean onRegenCooldown() {
        if (!config().manaEnabled.get() || this.manaRegenTimeout <= 0) {
            this.manaRegenTimeout = 0;
            return false;
        }
        this.manaRegenTimeout--;
        return true;
    }

    // ── 数值 ──

    public float getMana() {
        return this.mana;
    }

    /** 服务端专用；客户端的数值只来自同步包 */
    public void setMana(float mana) {
        if (this.getEntity().level().isClientSide) {
            return;
        }
        final float clamped = Math.min(Math.max(mana, 0), this.getMaxMana());
        if (this.mana != clamped) {
            this.mana = clamped;
            this.sendInformationToPlayer();
        }
    }

    public void addMana(float mana) {
        this.setMana(this.mana + mana);
    }

    public void refillMana() {
        if (this.getMana() != this.getMaxMana()) {
            this.setMana(this.getMaxMana());
        }
    }

    public boolean needMana() {
        return this.getMana() < this.getMaxMana();
    }

    /** 魔力系统关闭、创造模式或花费 ≤ 0 时恒可支付 */
    public boolean canSpendMana(float cost) {
        if (!Float.isFinite(cost)) {
            return false;
        }
        if (!config().manaEnabled.get() || this.isCreativePlayer() || cost <= 0F) {
            return true;
        }
        return cost <= this.getMana();
    }

    /** 支付魔力；不足时在动作栏提示并返回 false，成功支付后暂停回复 */
    public boolean spendMana(float cost) {
        final boolean charged = config().manaEnabled.get() && !this.isCreativePlayer() && Float.isFinite(cost) && cost > 0F;
        if (!this.canSpendMana(cost)) {
            if (charged && this.getEntity() instanceof Player player) {
                player.displayClientMessage(Component.translatable("xat.mana.insufficient"), true);
            }
            return false;
        }
        if (charged) {
            this.setMana(this.mana - cost);
            this.setManaRegenTimeout();
        }
        return true;
    }

    public float getMaxMana() {
        final AttributeInstance maxMana = this.getEntity().getAttribute(ModAttributes.MAX_MANA.get());
        if (maxMana == null) {
            return 100F;
        }
        final float max = (float) maxMana.getValue();
        final float affinityBonus = (float) (maxMana.getBaseValue() * (this.getMagicAffinity() * 0.01F) - maxMana.getBaseValue());
        return Math.max(max + affinityBonus, 0);
    }

    public double getBonusMana() {
        return this.bonusMana;
    }

    public void setBonusMana(double bonus) {
        this.bonusMana = Math.min(Math.max(bonus, 0), config().bonusMax.get());
        this.sendInformationToPlayer();
    }

    public void setManaRegenTimeout() {
        final double multiplier = this.attributeValue(ModAttributes.MANA_REGEN_COOLDOWN.get(), 1D);
        this.manaRegenTimeout = (int) (config().manaRegenTimeout.get() * multiplier);
    }

    /** 按倍数延长回复暂停时间（如末影女王在水中耗魔时为 2 倍） */
    public void setManaRegenTimeout(double factor) {
        this.setManaRegenTimeout();
        this.manaRegenTimeout *= factor;
    }

    public double getManaRegenTimeout() {
        return this.manaRegenTimeout;
    }

    public int getMagicAffinity() {
        final AttributeInstance affinity = this.getEntity().getAttribute(ModAttributes.MAGIC_AFFINITY.get());
        if (affinity == null) {
            return 0;
        }
        return (int) affinity.getValue() + this.getRacialAffinity();
    }

    public int getRacialAffinity() {
        final EntityProperties properties = EntityProperties.get(this.getEntity());
        return properties != null
                ? properties.getCurrentRace().getMagicAffinity()
                : ModRaces.NONE.get().getMagicAffinity();
    }

    private double attributeValue(Attribute attribute, double fallback) {
        final AttributeInstance instance = this.getEntity().getAttribute(attribute);
        return instance != null ? instance.getValue() : fallback;
    }

    // ── 同步 ──

    @Override
    public void onJoinWorld() {
        this.sendInformationToPlayer();
    }

    public void sendInformationToPlayer() {
        if (!this.getEntity().level().isClientSide && this.getEntity() instanceof ServerPlayer player) {
            NetworkHandler.sendTo(new SyncManaStatsPacket(this.getEntity(), this.saveToNBT(new CompoundTag())), player);
        }
    }

    /** 把能力即将消耗的魔力告诉本人的魔力条（数值不变不重复发包） */
    public void syncManaCostToHud(float cost) {
        if (this.getEntity().level().isClientSide || !(this.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        final float rounded = Math.round(cost * 1000.0F) / 1000.0F;
        if (Float.compare(this.lastSyncedManaCost, rounded) != 0) {
            this.lastSyncedManaCost = rounded;
            NetworkHandler.sendTo(new SyncManaCostToHudPacket(rounded), player);
        }
    }

    // ── 存取 ──

    /** 死亡且不保留物品栏时魔力回满，否则保留；额外上限点数总是保留 */
    @Override
    public void copyFrom(@Nonnull MagicStats source, boolean wasDeath, boolean keepInv) {
        this.bonusMana = source.bonusMana;
        this.mana = wasDeath && !keepInv ? this.getMaxMana() : source.mana;
        this.sync = true;
    }

    @Override
    public CompoundTag saveToNBT(@Nonnull CompoundTag tag) {
        tag.putFloat("mana", this.mana);
        tag.putDouble("bonus_mana", this.bonusMana);
        return tag;
    }

    @Override
    public void loadFromNBT(@Nonnull CompoundTag tag) {
        if (tag.contains("mana")) {
            this.mana = tag.getFloat("mana");
        }
        if (tag.contains("bonus_mana")) {
            this.bonusMana = tag.getDouble("bonus_mana");
        }
    }
}
