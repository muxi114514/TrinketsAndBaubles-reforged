package xzeroair.trinkets.entity;

import java.util.List;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import xzeroair.trinkets.entity.area.AbstractAreaEffect.LiquidBehavior;
import xzeroair.trinkets.entity.area.AreaEffectEntity;
import xzeroair.trinkets.entity.area.action.DamageAreaAction;
import xzeroair.trinkets.entity.area.action.FreezeLiquidAreaAction;
import xzeroair.trinkets.entity.area.action.PlaceFireAreaAction;
import xzeroair.trinkets.entity.area.action.PlaceSnowAreaAction;
import xzeroair.trinkets.entity.area.action.PotionAreaAction;
import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.helpers.LiquidFreezer;
import xzeroair.trinkets.util.helpers.PotionHelper;

/**
 * 龙息与地形/残留区域相关的规则：按元素配置残留区域的动作与液体行为，冰霜吐息入水冻结周围液体。
 * 从 BreathProjectile 拆出，保持投射物类只管飞行与命中。
 */
public final class BreathTerrain {

    public static final int FREEZE_RADIUS = 2;
    /** 中性吐息命中区域每次脉冲的伤害占吐息伤害的比例 */
    public static final float NEUTRAL_AREA_DAMAGE_RATIO = 0.1F;
    public static final int FIRE_IMPACT_INITIAL_MAX_BLOCKS = 6;
    public static final int FIRE_IMPACT_DELAYED_ATTEMPT_INTERVAL = 20;
    public static final float FIRE_IMPACT_DELAYED_ATTEMPT_CHANCE = 0.7F;

    /** 残留区域：冰浮于水、遇岩浆消失；火浮于岩浆、遇水消失；其余遇液体即消失。中性吐息附带小额伤害 */
    static void configureImpactArea(AreaEffectEntity area, Element element, float damage, List<? extends String> effects, boolean terrain) {
        if (element == ModElements.ICE.get()) {
            area.setWaterBehavior(LiquidBehavior.FLOAT).setLavaBehavior(LiquidBehavior.EXPIRE);
        } else if (element == ModElements.FIRE.get()) {
            area.setWaterBehavior(LiquidBehavior.EXPIRE).setLavaBehavior(LiquidBehavior.FLOAT);
        } else {
            area.setWaterBehavior(LiquidBehavior.EXPIRE).setLavaBehavior(LiquidBehavior.EXPIRE);
        }
        if (element == ModElements.NEUTRAL.get()) {
            area.addAction(new DamageAreaAction(damage * NEUTRAL_AREA_DAMAGE_RATIO));
        }
        for (String effect : effects) {
            final PotionHelper.ParsedEffect parsed = PotionHelper.parse(effect);
            if (parsed != null) {
                area.addAction(new PotionAreaAction(parsed.toInstance()));
            }
        }
        if (!terrain) {
            return;
        }
        if (element == ModElements.FIRE.get()) {
            area.addAction(new PlaceFireAreaAction(FIRE_IMPACT_INITIAL_MAX_BLOCKS, FIRE_IMPACT_DELAYED_ATTEMPT_INTERVAL, FIRE_IMPACT_DELAYED_ATTEMPT_CHANCE));
        } else if (element == ModElements.ICE.get()) {
            area.addAction(new FreezeLiquidAreaAction(4));
            area.addAction(new PlaceSnowAreaAction());
        }
    }

    /** 冰霜吐息入液：冻结周围水平半径 2 内的液体源（冷却岩浆 / 霜冰） */
    static void freezeAround(Level level, Vec3 center, boolean water) {
        LiquidFreezer.freeze(level, center, FREEZE_RADIUS, water);
    }

    private BreathTerrain() {
    }
}
