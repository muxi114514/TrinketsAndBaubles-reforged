package xzeroair.trinkets.races;

import net.minecraft.nbt.CompoundTag;

import xzeroair.trinkets.util.helpers.NBTHelper;

/**
 * 种族外观状态：性别、配色方案、主/副/辅助特征色、特征变种与是否显示特征（翅膀、耳朵等）。
 * 由种族选择界面修改、随实体种族数据存取，渲染层据此绘制外观。
 *
 * 移植说明：1.12 这些字段散落在 EntityRacePropertiesHandler 中；拆出以控制行为类的体量，NBT 键名保持不变。
 */
public class RaceAppearance {

    private int gender;
    private int colorOption;
    private int primaryColor;
    private int secondaryColor;
    private int auxColor;
    private int variant;
    private int auxVariant;
    private boolean showTraits = true;

    public RaceAppearance(int primaryColor, int secondaryColor, int auxColor) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.auxColor = auxColor;
    }

    public int getGender() {
        return this.gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public int getColorOption() {
        return this.colorOption;
    }

    public void setColorOption(int colorOption) {
        this.colorOption = colorOption;
    }

    public int getPrimaryColor() {
        return this.primaryColor;
    }

    public void setPrimaryColor(int color) {
        this.primaryColor = color;
    }

    public int getSecondaryColor() {
        return this.secondaryColor;
    }

    public void setSecondaryColor(int color) {
        this.secondaryColor = color;
    }

    public int getAuxColor() {
        return this.auxColor;
    }

    public void setAuxColor(int color) {
        this.auxColor = color;
    }

    public int getVariant() {
        return this.variant;
    }

    public void setVariant(int variant) {
        this.variant = variant;
    }

    public int getAuxVariant() {
        return this.auxVariant;
    }

    public void setAuxVariant(int variant) {
        this.auxVariant = variant;
    }

    public boolean showTraits() {
        return this.showTraits;
    }

    public void setShowTraits(boolean showTraits) {
        this.showTraits = showTraits;
    }

    public CompoundTag save(CompoundTag compound) {
        compound.putInt("gender", this.gender);
        compound.putInt("colorOption", this.colorOption);
        compound.putInt("traitPrimaryColor", this.primaryColor);
        compound.putInt("traitSecondaryColor", this.secondaryColor);
        compound.putInt("traitAuxColor", this.auxColor);
        compound.putInt("traitVariant", this.variant);
        compound.putInt("traitAuxVariant", this.auxVariant);
        compound.putBoolean("showTraits", this.showTraits);
        return compound;
    }

    public void load(CompoundTag compound) {
        NBTHelper.hasInteger(compound, "gender", value -> this.gender = value);
        NBTHelper.hasInteger(compound, "colorOption", value -> this.colorOption = value);
        NBTHelper.hasInteger(compound, "traitPrimaryColor", value -> this.primaryColor = value);
        NBTHelper.hasInteger(compound, "traitSecondaryColor", value -> this.secondaryColor = value);
        NBTHelper.hasInteger(compound, "traitAuxColor", value -> this.auxColor = value);
        NBTHelper.hasInteger(compound, "traitVariant", value -> this.variant = value);
        NBTHelper.hasInteger(compound, "traitAuxVariant", value -> this.auxVariant = value);
        NBTHelper.hasBoolean(compound, "showTraits", value -> this.showTraits = value);
    }

    /**
     * 载入客户端界面提交的外观；任一项越界则整份拒绝（对应 1.12 loadProfileData）。
     *
     * 移植说明：界面的辅助变种滑条取值 0~辅助变种数，最大值表示「不显示」（龙无角、法埃利斯无尾、牛头族无铃铛），
     * 1.12 校验写成「小于变种数」会把这一合法选择整份拒绝，此处改为「不超过变种数」。
     */
    public boolean loadProfile(CompoundTag compound, int primaryVariants, int auxVariants) {
        final int option = compound.contains("colorOption") ? compound.getInt("colorOption") : this.colorOption;
        final int newVariant = compound.contains("traitVariant") ? compound.getInt("traitVariant") : this.variant;
        final int newAuxVariant = compound.contains("traitAuxVariant") ? compound.getInt("traitAuxVariant") : this.auxVariant;
        final int newGender = compound.contains("gender") ? compound.getInt("gender") : this.gender;
        final boolean validVariant = newVariant >= 0 && (primaryVariants > 0 ? newVariant < primaryVariants : newVariant == 0);
        if (option < 0 || option > 2 || !validVariant || newAuxVariant < 0 || newAuxVariant > auxVariants || newGender != 0) {
            return false;
        }
        this.load(compound);
        return true;
    }

    public void copyFrom(RaceAppearance source) {
        this.gender = source.gender;
        this.colorOption = source.colorOption;
        this.primaryColor = source.primaryColor;
        this.secondaryColor = source.secondaryColor;
        this.auxColor = source.auxColor;
        this.variant = source.variant;
        this.auxVariant = source.auxVariant;
        this.showTraits = source.showTraits;
    }
}
