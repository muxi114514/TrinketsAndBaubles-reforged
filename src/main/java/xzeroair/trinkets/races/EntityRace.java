package xzeroair.trinkets.races;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.init.ModRaces;
import xzeroair.trinkets.util.Reference;

/**
 * 种族定义（注册表条目）。
 *
 * 移植说明：
 * - 1.12 的 EntityRace 继承 IForgeRegistryEntry.Impl，该接口在 1.19+ 已被 Forge 移除；
 *   1.20.1 注册表条目为普通对象，注册名由注册表持有。
 * - 1.12 用 switch(name) 在本类里 new 出 9 个种族的行为处理器与信息包装，
 *   导致「种族定义」反向依赖全部「种族实现」。此处改为构造时注入工厂（依赖倒置），
 *   本类因此不再认识任何具体种族类。
 */
public class EntityRace {

    /** 由种族定义产出其行为处理器 */
    @FunctionalInterface
    public interface HandlerFactory {
        EntityRacePropertiesHandler create(@Nullable LivingEntity entity, @Nullable EntityProperties properties, RaceCache cache);
    }

    protected final String name;
    protected final UUID uuid;
    protected final int primaryColor;
    protected final int secondaryColor;

    protected int magicAffinityValue = 100;
    protected int raceHeight = 100;
    protected int raceWidth = 100;
    protected boolean canFly = false;

    @Nullable
    protected HandlerFactory handlerFactory;

    protected RaceInformation information = new RaceInformation();

    public EntityRace(String name, String uuid, int color1, int color2) {
        this.name = name;
        this.uuid = UUID.fromString(uuid);
        this.primaryColor = color1;
        this.secondaryColor = color2;
    }

    // ── 标识 ──

    public String getName() {
        return this.name;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public ResourceLocation getRegistryName() {
        return ModRaces.registry().getKey(this);
    }

    public String getTranslationKey() {
        return Reference.MODID + ".race." + this.name.toLowerCase();
    }

    public Component getDisplayName() {
        return Component.translatable(this.getTranslationKey() + ".name");
    }

    public boolean isNone() {
        return this.name.equalsIgnoreCase("None");
    }

    @Nullable
    public static EntityRace getByUUID(UUID uuid) {
        for (EntityRace race : ModRaces.registry().getValues()) {
            if (race.getUUID().equals(uuid)) {
                return race;
            }
        }
        return ModRaces.NONE.get();
    }

    /** 按注册名取种族；1.12 还支持传数字 id，1.20.1 数字 id 不稳定故不再支持 */
    @Nullable
    public static EntityRace getByName(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return ModRaces.registry().getValue(new ResourceLocation(id.toLowerCase()));
    }

    // ── 属性 ──

    public int getRaceHeight() {
        return this.raceHeight;
    }

    public EntityRace setRaceHeight(int raceHeight) {
        this.raceHeight = raceHeight;
        return this;
    }

    public int getRaceWidth() {
        return this.raceWidth;
    }

    public EntityRace setRaceWidth(int raceWidth) {
        this.raceWidth = raceWidth;
        return this;
    }

    public EntityRace setRaceSize(int size) {
        return this.setRaceHeight(size).setRaceWidth(size);
    }

    public int getMagicAffinity() {
        return this.magicAffinityValue;
    }

    public EntityRace setMagicAffinity(int magicAffinity) {
        this.magicAffinityValue = magicAffinity;
        return this;
    }

    public boolean canFly() {
        return this.canFly;
    }

    public EntityRace setCanFly(boolean canFly) {
        this.canFly = canFly;
        return this;
    }

    public int getPrimaryColor() {
        return this.primaryColor;
    }

    public int getSecondaryColor() {
        return this.secondaryColor;
    }

    // ── 行为处理器 ──

    public EntityRace setHandlerFactory(HandlerFactory factory) {
        this.handlerFactory = factory;
        return this;
    }

    /** 本族的展示信息（配色、特征变体数、体型），对应 1.12 的 getRaceInformation() */
    public RaceInformation getInformation() {
        return this.information;
    }

    public EntityRace setInformation(RaceInformation information) {
        this.information = information;
        return this;
    }

    public EntityRacePropertiesHandler getRaceHandler(@Nullable LivingEntity entity, @Nullable EntityProperties properties) {
        return this.getRaceHandler(entity, properties, new RaceCache(this));
    }

    public EntityRacePropertiesHandler getRaceHandler(@Nullable LivingEntity entity, @Nullable EntityProperties properties,
            @Nullable RaceCache raceCache) {
        final RaceCache cache = raceCache == null ? new RaceCache(this) : raceCache;
        if (this.handlerFactory == null) {
            return new EmptyHandler(entity, properties, cache);
        }
        return this.handlerFactory.create(entity, properties, cache);
    }
}
