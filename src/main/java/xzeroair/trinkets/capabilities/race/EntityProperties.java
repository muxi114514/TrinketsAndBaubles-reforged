package xzeroair.trinkets.capabilities.race;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import xzeroair.trinkets.api.events.TransformationEvent;
import xzeroair.trinkets.capabilities.Capabilities;
import xzeroair.trinkets.capabilities.CapabilityEntityBase;
import xzeroair.trinkets.entity.AlphaWolf;
import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.init.ModRaces;
import xzeroair.trinkets.network.NetworkHandler;
import xzeroair.trinkets.network.OpenRaceSelectionPacket;
import xzeroair.trinkets.network.SyncRaceDataPacket;
import xzeroair.trinkets.races.EntityRace;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.races.IRaceProvider;
import xzeroair.trinkets.races.RaceAttributes;
import xzeroair.trinkets.traits.AbilityHandler;
import xzeroair.trinkets.traits.elements.ItemElements;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.handlers.Counter;
import xzeroair.trinkets.util.helpers.NBTHelper;
import xzeroair.trinkets.util.helpers.TrinketHelper;

/**
 * 实体种族属性能力（对应 1.12 capabilities/race/EntityProperties）。
 *
 * 持有五份种族状态、种族行为处理器与能力管理器；服务端每 tick 解析种族（updateRace）并下发同步。
 * 1.12 的 KeybindHandler 移到客户端（ClientAbilityInput），ClientInfo 随 P6 客户端层处理。
 *
 * 移植说明：原版此类 1021 行、职责有七项（种族状态机 / 体型 / 属性修改 / 速度追踪 / 生命周期 /
 * 网络同步 / 客户端信息）。按既定方针先 1:1 移、跑通后再按职责拆，故此处保留其结构不做重新设计。
 */
public class EntityProperties extends CapabilityEntityBase<EntityProperties, LivingEntity> {

    public static final String TAG_KEY = Reference.MODID + ":race";
    private static final String TEMP_RACE_COUNTER = "TempRace";

    /** 体型缩放百分比，100 = 原始尺寸 */
    protected int widthValue = 100;
    protected int heightValue = 100;
    protected float defaultWidth;
    protected float defaultHeight;

    protected boolean firstLogin = true;
    protected boolean login = true;
    protected boolean isFake = false;
    protected boolean isChild;

    /** 待同步标记；网络层就绪后由 tick 消费 */
    protected boolean sync = false;
    protected boolean syncTracking = false;

    protected final AbilityHandler abilities;

    /**
     * 五份种族状态：
     * original 出生种族 / imbued 由物品附着的种族 / potion 药水临时种族 /
     * current 当前生效 / previous 上一个（体型渐变的起点）
     */
    protected RaceCache originalRace;
    protected RaceCache imbuedRace;
    protected RaceCache potionRace;
    protected RaceCache previousRace;
    /** 飞行速度是否由属性接管（仅运行期，两端各自维护） */
    private boolean flySpeedControlled;
    /** 种族选择授权剩余 tick（仅服务端运行期，不存盘；1.12 同为 60 秒） */
    private int raceSelectionTicks;
    protected RaceCache currentRace;

    protected EntityRacePropertiesHandler raceHandler;

    public EntityProperties(LivingEntity entity) {
        super(entity);
        this.defaultWidth = entity.getBbWidth();
        this.defaultHeight = entity.getBbHeight();
        this.isChild = entity.isBaby();
        this.abilities = new AbilityHandler(this);
        this.originalRace = new RaceCache();
        this.imbuedRace = this.originalRace;
        this.potionRace = this.originalRace;
        this.previousRace = this.originalRace;
        this.currentRace = this.originalRace;
        this.raceHandler = this.currentRace.getRace().getRaceHandler(entity, this, this.currentRace);
    }

    public AbilityHandler getAbilityHandler() {
        return this.abilities;
    }

    // ── 种族状态 ──

    public EntityRacePropertiesHandler getRaceHandler() {
        return this.raceHandler;
    }

    public RaceCache getOriginalRaceCache() {
        return this.originalRace;
    }

    public RaceCache getImbuedRaceCache() {
        return this.imbuedRace;
    }

    public RaceCache getPotionRaceCache() {
        return this.potionRace;
    }

    public RaceCache getPreviousRaceCache() {
        return this.previousRace;
    }

    public void setPreviousRaceCache(RaceCache cache) {
        this.previousRace = cache;
    }

    public RaceCache getCurrentRaceCache() {
        return this.currentRace;
    }

    public EntityRace getCurrentRace() {
        return this.currentRace.getRace();
    }

    public EntityRace getPreviousRace() {
        return this.previousRace.getRace();
    }

    public boolean hasRace() {
        return !this.getCurrentRace().isNone();
    }

    /**
     * 切换当前种族：记下上一形态作为体型渐变起点，重建行为处理器并跑一次变身初始化。
     * 体型本身不在此处改，由 handler 的 updateSize 每 tick 逼近目标值。
     */
    public void setCurrentRaceCache(RaceCache cache) {
        if (cache == null || this.currentRace.compare(cache)) {
            return;
        }
        if (this.raceHandler != null) {
            this.raceHandler.onTransformEnd();
        }
        // 撤掉上一个与当前种族的属性修饰，新形态渐变期间会按比例重新施加旧种族的部分
        RaceAttributes.removeAll(this.getEntity(), this.previousRace.getRace().getUUID(), this.currentRace.getRace().getUUID());
        // 变身时从狼王上下来（狼王只供哥布林骑乘）
        if (this.getEntity().getVehicle() instanceof AlphaWolf) {
            this.getEntity().stopRiding();
        }
        this.previousRace = this.currentRace;
        this.currentRace = cache;
        this.raceHandler = cache.getRace().getRaceHandler(this.getEntity(), this, cache);
        this.raceHandler.onTransform();
        this.scheduleResync();
    }

    public void setImbuedRaceCache(@Nullable RaceCache cache) {
        final RaceCache value = cache == null ? new RaceCache() : cache;
        if (!this.imbuedRace.compare(value)) {
            this.imbuedRace = value;
            this.scheduleResync();
        }
    }

    public void setPotionRaceCache(@Nullable RaceCache cache) {
        final RaceCache value = cache == null ? new RaceCache() : cache;
        if (!this.potionRace.compare(value)) {
            this.potionRace = value;
            this.scheduleResync();
        }
    }

    public void setOriginalRaceCache(@Nullable RaceCache cache) {
        final RaceCache value = cache == null ? new RaceCache() : cache;
        if (!this.originalRace.compare(value)) {
            this.originalRace = value;
            this.scheduleResync();
        }
    }

    public void setHeightValue(int value) {
        this.heightValue = value;
    }

    public void setWidthValue(int value) {
        this.widthValue = value;
    }

    /** 便捷取用；实体未附加本能力（如非玩家）时返回 null */
    @Nullable
    public static EntityProperties get(@Nullable LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.getCapability(Capabilities.ENTITY_PROPERTIES).orElse(null);
    }

    @Override
    public void onUpdatePre() {
        this.abilities.onUpdatePre(this.getEntity());
    }

    /** 顺序同 1.12 onPlayerUpdate：能力 tick → 解析种族 → 种族 tick → 能力收尾 */
    @Override
    public void onUpdate() {
        this.abilities.onUpdate(this.getEntity());
        this.updateRace();
        if (this.raceHandler != null) {
            this.raceHandler.onTick();
        }
        this.abilities.onUpdatePost(this.getEntity());
        this.syncIfNeeded();
        if (!this.getEntity().level().isClientSide && this.raceSelectionTicks > 0) {
            this.raceSelectionTicks--;
        }
        // 与 1.12 相同：首个 tick 之后即不再是「首次登录」
        this.login = false;
        this.firstLogin = false;
    }

    /**
     * 服务端每 tick 解析「此刻应有的种族」，与当前不同则变身。
     * 优先级：变身药水 → 唯一佩戴的变身饰品 → 食物附着种族 → 出生种族。
     * 临时种族（药水）到时由 TempRace 计时器清除。对应 1.12 EntityProperties#updateRace。
     *
     * 移植说明：1.12 的通用计数器循环不存在于此（该类只有 TempRace 一个计数器，由这里推进），
     * 若在 onUpdate 里再统一推进会让临时种族以两倍速度结束。
     */
    public void updateRace() {
        final Counter tempRace = this.getTickHandler().getCounter(TEMP_RACE_COUNTER);
        if (tempRace != null && tempRace.Tick()) {
            this.setPotionRaceCache(null);
            this.getTickHandler().removeCounter(TEMP_RACE_COUNTER);
        }
        if (this.getEntity().level().isClientSide) {
            return;
        }
        final TransformationEvent.RaceUpdateEvent update = new TransformationEvent.RaceUpdateEvent(this.getEntity(), this, this.resolveRace());
        if (MinecraftForge.EVENT_BUS.post(update) || !update.raceChanged()) {
            return;
        }
        final RaceCache newRace = update.getNewRaceCache() == null ? new RaceCache() : update.getNewRaceCache();
        MinecraftForge.EVENT_BUS.post(new TransformationEvent.EndTransformation(this.getEntity(), this, this.currentRace));
        this.setCurrentRaceCache(newRace);
        if (newRace.isTemporary() && newRace.getDuration() > 0) {
            this.getTickHandler().getCounter(TEMP_RACE_COUNTER, newRace.getDuration(), true, true, true, true).resetTick();
        }
        MinecraftForge.EVENT_BUS.post(new TransformationEvent.StartTransformation(this.getEntity(), this, this.currentRace));
    }

    /** fake 标记：当前种族来自药水或饰品（非「真实」种族），渲染与界面据此区分 */
    private RaceCache resolveRace() {
        this.isFake = true;
        if (!this.potionRace.getRace().isNone()) {
            return this.potionRace;
        }
        final ItemStack provider = this.getRaceProvider();
        if (!provider.isEmpty() && provider.getItem() instanceof IRaceProvider raceProvider) {
            return new RaceCache(raceProvider.getRace(), ItemElements.getPrimary(provider, ModElements.NEUTRAL.get()));
        }
        this.isFake = false;
        if (!this.imbuedRace.getRace().isNone()) {
            return this.imbuedRace;
        }
        return this.originalRace;
    }

    /** 恰好佩戴一件变身饰品时返回它；佩戴多件互相冲突，视为没有（与 1.12 一致） */
    public ItemStack getRaceProvider() {
        final List<ItemStack> providers = TrinketHelper.findEquipped(this.getEntity(), stack -> stack.getItem() instanceof IRaceProvider);
        return providers.size() == 1 ? providers.get(0) : ItemStack.EMPTY;
    }

    /**
     * 服务端权威：有待同步标记时把本能力的 NBT 下发给追踪者与自己。
     * 1.12 需 sendTo + sendToTracking 各发一次，此处 TRACKING_ENTITY_AND_SELF 一次覆盖。
     */
    protected void syncIfNeeded() {
        if (!this.needsSync() || this.getEntity().level().isClientSide) {
            return;
        }
        NetworkHandler.sendToTrackingAndSelf(
                new SyncRaceDataPacket(this.getEntity(), this.saveToNBT(new CompoundTag())),
                this.getEntity());
        this.clearSync();
    }

    @Override
    public void onLogin() {
        this.login = true;
        this.scheduleResync();
        if (this.firstLogin && TrinketsConfig.SERVER.races.selectionMenu.get() && this.getEntity() instanceof ServerPlayer player) {
            this.authorizeRaceSelection();
            NetworkHandler.sendTo(new OpenRaceSelectionPacket(true), player);
        }
    }

    /** 哥布林下线时从狼王上下来，狼王随之还原为原来的狼 */
    @Override
    public void onLogoff() {
        if (this.currentRace.compareRace(ModRaces.GOBLIN.get()) && this.getEntity().getVehicle() instanceof AlphaWolf) {
            this.getEntity().stopRiding();
        }
    }

    @Override
    public void onJoinWorld() {
        this.scheduleResync();
    }

    @Override
    public CompoundTag saveToNBT(CompoundTag compound) {
        compound.putInt("heightValue", this.heightValue);
        compound.putInt("widthValue", this.widthValue);
        compound.putFloat("default_height", this.defaultHeight);
        compound.putFloat("default_width", this.defaultWidth);
        compound.putBoolean("first_login", this.firstLogin);
        compound.putBoolean("fake", this.isFake);
        compound.putBoolean("child", this.isChild);
        this.getTickHandler().saveCountersToNBT(compound);
        this.abilities.saveAbilitiesToNBT(compound);
        compound.put("OriginalRace", this.originalRace.saveToNBT(new CompoundTag()));
        compound.put("ImbuedRace", this.imbuedRace.saveToNBT(new CompoundTag()));
        compound.put("PotionRace", this.potionRace.saveToNBT(new CompoundTag()));
        compound.put("PreviousRace", this.previousRace.saveToNBT(new CompoundTag()));
        compound.put("CurrentRace", this.currentRace.saveToNBT(new CompoundTag()));
        if (this.raceHandler != null) {
            this.raceHandler.savedNBTData(compound);
        }
        return compound;
    }

    @Override
    public void loadFromNBT(CompoundTag compound) {
        NBTHelper.hasInteger(compound, "heightValue", value -> this.heightValue = value);
        NBTHelper.hasInteger(compound, "widthValue", value -> this.widthValue = value);
        NBTHelper.hasFloat(compound, "default_height", value -> this.defaultHeight = value);
        NBTHelper.hasFloat(compound, "default_width", value -> this.defaultWidth = value);
        NBTHelper.hasBoolean(compound, "first_login", value -> this.firstLogin = value);
        NBTHelper.hasBoolean(compound, "fake", value -> this.isFake = value);
        NBTHelper.hasBoolean(compound, "child", value -> this.isChild = value);
        this.getTickHandler().loadCountersFromNBT(compound);
        this.abilities.loadAbilitiesFromNBT(compound);
        NBTHelper.hasTag(compound, "OriginalRace", tag -> this.originalRace = RaceCache.loadFromNBT(tag));
        NBTHelper.hasTag(compound, "ImbuedRace", tag -> this.imbuedRace = RaceCache.loadFromNBT(tag));
        NBTHelper.hasTag(compound, "PotionRace", tag -> this.potionRace = RaceCache.loadFromNBT(tag));
        NBTHelper.hasTag(compound, "PreviousRace", tag -> this.previousRace = RaceCache.loadFromNBT(tag));
        if (NBTHelper.hasTagCompound(compound, "CurrentRace")) {
            this.currentRace = RaceCache.loadFromNBT(compound.getCompound("CurrentRace"));
            this.raceHandler = this.currentRace.getRace().getRaceHandler(this.getEntity(), this, this.currentRace);
            this.raceHandler.loadNBTData(compound);
            this.raceHandler.initializeState();
        } else if (this.raceHandler != null) {
            this.raceHandler.loadNBTData(compound);
        }
    }

    @Override
    public void copyFrom(@Nonnull EntityProperties source, boolean wasDeath, boolean keepInv) {
        this.firstLogin = source.firstLogin;
        this.login = source.login;
        this.defaultWidth = source.defaultWidth;
        this.defaultHeight = source.defaultHeight;
        this.isChild = source.isChild;
        this.originalRace = source.originalRace;
        if (wasDeath && !keepInv) {
            // 死亡且不保留物品：当前种族回落到附着种族，没有则回到出生种族
            this.previousRace = source.currentRace;
            this.currentRace = source.imbuedRace.getRace().isNone() ? source.originalRace : source.imbuedRace;
            this.heightValue = this.currentRace.getRace().getRaceHeight();
            this.widthValue = this.currentRace.getRace().getRaceWidth();
            if (TrinketsConfig.SERVER.food.keepEffectsOnDeath.get()) {
                this.imbuedRace = source.imbuedRace;
            }
        } else {
            this.currentRace = source.currentRace;
            this.previousRace = source.previousRace;
            this.imbuedRace = source.imbuedRace;
            this.potionRace = source.potionRace;
            this.heightValue = source.heightValue;
            this.widthValue = source.widthValue;
        }
        this.raceHandler = this.currentRace.getRace().getRaceHandler(this.getEntity(), this, this.currentRace);
        if (source.raceHandler != null) {
            this.raceHandler.copyFrom(source.raceHandler, wasDeath, keepInv);
        }
        this.abilities.copyFrom(source.getAbilityHandler(), wasDeath, keepInv);
        this.scheduleResync();
    }

    public void scheduleResync() {
        this.sync = true;
        this.syncTracking = true;
    }

    public boolean needsSync() {
        return this.sync;
    }

    public void clearSync() {
        this.sync = false;
        this.syncTracking = false;
    }

    public boolean isFirstLogin() {
        return this.firstLogin;
    }

    public void setFirstLogin(boolean firstLogin) {
        this.firstLogin = firstLogin;
    }

    /** 允许在接下来 60 秒内提交一次种族选择（首次登录开启选择菜单时、或由指令授予） */
    public void authorizeRaceSelection() {
        this.raceSelectionTicks = 20 * 60;
    }

    public boolean isRaceSelectionAuthorized() {
        return this.raceSelectionTicks > 0;
    }

    public void consumeRaceSelectionAuthorization() {
        this.raceSelectionTicks = 0;
    }

    public boolean isFake() {
        return this.isFake;
    }

    public boolean isChild() {
        return this.isChild;
    }

    public int getWidthValue() {
        return this.widthValue;
    }

    public int getHeightValue() {
        return this.heightValue;
    }

    public float getDefaultWidth() {
        return this.defaultWidth;
    }

    public float getDefaultHeight() {
        return this.defaultHeight;
    }

    public boolean isFlySpeedControlled() {
        return this.flySpeedControlled;
    }

    public void setFlySpeedControlled(boolean controlled) {
        this.flySpeedControlled = controlled;
    }
}
