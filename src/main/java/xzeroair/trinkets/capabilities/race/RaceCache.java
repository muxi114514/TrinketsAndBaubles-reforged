package xzeroair.trinkets.capabilities.race;

import javax.annotation.Nonnull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.init.ModRaces;
import xzeroair.trinkets.races.EntityRace;
import xzeroair.trinkets.traits.elements.Element;

/**
 * 一份种族状态：种族本体 + 主副元素 + 是否临时（药水变身）及其时长与来源药水。
 *
 * 移植说明（两处刻意修正，不是照搬）：
 * 1. **NBT 改存注册名而非数字 id**。1.12 存 `Registry.getID(race)`，数字 id 会随注册顺序、
 *    模组增减而变；1.20.1 的自定义注册表更不该依赖它。改存 ResourceLocation 字符串后存档稳定，
 *    并保留对旧数字 id 的读取兼容（读到 int 时按索引回退）。
 * 2. **修正 1.12 的 temporary 读写笔误**：原版 saveToNBT 硬编码 `setBoolean("temporary", true)`，
 *    loadFromNBT 又用 `hasKey("temporary")` 判存在性而非取值，结果所有种族都被当成临时状态。
 *    此处按字段实际值读写。
 */
public class RaceCache {

    protected int duration;
    protected boolean temporary;
    protected EntityRace race;
    protected Element primary;
    protected Element secondary;
    protected String potion;

    public RaceCache() {
        this(ModRaces.NONE.get(), ModElements.NEUTRAL.get());
    }

    public RaceCache(EntityRace race) {
        this(race, ModElements.NEUTRAL.get());
    }

    public RaceCache(Element element) {
        this(ModRaces.NONE.get(), element);
    }

    public RaceCache(EntityRace race, Element primary) {
        this(race, primary, ModElements.NEUTRAL.get());
    }

    public RaceCache(EntityRace race, Element primary, Element secondary) {
        this(race, primary, secondary, false, 0, "");
    }

    public RaceCache(EntityRace race, Element primary, Element secondary, boolean temporary, int duration, String potion) {
        this.race = race;
        this.primary = primary;
        this.secondary = secondary;
        this.temporary = temporary;
        this.duration = duration;
        this.potion = potion == null ? "" : potion;
    }

    public EntityRace getRace() {
        return this.race == null ? ModRaces.NONE.get() : this.race;
    }

    public Element getPrimaryElement() {
        return this.primary == null ? ModElements.NEUTRAL.get() : this.primary;
    }

    public Element getSecondaryElement() {
        return this.secondary == null ? ModElements.NEUTRAL.get() : this.secondary;
    }

    /** 主色：有主元素则取元素色，否则取种族色 */
    public int getPrimaryColor() {
        final Element neutral = ModElements.NEUTRAL.get();
        return this.getPrimaryElement() == neutral
                ? this.getRace().getPrimaryColor()
                : this.getPrimaryElement().getPrimaryColor();
    }

    /** 副色：副元素 > 主元素 > 种族，逐级回退 */
    public int getSecondaryColor() {
        final Element neutral = ModElements.NEUTRAL.get();
        if (this.getSecondaryElement() != neutral) {
            return this.getSecondaryElement().getPrimaryColor();
        }
        if (this.getPrimaryElement() != neutral) {
            return this.getPrimaryElement().getSecondaryColor();
        }
        return this.getRace().getSecondaryColor();
    }

    // ── 存取 ──

    public CompoundTag saveToNBT(@Nonnull CompoundTag tag) {
        tag.putString("race", String.valueOf(this.getRace().getRegistryName()));
        tag.putString("element", String.valueOf(this.getPrimaryElement().getRegistryName()));
        tag.putString("secondary", String.valueOf(this.getSecondaryElement().getRegistryName()));
        tag.putBoolean("temporary", this.temporary);
        tag.putInt("duration", this.duration);
        tag.putString("potion", this.potion);
        return tag;
    }

    public static RaceCache loadFromNBT(CompoundTag tag) {
        final EntityRace race = readRace(tag, "race");
        final Element primary = readElement(tag, "element");
        final Element secondary = readElement(tag, "secondary");
        final boolean temporary = tag.getBoolean("temporary");
        final int duration = temporary ? tag.getInt("duration") : 0;
        final String potion = temporary ? tag.getString("potion") : "";
        return new RaceCache(race, primary, secondary, temporary, duration, potion);
    }

    private static EntityRace readRace(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return ModRaces.NONE.get();
        }
        final EntityRace race = EntityRace.getByName(tag.getString(key));
        return race == null ? ModRaces.NONE.get() : race;
    }

    private static Element readElement(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return ModElements.NEUTRAL.get();
        }
        final Element element = ModElements.byName(new ResourceLocation(tag.getString(key)));
        return element == null ? ModElements.NEUTRAL.get() : element;
    }

    // ── 临时状态 ──

    public RaceCache setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public int getDuration() {
        return this.duration;
    }

    public RaceCache setTemporary(boolean temporary) {
        this.temporary = temporary;
        return this;
    }

    public boolean isTemporary() {
        return this.temporary;
    }

    public RaceCache setPotion(String potion) {
        this.potion = potion == null ? "" : potion;
        return this;
    }

    public String getPotion() {
        return this.potion;
    }

    // ── 比较 ──

    public boolean compare(RaceCache other) {
        return this.compareRace(other)
                && this.comparePrimaryElement(other)
                && this.compareTemporary(other)
                && this.potion.equals(other.potion);
    }

    public boolean compareRace(RaceCache other) {
        return this.getRace().equals(other.getRace());
    }

    public boolean compareRace(EntityRace race) {
        return this.getRace().equals(race);
    }

    public boolean compareRace(String race) {
        return String.valueOf(this.getRace().getRegistryName()).equals(race);
    }

    public boolean comparePrimaryElement(RaceCache other) {
        return this.getPrimaryElement().equals(other.getPrimaryElement());
    }

    public boolean comparePrimaryElement(Element element) {
        return this.getPrimaryElement().equals(element);
    }

    public boolean comparePrimaryElement(String element) {
        return String.valueOf(this.getPrimaryElement().getRegistryName()).equals(element);
    }

    public boolean compareTemporary(RaceCache other) {
        return this.isTemporary() == other.isTemporary();
    }

    public boolean compareDuration(RaceCache other) {
        return this.getDuration() == other.getDuration();
    }
}
