package xzeroair.trinkets.traits.abilities;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import xzeroair.trinkets.traits.AbilityHandler.AbilityHolder;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.handlers.TickHandler;

/**
 * 能力基类。
 *
 * 移植说明：1.12 原类含大量已注释的旧注册表代码（能力曾打算走 Forge 注册表），此处不移。
 * 注册名仍由能力自己持有（能力不是注册表条目，是每实体一份的实例）。
 */
public class Ability implements IAbilityInterface {

    protected static final String UNKNOWN_SOURCE = "UNKNOWN_SOURCE";
    protected static final String REMOVE_TAG = "BEGONE_THOT";
    protected static final String COST_TAG = "COST";
    protected static final String DAMAGE_TAG = "DAMAGE";
    protected static final String FREQUENCY_TAG = "FREQUENCY";
    protected static final String COOLDOWN_TAG = "COOLDOWN";

    protected final RandomSource random = RandomSource.create();
    protected TickHandler tickHandler;
    protected AbilityHolder abilityHolder;
    protected Element requiredElement;
    protected String SOURCE;

    private ResourceLocation regName;
    private String uuid;
    private String translationKey;
    private boolean enabled;
    private boolean changed;
    private boolean firstUpdate;
    private boolean removeAbility;

    public Ability() {
        this(Reference.MODID, null);
        this.setTranslationKey(this.getClass().getSimpleName());
        this.setRegistryName(Reference.MODID, this.getClass().getSimpleName().toLowerCase(Locale.ROOT));
    }

    public Ability(String name) {
        this(Reference.MODID, name);
    }

    public Ability(String modID, @Nullable String name) {
        this.tickHandler = new TickHandler();
        this.enabled = true;
        this.firstUpdate = true;
        this.changed = false;
        this.SOURCE = UNKNOWN_SOURCE;
        this.removeAbility = false;
        if (name != null) {
            this.setTranslationKey(name);
            this.setRegistryName(modID, name);
        }
    }

    @Override
    public boolean hasChanged() {
        return this.changed;
    }

    @Override
    public IAbilityInterface setChanged(boolean change) {
        this.changed = change;
        return this;
    }

    @Override
    public boolean isAbilityEnabled() {
        return this.enabled;
    }

    @Override
    public IAbilityInterface setAbilityEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    public boolean isFirstUpdate() {
        return this.firstUpdate;
    }

    @Override
    public IAbilityInterface setFirstUpdate(boolean firstUpdate) {
        this.firstUpdate = firstUpdate;
        return this;
    }

    @Override
    public Ability setRequiredElement(Element requiredElement) {
        this.requiredElement = requiredElement;
        return this;
    }

    @Nullable
    @Override
    public Element getRequiredElement() {
        return this.requiredElement;
    }

    @Override
    public boolean shouldRemove() {
        return this.removeAbility;
    }

    @Override
    public Ability scheduleRemoval() {
        this.removeAbility = true;
        return this;
    }

    @Override
    public Ability cacheAbilityHolder(AbilityHolder holder) {
        this.abilityHolder = holder;
        return this;
    }

    @Override
    public AbilityHolder getAbilityHolder() {
        return this.abilityHolder;
    }

    @Nullable
    @Override
    public CompoundTag sendAbilityData() {
        return null;
    }

    @Override
    public void loadStorage(CompoundTag compound) {
    }

    public TickHandler getTickHandler() {
        return this.tickHandler;
    }

    public boolean sendMessageToPlayer(Entity entity) {
        return false;
    }

    // ── 标识 ──

    @Override
    public ResourceLocation getRegistryName() {
        return this.regName;
    }

    protected Ability setRegistryName(String modID, String name) {
        if (this.regName != null) {
            throw new IllegalStateException("Attempted to set registry name with existing registry name! New: "
                    + name + " Old: " + this.getRegistryName());
        }
        this.regName = new ResourceLocation(modID.toLowerCase(Locale.ROOT), name.toLowerCase(Locale.ROOT));
        return this;
    }

    @Override
    public String getUUID() {
        return this.uuid;
    }

    protected Ability setUUID(String uuid) {
        this.uuid = uuid;
        return this;
    }

    protected Ability setTranslationKey(String translationKey) {
        this.translationKey = translationKey;
        return this;
    }

    @Override
    public String getTranslationKey() {
        return Reference.MODID + ".ability." + this.translationKey;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(this.getTranslationKey() + ".name");
    }

    // ── 通用判定 ──

    public boolean isCreativePlayer(Entity entity) {
        return entity instanceof Player player && this.isCreativePlayer(player);
    }

    public boolean isCreativePlayer(@Nonnull Player player) {
        return player.isCreative() || player.isSpectator();
    }

    public boolean isCreativeFlying(Entity entity) {
        return entity instanceof Player player && this.isCreativeFlying(player);
    }

    public boolean isCreativeFlying(@Nonnull Player player) {
        return player.getAbilities().flying;
    }

    public boolean isSpectator(Entity entity) {
        return entity instanceof Player player && this.isSpectator(player);
    }

    public boolean isSpectator(@Nonnull Player player) {
        return player.isSpectator();
    }
}
