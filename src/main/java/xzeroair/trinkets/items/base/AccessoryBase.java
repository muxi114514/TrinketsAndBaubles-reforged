package xzeroair.trinkets.items.base;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import xzeroair.trinkets.api.IAccessoryInterface;
import xzeroair.trinkets.api.ItemHandlerType;
import xzeroair.trinkets.api.SlotInformation;
import xzeroair.trinkets.attributes.AttributeEntry;
import xzeroair.trinkets.attributes.ConfigAttributes;
import xzeroair.trinkets.capabilities.trinket.TrinketProperties;
import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.traits.elements.IElementProvider;
import xzeroair.trinkets.traits.elements.ItemElements;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.SurvivalConfig;
import xzeroair.trinkets.util.config.server.items.AccessoryConfig;
import xzeroair.trinkets.util.helpers.PotionHelper;
import xzeroair.trinkets.util.helpers.damage.DamageTypeRules;
import xzeroair.trinkets.util.helpers.damage.DamageTypeRules.Stage;

/**
 * 饰品基类：佩戴在 Curios 饰品栏时施加属性、效果增删与能力，并按配置忽略伤害类型、阻断效果。
 *
 * 移植说明：
 * - 1.12 分别由 Baubles 的 IBauble 与自带饰品栏回调驱动；1.20.1 统一实现 Curios 的 ICurioItem。
 * - 属性每 tick 按配置更新（配置热改立即生效），卸下时按本饰品 UUID 撤除；
 *   Curios 在栈 NBT 变化（如切换能力开关）时也会回调卸下/佩戴，此时物品未变，不撤属性，避免最大生命被瞬间钳低。
 * - 1.12 在登录/登出时撤除全部饰品属性；此处按条目的存盘标记处理，重进游戏不再先掉血再补回。
 */
public abstract class AccessoryBase extends ItemBase implements ICurioItem, IAccessoryInterface, IElementProvider {

    private final UUID attributeUUID;

    protected AccessoryBase(String attributeUUID) {
        this(new Properties(), attributeUUID);
    }

    protected AccessoryBase(Properties properties, String attributeUUID) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
        this.attributeUUID = UUID.fromString(attributeUUID);
    }

    public UUID getAttributeUUID() {
        return this.attributeUUID;
    }

    /** 本饰品的配置；无独立配置时返回 null */
    @Nullable
    public abstract AccessoryConfig<?> getAccessoryConfig();

    /** 生存类联动段；有元素形态的饰品（龙之眼）按元素覆盖 */
    @Nullable
    public SurvivalConfig getSurvivalConfig(ItemStack stack) {
        final AccessoryConfig<?> config = this.getAccessoryConfig();
        return config == null ? null : config.survival;
    }

    /** 填充本饰品提供的能力（每个栈实例只调用一次，元素变化时重调） */
    public void initAbilities(ItemStack stack, List<IAbilityInterface> abilities) {
    }

    public List<? extends String> getEffectsToAdd(ItemStack stack) {
        final AccessoryConfig<?> config = this.getAccessoryConfig();
        return config == null ? List.of() : config.effectsToAdd.get();
    }

    public List<? extends String> getEffectsToRemove(ItemStack stack) {
        final AccessoryConfig<?> config = this.getAccessoryConfig();
        return config == null ? List.of() : config.effectsToRemove.get();
    }

    public List<? extends String> getDamageTypesToIgnore(ItemStack stack) {
        final AccessoryConfig<?> config = this.getAccessoryConfig();
        return config == null ? List.of() : config.damageTypesToIgnore.get();
    }

    public List<? extends String> getAttributeConfig(ItemStack stack) {
        final AccessoryConfig<?> config = this.getAccessoryConfig();
        return config == null ? List.of() : config.attributes.get();
    }

    @Override
    public boolean isAccessoryEnabled() {
        final AccessoryConfig<?> config = this.getAccessoryConfig();
        return config == null || config.enabled.get();
    }

    @Override
    public Element getPrimaryElement(ItemStack stack) {
        return ItemElements.getPrimary(stack, this.getPrimaryElement());
    }

    // ── Curios ──

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!this.isAccessoryEnabled()) {
            return;
        }
        final LivingEntity entity = slotContext.entity();
        if (!entity.level().isClientSide) {
            this.applyWornEffects(entity, stack);
        }
        final TrinketProperties properties = TrinketProperties.get(stack);
        if (properties != null) {
            properties.registerWorn(entity, SlotInformation.curio(stack, slotContext.identifier(), slotContext.index()));
        }
    }

    /** 佩戴期间：属性 → 清理免疫效果（两层免疫的清理层）→ 补充常驻效果 */
    protected void applyWornEffects(LivingEntity entity, ItemStack stack) {
        ConfigAttributes.apply(entity, this.getDescriptionId(), this.attributeUUID, this.getAttributeConfig(stack), 1.0D, false,
                entry -> isAttributeActive(entity, entry));
        PotionHelper.removeAllFromConfig(entity, this.getEffectsToRemove(stack));
        PotionHelper.addAllFromConfig(entity, true, this.getEffectsToAdd(stack));
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (!ItemStack.isSameItem(prevStack, stack)) {
            playEquipSound(slotContext.entity(), 1.9F);
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (ItemStack.isSameItem(newStack, stack)) {
            return;
        }
        ConfigAttributes.removeAll(slotContext.entity(), this.attributeUUID);
        playEquipSound(slotContext.entity(), 2.0F);
    }

    /** 同名饰品只能戴一件（对应 1.12 TrinketHelper.AccessoryCheck），自身所在槽位除外 */
    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        if (!this.isAccessoryEnabled()) {
            return false;
        }
        return CuriosApi.getCuriosInventory(slotContext.entity()).resolve()
                .map(inventory -> inventory.findCurios(worn -> worn.is(this)).stream()
                        .allMatch(result -> result.slotContext().identifier().equals(slotContext.identifier())
                                && result.slotContext().index() == slotContext.index()))
                .orElse(true);
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player && player.isCreative()) {
            return true;
        }
        return !EnchantmentHelper.hasBindingCurse(stack);
    }

    // ── 事件（由 AccessoryEventHandler 分发）──

    @Override
    public void eventLivingAttacked(ItemStack stack, LivingEntity attacked, LivingAttackEvent event) {
        if (DamageTypeRules.shouldCancel(event.getSource(), event.getAmount(), this.getPrimaryElement(stack),
                this.getDamageTypesToIgnore(stack))) {
            event.setCanceled(true);
        }
    }

    @Override
    public void eventLivingHurtAttacked(ItemStack stack, LivingEntity attacked, LivingHurtEvent event) {
        event.setAmount(DamageTypeRules.scale(Stage.HURT, event.getSource(), event.getAmount(), this.getPrimaryElement(stack),
                this.getDamageTypesToIgnore(stack)));
    }

    @Override
    public void eventLivingDamageAttacked(ItemStack stack, LivingEntity attacked, LivingDamageEvent event) {
        event.setAmount(DamageTypeRules.scale(Stage.DAMAGED, event.getSource(), event.getAmount(), this.getPrimaryElement(stack),
                this.getDamageTypesToIgnore(stack)));
    }

    /** 免疫的阻断层：名单内效果直接拒绝施加 */
    @Override
    public void eventPotionApplicable(ItemStack stack, LivingEntity entity, MobEffectEvent.Applicable event) {
        if (PotionHelper.isEffect(event.getEffectInstance(), this.getEffectsToRemove(stack))) {
            event.setResult(Event.Result.DENY);
        }
    }

    // ── 原版物品行为 ──

    /** 右键切换主能力，潜行右键切换副能力（对应 1.12 TrinketProperties#itemRightClicked） */
    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, Player player, @Nonnull InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                TrinketData.setAltAbility(stack, !TrinketData.isAltAbility(stack));
            } else {
                TrinketData.setMainAbility(stack, !TrinketData.isMainAbility(stack));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** 放在物品栏或拿在手上时登记对应能力；Curios 饰品栏调用本方法时 slot 为 -1，由 curioTick 处理 */
    @Override
    public void inventoryTick(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull Entity entity, int slot, boolean selected) {
        if (slot < 0 || !(entity instanceof Player player) || !this.isAccessoryEnabled()) {
            return;
        }
        final ItemHandlerType type = carriedType(player, stack, slot);
        final TrinketProperties properties = TrinketProperties.get(stack);
        if (type != null && properties != null) {
            properties.registerCarried(player, new SlotInformation(stack, type, slot));
        }
    }

    @Override
    public void onCraftedBy(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull Player player) {
        TrinketData.setCrafter(stack, player);
    }

    /** 有元素变种的饰品按元素区分名称（item.xat.dragons_eye.fire） */
    @Nonnull
    @Override
    public String getDescriptionId(@Nonnull ItemStack stack) {
        if (this.getSubElements().length == 0) {
            return super.getDescriptionId(stack);
        }
        final Element element = this.getPrimaryElement(stack);
        final ResourceLocation id = ModElements.registry().getKey(element);
        return element.isNone() || id == null ? super.getDescriptionId(stack) : this.getDescriptionId() + "." + id.getPath();
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new TrinketProperties.Provider(stack);
    }

    // ── 内部 ──

    /** Inventory#tick 对各分区都传「序号 == 选中格」，须按栈实例判断它实际所在的分区 */
    @Nullable
    private static ItemHandlerType carriedType(Player player, ItemStack stack, int slot) {
        final Inventory inventory = player.getInventory();
        if (slot < inventory.items.size() && inventory.items.get(slot) == stack) {
            if (slot == inventory.selected) {
                return ItemHandlerType.MAINHAND;
            }
            return Inventory.isHotbarSlot(slot) ? ItemHandlerType.HOTBAR : ItemHandlerType.INVENTORY;
        }
        if (slot == 0 && inventory.offhand.get(0) == stack) {
            return ItemHandlerType.OFFHAND;
        }
        return null;
    }

    /**
     * 游泳速度与深海探索者及名单中的其它游泳附魔默认不叠加，飞行时也不生效（与 1.12 一致）。
     */
    private static boolean isAttributeActive(LivingEntity entity, AttributeEntry entry) {
        if (!entry.attributes().contains(ForgeMod.SWIM_SPEED.get())) {
            return true;
        }
        if (entity instanceof Player player && player.getAbilities().flying) {
            return false;
        }
        if (!TrinketsConfig.SERVER.misc.depthStriderStacks.get() && EnchantmentHelper.getDepthStrider(entity) > 0) {
            return false;
        }
        for (String id : TrinketsConfig.SERVER.misc.nonStackingSwimEnchantments.get()) {
            final ResourceLocation key = ResourceLocation.tryParse(id);
            final var enchantment = key == null || !ForgeRegistries.ENCHANTMENTS.containsKey(key) ? null : ForgeRegistries.ENCHANTMENTS.getValue(key);
            if (enchantment != null && EnchantmentHelper.getEnchantmentLevel(enchantment, entity) > 0) {
                return false;
            }
        }
        return true;
    }

    private static void playEquipSound(LivingEntity entity, float pitch) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARMOR_EQUIP_DIAMOND,
                SoundSource.PLAYERS, 0.75F, pitch);
    }
}
