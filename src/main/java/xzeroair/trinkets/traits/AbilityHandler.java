package xzeroair.trinkets.traits;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.Trinkets;
import xzeroair.trinkets.api.ItemHandlerType;
import xzeroair.trinkets.api.SlotInformation;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.network.AbilityCacheSyncPacket;
import xzeroair.trinkets.network.NetworkHandler;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;
import xzeroair.trinkets.traits.abilities.interfaces.IContainerAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IEquippedAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IHeldAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableInventoryAbility;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.helpers.NBTHelper;

/**
 * 能力管理器：按注册名持有实体当前生效的能力，负责注册/替换/移除、每 tick 分发与存取。
 *
 * 移植说明：
 * - 能力实例是每实体一份的运行时对象，不是注册表条目；其归属由 AbilityHolder 记录的
 *   来源 id 与槽位信息决定，来源失效（脱下饰品、换种族、药水结束）时能力自动移除。
 * - 禁用令（kill order）是配置把某能力关掉时的标记，存在实体持久化 NBT 中并下发客户端，
 *   使两端对「该来源的该能力不生效」达成一致。
 * - active 用 TreeMap：实例随 capability 挂单个实体、只在其所属线程访问，有序遍历还能让 NBT 稳定。
 */
public class AbilityHandler {

    public static final String CAP_KEY = Reference.MODID + ":abilities";
    private static final String DISABLED_SOURCES = "DisabledSources";

    private final EntityProperties parentProperties;
    protected Map<String, AbilityHolder> active = new TreeMap<>();
    protected boolean hasChanged = false;

    public AbilityHandler(EntityProperties properties) {
        this.parentProperties = properties;
    }

    public EntityProperties getParentProperties() {
        return this.parentProperties;
    }

    public Map<String, AbilityHolder> getActiveAbilities() {
        return this.active;
    }

    public LivingEntity getOwner() {
        return this.parentProperties.getEntity();
    }

    // ---- 注册 ----

    public void registerAbilities(LivingEntity entity, String source, @Nonnull List<? extends IAbilityInterface> abilities) {
        for (IAbilityInterface ability : abilities) {
            this.registerAbility(entity, source, new SlotInformation(ItemHandlerType.OTHER), ability);
        }
    }

    public void registerAbilities(LivingEntity entity, String source, SlotInformation info,
            @Nonnull List<? extends IAbilityInterface> abilities) {
        for (IAbilityInterface ability : abilities) {
            this.registerAbility(entity, source, info, ability);
        }
    }

    public IAbilityInterface registerRaceAbility(LivingEntity entity, String source, IAbilityInterface ability) {
        return this.replaceAbility(entity, source, new SlotInformation(ItemHandlerType.RACE), ability);
    }

    /**
     * 添加或替换能力。
     *
     * @return 未能替换时返回传入的能力；成功新增返回 null；发生替换则返回被替换掉的旧能力
     */
    @Nullable
    public IAbilityInterface replaceAbility(@Nonnull LivingEntity entity, String source, SlotInformation info,
            @Nonnull IAbilityInterface ability) {
        final String key = ability.getRegistryName().toString();
        if (this.blockedByKillOrder(entity, source, key, ability)) {
            return ability;
        }
        if (ability.shouldRemove()) {
            return ability;
        }
        if (info == null) {
            info = new SlotInformation(ItemHandlerType.OTHER);
        }
        final AbilityHolder value = this.active.get(key);
        if (value == null) {
            this.put(key, new AbilityHolder(this, source, info, ability));
            return null;
        }
        if (!value.sameAbilityOrigin(source, info, ability)) {
            value.getAbility().onAbilityRemoved(entity);
            this.put(key, new AbilityHolder(this, source, info, ability));
            return value.getAbility();
        }
        return ability;
    }

    @Nullable
    public IAbilityInterface registerAbility(LivingEntity entity, String source, IAbilityInterface ability) {
        return this.registerAbility(entity, source, new SlotInformation(ItemHandlerType.OTHER), ability);
    }

    /** 仅当该能力尚无归属时添加 */
    @Nullable
    public IAbilityInterface registerAbility(@Nonnull LivingEntity entity, String source, SlotInformation info,
            @Nonnull IAbilityInterface ability) {
        final String key = ability.getRegistryName().toString();
        if (this.blockedByKillOrder(entity, source, key, ability)) {
            return ability;
        }
        if (ability.shouldRemove()) {
            return ability;
        }
        if (info == null) {
            info = new SlotInformation(ItemHandlerType.OTHER);
        }
        if (!this.active.containsKey(key)) {
            this.put(key, new AbilityHolder(this, source, info, ability));
            return null;
        }
        return ability;
    }

    private void put(String key, AbilityHolder holder) {
        holder.getAbility().setFirstUpdate(true);
        this.active.put(key, holder);
    }

    /** 处理被配置关闭的能力：服务端下达并广播禁用令，客户端遇到禁用令直接拒绝注册 */
    private boolean blockedByKillOrder(LivingEntity entity, String source, String key, IAbilityInterface ability) {
        if (entity.level().isClientSide) {
            return this.hasKillOrder(source, key);
        }
        if (!ability.isAbilityEnabled()) {
            if (!this.hasKillOrder(source, key)) {
                this.addKillOrder(source, key);
                this.sendKillOrder(entity, source, key);
            }
            return true;
        }
        if (this.hasKillOrder(source, key)) {
            this.removeKillOrder(source, key);
        }
        return false;
    }

    @Nullable
    public IAbilityInterface removeAbility(String ability) {
        final AbilityHolder oldHolder = this.active.remove(ability);
        if (oldHolder == null) {
            return null;
        }
        final IAbilityInterface oldAbility = oldHolder.getAbility();
        oldAbility.onAbilityRemoved(this.getOwner());
        return oldAbility;
    }

    @Nullable
    public AbilityHolder getAbilityHolder(String ability) {
        return this.active.get(ability);
    }

    @Nullable
    public IAbilityInterface getAbility(String ability) {
        final AbilityHolder holder = this.getAbilityHolder(ability);
        return holder != null ? holder.getAbility() : null;
    }

    // ---- tick ----

    public void onUpdatePre(LivingEntity entity) {
        this.active.values().removeIf(cache -> cache.getAbility().shouldRemove());
        for (Entry<String, AbilityHolder> entry : this.active.entrySet()) {
            final IAbilityInterface ability = entry.getValue().getAbility();
            if (ability instanceof ITickableAbility tickable) {
                try {
                    tickable.tickAbilityPre(entity);
                } catch (Exception e) {
                    Trinkets.LOGGER.error("Error with ability:{}", entry.getKey(), e);
                }
            }
        }
    }

    public void onUpdate(LivingEntity entity) {
        for (Entry<String, AbilityHolder> entry : this.active.entrySet()) {
            final IAbilityInterface ability = entry.getValue().getAbility();
            if (ability.shouldRemove()) {
                ability.onAbilityRemoved(entity);
                continue;
            }
            if (ability.isFirstUpdate()) {
                if (!entity.level().isClientSide) {
                    this.loadAbilityFromEntity(entity, ability);
                }
                if (ability.shouldRemove() || !ability.isAbilityEnabled()) {
                    ability.onAbilityRemoved(entity);
                    ability.setFirstUpdate(false);
                    continue;
                }
                ability.onAbilityAdded(this.getOwner());
            }
            this.processAbility(ability, this.getOwner());
            if (ability.hasChanged()) {
                this.saveInfoToEntity(this.getOwner(), ability);
                this.sendNBTToPlayer(this.getOwner(), ability);
                ability.setChanged(false);
            }
            ability.setFirstUpdate(false);
            if (this.shouldRemove(entry, this.getOwner())) {
                ability.scheduleRemoval();
            }
            if (ability.shouldRemove()) {
                ability.onAbilityRemoved(entity);
            }
        }
    }

    public void onUpdatePost(LivingEntity entity) {
        this.hasChanged = false;
    }

    public boolean hasChanged() {
        return this.hasChanged;
    }

    /** 按能力实现的接口分派它这一 tick 该做的事 */
    private void processAbility(IAbilityInterface ability, LivingEntity entity) {
        try {
            final AbilityHolder holder = this.getAbilityHolder(ability.getRegistryName().toString());
            final SlotInformation info = holder != null ? holder.getInfo() : null;
            if (ability instanceof ITickableAbility tickable) {
                tickable.tickAbility(entity);
            }
            if (ability instanceof IEquippedAbility equipped) {
                equipped.head(entity.getItemBySlot(EquipmentSlot.HEAD), entity);
                equipped.chest(entity.getItemBySlot(EquipmentSlot.CHEST), entity);
                equipped.legs(entity.getItemBySlot(EquipmentSlot.LEGS), entity);
                equipped.feet(entity.getItemBySlot(EquipmentSlot.FEET), entity);
            }
            if (ability instanceof IHeldAbility held) {
                held.heldMainHand(entity.getMainHandItem(), entity);
                held.heldOffhand(entity.getOffhandItem(), entity);
            }
            if (entity instanceof Player player) {
                if (ability instanceof IContainerAbility container) {
                    container.inventoryContainer(player.inventoryMenu);
                    container.openContainer(player.containerMenu);
                    container.playerInventory(player.getInventory());
                }
                if (ability instanceof ITickableInventoryAbility inventoryAbility && info != null) {
                    this.tickInventoryAbility(inventoryAbility, info, player);
                }
            }
        } catch (Exception e) {
            Trinkets.LOGGER.error("Error with ability:{}", ability.getRegistryName(), e);
        }
    }

    private void tickInventoryAbility(ITickableInventoryAbility ability, SlotInformation info, Player player) {
        final int inHand = player.getInventory().selected;
        final ItemStack stack = info.getStackFromHandler(player);
        if (!stack.isEmpty()) {
            final int slot = info.getSlot();
            final boolean selected = info.getHandlerType() == ItemHandlerType.MAINHAND && (inHand == slot);
            ability.onUpdate(stack, player.level(), player, slot, selected);
            return;
        }
        final List<ItemStack> inventory = player.getInventory().items;
        for (int index = 0; index < inventory.size(); index++) {
            final ItemStack itemStack = inventory.get(index);
            if (!itemStack.isEmpty()) {
                ability.onUpdate(itemStack, player.level(), player, index, inHand == index);
            }
        }
    }

    /** 判断这条能力的来源是否已失效 */
    private boolean shouldRemove(@Nonnull Entry<String, AbilityHolder> entry, LivingEntity entity) {
        final String key = entry.getKey();
        final AbilityHolder cache = entry.getValue();
        final String source = cache.getSourceID();
        if (this.hasKillOrder(source, key)) {
            return true;
        }
        final SlotInformation sourceInfo = cache.getInfo();
        switch (sourceInfo.getHandlerType()) {
            case NONE:
                return true;
            case OTHER:
                return false;
            case RACE:
                // 种族变了、已无种族、或能力要求的元素与当前主元素不符，均视为来源失效
                final RaceCache raceCache = this.parentProperties.getCurrentRaceCache();
                final Element required = cache.getAbility().getRequiredElement();
                final boolean elementMismatch = required != null && !raceCache.comparePrimaryElement(required);
                final boolean sameRace = String.valueOf(raceCache.getRace().getRegistryName()).equals(source);
                return !sameRace || raceCache.getRace().isNone() || elementMismatch;
            case POTION:
                final MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(source));
                return effect == null || !entity.hasEffect(effect);
            default:
                final ItemStack stack = sourceInfo.getStackFromHandler(entity);
                if (stack.isEmpty()) {
                    return true;
                }
                // 槽位按「容器 + 序号」精确定位（Curios 另含槽位类型），物品被挪走即视为来源失效
                final ResourceLocation current = ForgeRegistries.ITEMS.getKey(stack.getItem());
                return current == null || !sourceInfo.getItemID().equals(current.toString());
        }
    }

    /**
     * 能力实例不跨实体复制——它们会由各自的来源（种族 / 饰品 / 药水）在新实体上重建。
     * 此方法保留以对齐 1.12 的调用点。
     */
    public void copyFrom(AbilityHandler source, boolean wasDeath, boolean keepInv) {
    }

    // ---- 禁用令 ----

    public boolean hasKillOrder(String source, String ability) {
        if (isBlank(source) || isBlank(ability)) {
            return false;
        }
        final CompoundTag cap = this.parentProperties.getTag().getCompound(CAP_KEY);
        if (!NBTHelper.hasTagCompound(cap, ability)) {
            return false;
        }
        final CompoundTag abilityTag = cap.getCompound(ability);
        if (!NBTHelper.hasTagCompound(abilityTag, DISABLED_SOURCES)) {
            return false;
        }
        return abilityTag.getCompound(DISABLED_SOURCES).contains(source);
    }

    public void addKillOrder(String source, String ability) {
        if (isBlank(source) || isBlank(ability)) {
            return;
        }
        final CompoundTag root = this.parentProperties.getTag();
        if (!root.contains(CAP_KEY)) {
            root.put(CAP_KEY, new CompoundTag());
        }
        final CompoundTag cap = root.getCompound(CAP_KEY);
        if (!cap.contains(ability)) {
            cap.put(ability, new CompoundTag());
        }
        final CompoundTag abilityTag = cap.getCompound(ability);
        if (!abilityTag.contains(DISABLED_SOURCES)) {
            abilityTag.put(DISABLED_SOURCES, new CompoundTag());
        }
        abilityTag.getCompound(DISABLED_SOURCES).putBoolean(source, true);
    }

    public void removeKillOrder(String source, String ability) {
        if (isBlank(source) || isBlank(ability)) {
            return;
        }
        final CompoundTag cap = this.parentProperties.getTag().getCompound(CAP_KEY);
        if (!cap.contains(ability)) {
            return;
        }
        final CompoundTag abilityTag = cap.getCompound(ability);
        if (!abilityTag.contains(DISABLED_SOURCES)) {
            return;
        }
        final CompoundTag disabled = abilityTag.getCompound(DISABLED_SOURCES);
        if (!disabled.contains(source)) {
            return;
        }
        disabled.remove(source);
        if (disabled.isEmpty()) {
            abilityTag.remove(DISABLED_SOURCES);
        }
        if (abilityTag.isEmpty()) {
            cap.remove(ability);
        }
        this.broadcastKillOrder(this.getOwner(), source, ability, false);
    }

    public void sendKillOrder(LivingEntity entity, String source, String ability) {
        if (entity == null || isBlank(source) || isBlank(ability)) {
            return;
        }
        this.broadcastKillOrder(entity, source, ability, true);
    }

    /**
     * 广播禁用令变更。
     * 移植说明：1.12 自行遍历世界玩家、按区块可见性筛选；1.20.1 交给 TRACKING_ENTITY_AND_SELF。
     */
    private void broadcastKillOrder(LivingEntity entity, String source, String ability, boolean disabled) {
        if (!(entity instanceof ServerPlayer)) {
            return;
        }
        final CompoundTag sync = new CompoundTag();
        sync.putString("Ability", ability);
        sync.putString("Source", source);
        sync.putBoolean(disabled ? "DISABLED" : "ENABLED", true);
        NetworkHandler.sendToTrackingAndSelf(new AbilityCacheSyncPacket(entity, sync), entity);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }

    // ---- 存取 ----

    /** 把能力的持久化数据与一次性同步数据下发给所有者 */
    private void sendNBTToPlayer(LivingEntity entity, IAbilityInterface ability) {
        if (!(entity instanceof ServerPlayer)) {
            return;
        }
        final String key = ability.getRegistryName().toString();
        final CompoundTag sync = new CompoundTag();
        final CompoundTag storage = ability.saveStorage(new CompoundTag());
        if (!storage.isEmpty()) {
            sync.put(key, storage);
        }
        final CompoundTag data = ability.sendAbilityData();
        if (data != null && !data.isEmpty()) {
            sync.put(key + ":data", data);
        }
        if (!sync.isEmpty()) {
            NetworkHandler.sendToTrackingAndSelf(new AbilityCacheSyncPacket(entity, sync), entity);
        }
    }

    public void saveInfoToEntity(LivingEntity entity, IAbilityInterface ability) {
        final CompoundTag root = this.parentProperties.getTag();
        if (!root.contains(CAP_KEY)) {
            root.put(CAP_KEY, new CompoundTag());
        }
        final CompoundTag storage = ability.saveStorage(new CompoundTag());
        if (!storage.isEmpty()) {
            root.getCompound(CAP_KEY).put(ability.getRegistryName().toString(), storage);
        }
    }

    public void loadAbilityFromEntity(LivingEntity entity, IAbilityInterface ability) {
        this.loadAbilityFromNBT(ability, this.parentProperties.getTag().getCompound(CAP_KEY));
    }

    public void loadAbilityFromNBT(@Nonnull IAbilityInterface ability, @Nonnull CompoundTag compound) {
        final String key = ability.getRegistryName().toString();
        if (NBTHelper.hasTagCompound(compound, key)) {
            ability.loadStorage(compound.getCompound(key));
        }
    }

    public CompoundTag saveAbilitiesToNBT(@Nonnull CompoundTag compound) {
        if (!compound.contains(CAP_KEY)) {
            compound.put(CAP_KEY, new CompoundTag());
        }
        final CompoundTag tag = compound.getCompound(CAP_KEY);
        for (Entry<String, AbilityHolder> entry : this.active.entrySet()) {
            try {
                final CompoundTag abilityTag = entry.getValue().getAbility().saveStorage(new CompoundTag());
                if (!abilityTag.isEmpty()) {
                    tag.put(entry.getKey(), abilityTag);
                }
            } catch (Exception e) {
                Trinkets.LOGGER.error("Error when saving ability:{}", entry.getKey(), e);
            }
        }
        return compound;
    }

    public void loadAbilitiesFromNBT(@Nonnull CompoundTag compound) {
        if (!NBTHelper.hasTagCompound(compound, CAP_KEY)) {
            return;
        }
        final CompoundTag tag = compound.getCompound(CAP_KEY);
        for (Entry<String, AbilityHolder> entry : this.active.entrySet()) {
            if (NBTHelper.hasTagCompound(tag, entry.getKey())) {
                try {
                    this.loadAbilityFromNBT(entry.getValue().getAbility(), tag);
                } catch (Exception e) {
                    Trinkets.LOGGER.error("Error when loading ability:{}", entry.getKey(), e);
                }
            }
        }
    }

    public CompoundTag saveToNBT(@Nonnull CompoundTag compound) {
        if (!compound.contains(CAP_KEY)) {
            compound.put(CAP_KEY, new CompoundTag());
        }
        return compound;
    }

    public void loadFromNBT(@Nonnull CompoundTag compound) {
    }

    // ---- 归属记录 ----

    /** 一条生效中的能力及其来源信息 */
    public static class AbilityHolder {

        protected final AbilityHandler handler;
        protected final String source;
        protected final SlotInformation info;
        protected final IAbilityInterface ability;

        public AbilityHolder(AbilityHandler handler, String source, SlotInformation info, @Nonnull IAbilityInterface ability) {
            this.handler = handler;
            this.source = source;
            this.info = info;
            this.ability = ability.cacheAbilityHolder(this);
        }

        public final AbilityHandler getHandler() {
            return this.handler;
        }

        public final String getSourceID() {
            return this.source;
        }

        public final SlotInformation getInfo() {
            return this.info;
        }

        public final IAbilityInterface getAbility() {
            return this.ability;
        }

        public final boolean sameAbilityOrigin(@Nonnull AbilityHolder other) {
            return this.sameAbilityOrigin(other.getSourceID(), other.getInfo(), other.getAbility());
        }

        /**
         * 面对同一能力键的新来源，判断现任是否保住归属。
         * 种族来源优先级最高：种族能力不会被非种族来源顶掉，反之则会。
         */
        public final boolean sameAbilityOrigin(String otherSource, @Nonnull SlotInformation otherInfo,
                @Nonnull IAbilityInterface otherAbility) {
            final ItemHandlerType handlerType = this.getInfo().getHandlerType();
            final ItemHandlerType otherHandlerType = otherInfo.getHandlerType();
            final boolean isRaceAbility = handlerType == ItemHandlerType.RACE;
            final boolean isOtherRaceAbility = otherHandlerType == ItemHandlerType.RACE;
            final boolean sameSource = this.getSourceID().contentEquals(otherSource);
            final boolean sameElementRequired = this.getAbility().getRequiredElement() == otherAbility.getRequiredElement();

            if (isRaceAbility && !isOtherRaceAbility) {
                return true;
            }
            if (!isRaceAbility && isOtherRaceAbility) {
                return false;
            }
            if (handlerType != otherHandlerType) {
                return true;
            }
            if (handlerType == ItemHandlerType.POTION) {
                return sameSource;
            }
            return sameSource && sameElementRequired;
        }
    }
}
