package xzeroair.trinkets.races;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import xzeroair.trinkets.util.config.server.RaceConfig;

/**
 * 种族的展示与体型信息（对应 1.12 的 RaceDefaultInformationWrapper 及其 9 个子类）。
 *
 * 移植说明：1.12 为每族写一个子类，内容只是 super(...) 的几个数字与两个变体数不同，
 * 体型与属性再转去读该族配置。此处收敛为单个类 + 每族一份实例（见 RaceInformations），
 * 体型与属性同样从配置实时读取，行为一致。
 */
public class RaceInformation {

    private final int width;
    private final int height;
    private final int primaryColor;
    private final int secondaryColor;
    private final int optionalColor;
    private final int primaryTraitVariants;
    private final int secondaryTraitVariants;

    @Nullable
    private final Supplier<RaceConfig<?>> config;

    public RaceInformation(int width, int height, int primaryColor, int secondaryColor, int optionalColor,
            int primaryTraitVariants, int secondaryTraitVariants, @Nullable Supplier<RaceConfig<?>> config) {
        this.width = width;
        this.height = height;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.optionalColor = optionalColor;
        this.primaryTraitVariants = primaryTraitVariants;
        this.secondaryTraitVariants = secondaryTraitVariants;
        this.config = config;
    }

    public RaceInformation() {
        this(100, 100, 11107684, 16374701, 16374701, 0, 0, null);
    }

    public int getWidth() {
        final RaceConfig<?> c = this.config();
        return c != null ? c.width.get() : this.width;
    }

    public int getHeight() {
        final RaceConfig<?> c = this.config();
        return c != null ? c.height.get() : this.height;
    }

    public int getSize() {
        return this.getHeight();
    }

    public int getPrimaryColor() {
        return this.primaryColor;
    }

    public int getSecondaryColor() {
        return this.secondaryColor;
    }

    public int getOptionalColor() {
        return this.optionalColor;
    }

    public int getPrimaryTraitMaxVariants() {
        return this.primaryTraitVariants;
    }

    public int getSecondaryTraitMaxVariants() {
        return this.secondaryTraitVariants;
    }

    public List<? extends String> getAttributes() {
        final RaceConfig<?> c = this.config();
        return c != null ? c.attributes.get() : List.of();
    }

    @Nullable
    private RaceConfig<?> config() {
        return this.config == null ? null : this.config.get();
    }
}
