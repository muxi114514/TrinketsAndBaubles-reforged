package xzeroair.trinkets.api.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.races.EntityRace;
import xzeroair.trinkets.traits.elements.Element;

/**
 * 种族变化相关事件（Forge 事件总线），供附属模组介入。对应 1.12 api/events/TransformationEvent。
 * 顺序：RaceUpdateEvent（可取消、可改写新种族）→ EndTransformation → StartTransformation。
 */
public abstract class TransformationEvent extends Event {

    private final LivingEntity entity;
    private final EntityProperties properties;
    private final RaceCache currentRace;

    protected TransformationEvent(LivingEntity entity, EntityProperties properties, RaceCache currentRace) {
        this.entity = entity;
        this.properties = properties;
        this.currentRace = currentRace;
    }

    public LivingEntity getEntity() {
        return this.entity;
    }

    public EntityProperties getEntityProperties() {
        return this.properties;
    }

    public RaceCache getCurrentRaceCache() {
        return this.currentRace;
    }

    /** 每 tick 服务端解析出应有种族后触发；取消则本 tick 不变身 */
    @Cancelable
    public static class RaceUpdateEvent extends TransformationEvent {

        private RaceCache newRaceCache;

        public RaceUpdateEvent(LivingEntity entity, EntityProperties properties, RaceCache resolved) {
            super(entity, properties, properties.getCurrentRaceCache());
            this.newRaceCache = resolved;
        }

        public RaceCache getNewRaceCache() {
            return this.newRaceCache;
        }

        public void setNewRaceCache(RaceCache cache) {
            this.newRaceCache = cache;
        }

        public EntityRace getNewRace() {
            return this.newRaceCache.getRace();
        }

        public Element getNewElement() {
            return this.newRaceCache.getPrimaryElement();
        }

        public boolean raceChanged() {
            return !this.getCurrentRaceCache().compare(this.newRaceCache);
        }
    }

    /** 旧种族即将结束（1.12 未标可取消，保持一致） */
    public static class EndTransformation extends TransformationEvent {

        public EndTransformation(LivingEntity entity, EntityProperties properties, RaceCache current) {
            super(entity, properties, current);
        }

        public RaceCache getPreviousRace() {
            return this.getCurrentRaceCache();
        }
    }

    /** 新种族已生效 */
    public static class StartTransformation extends TransformationEvent {

        public StartTransformation(LivingEntity entity, EntityProperties properties, RaceCache current) {
            super(entity, properties, current);
        }
    }
}
