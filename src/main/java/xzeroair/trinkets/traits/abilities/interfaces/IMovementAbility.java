package xzeroair.trinkets.traits.abilities.interfaces;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** 移动输入驱动的能力（闪避等复合手势）。 */
public interface IMovementAbility extends IKeyBindInterface {

    @Override
    default String getKey() {
        return "";
    }

    /** 仅客户端调用：随移动快照一并上行的附加数据 */
    @OnlyIn(Dist.CLIENT)
    default CompoundTag createMovementPayload(Entity entity, int primaryState, boolean primaryDown, boolean auxiliaryDown,
            int left, int right, int forward, int back, int jump, int sneak) {
        return null;
    }

    /**
     * 接收一份完整的移动输入快照。每个变化的回调都会执行以便能力观察组合输入；
     * 仅当有回调否决时整份快照才被丢弃。只有像闪避那样需要自行接管分发的复合手势才 override 本方法。
     */
    default boolean onMovement(Entity entity, int primaryState, boolean primaryDown, boolean auxiliaryDown,
            int left, int right, int forward, int back, int jump, int sneak, @Nullable CompoundTag payload) {
        boolean accepted = true;
        if ((left >= 0) && !this.left(entity, left, primaryState, primaryDown, auxiliaryDown, payload)) {
            accepted = false;
        }
        if ((right >= 0) && !this.right(entity, right, primaryState, primaryDown, auxiliaryDown, payload)) {
            accepted = false;
        }
        if ((forward >= 0) && !this.forward(entity, forward, primaryState, primaryDown, auxiliaryDown, payload)) {
            accepted = false;
        }
        if ((back >= 0) && !this.back(entity, back, primaryState, primaryDown, auxiliaryDown, payload)) {
            accepted = false;
        }
        if ((jump >= 0) && !this.jump(entity, jump, primaryState, primaryDown, auxiliaryDown, payload)) {
            accepted = false;
        }
        if ((sneak >= 0) && !this.sneak(entity, sneak, primaryState, primaryDown, auxiliaryDown, payload)) {
            accepted = false;
        }
        return accepted;
    }

    default boolean left(Entity entity, int state, int primaryState, boolean primaryDown, boolean auxiliaryDown, @Nullable CompoundTag payload) {
        return true;
    }

    default boolean right(Entity entity, int state, int primaryState, boolean primaryDown, boolean auxiliaryDown, @Nullable CompoundTag payload) {
        return true;
    }

    default boolean forward(Entity entity, int state, int primaryState, boolean primaryDown, boolean auxiliaryDown, @Nullable CompoundTag payload) {
        return true;
    }

    default boolean back(Entity entity, int state, int primaryState, boolean primaryDown, boolean auxiliaryDown, @Nullable CompoundTag payload) {
        return true;
    }

    default boolean jump(Entity entity, int state, int primaryState, boolean primaryDown, boolean auxiliaryDown, @Nullable CompoundTag payload) {
        return true;
    }

    default boolean sneak(Entity entity, int state, int primaryState, boolean primaryDown, boolean auxiliaryDown, @Nullable CompoundTag payload) {
        return true;
    }
}
