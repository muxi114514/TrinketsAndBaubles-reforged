package xzeroair.trinkets.util.helpers;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

/** 伤害源判定工具。 */
public final class DamageHelper {

    /**
     * 是否为间接伤害：直接来源与施害者不同（投射物、溅射等）、爆炸、投射物或魔法。
     * 对应 1.12 的 `instanceof EntityDamageSourceIndirect || isExplosion() || isProjectile() || isMagicDamage()`。
     */
    public static boolean isIndirect(DamageSource source) {
        return source.getDirectEntity() != source.getEntity()
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.IS_PROJECTILE)
                || source.is(DamageTypes.MAGIC);
    }

    private DamageHelper() {
    }
}
