package xzeroair.trinkets.traits.abilities;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.config.abilities.ClimbingAbilityConfig;
import xzeroair.trinkets.util.helpers.BlockMatcher;
import xzeroair.trinkets.util.helpers.ChunkSafety;

/**
 * 攀爬：悬空贴墙时沿墙上爬（潜行则悬停），且不累计坠落距离。
 * 可爬方块由配置列表决定（白名单：只有列出的可爬；黑名单：除列出的都可爬），头顶被挡时不能爬。
 *
 * 移植说明：
 * - 1.12 的 isGrounded 用体型调整后的自定义包围盒判着地，补偿的是 1.12 体型实现下原版 onGround 不准的问题；
 *   1.20.1 经 EntityEvent.Size 改的是真实尺寸，原版 onGround 天然准确，直接使用。
 * - 1.12 靠 sendAbilityData / loadDataCache 把方块列表下发给客户端（客户端也要预测爬墙运动）；
 *   1.20.1 的 SERVER 配置自动同步，客户端构造能力时读到的已是服务端配置，那套下发不再需要。
 * - 前方方块可能跨区块边界，经 ChunkCache 只读内存，区块未加载则本 tick 不爬。
 */
public class AbilityClimbing extends Ability implements ITickableAbility {

    private static final double CLIMB_SPEED = 0.1D;

    private final boolean useWhitelist;
    private final BlockMatcher blocks;
    private final int listedBlocks;

    public AbilityClimbing(@Nonnull ClimbingAbilityConfig config) {
        super(AbilityNames.CLIMBING);
        this.setAbilityEnabled(config.isEnabled());
        this.useWhitelist = config.useWhitelist.get();
        this.blocks = config.isEnabled() ? BlockMatcher.of(config.blocks.get()) : BlockMatcher.EMPTY;
        this.listedBlocks = config.blocks.get().size();
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        if (this.isCreativePlayer(entity) || entity.onGround() || !entity.horizontalCollision) {
            return;
        }
        if (!this.canClimb(entity)) {
            return;
        }
        final Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, entity.isShiftKeyDown() ? 0.0D : CLIMB_SPEED, motion.z);
        entity.fallDistance = 0;
    }

    protected boolean canClimb(LivingEntity entity) {
        final Level level = entity.level();
        final BlockPos body = entity.blockPosition();
        final ChunkSafety.ChunkCache chunks = new ChunkSafety.ChunkCache(level);
        final BlockState bodyState = chunks.getBlockState(body);
        final BlockState frontState = chunks.getBlockState(body.relative(entity.getDirection()));
        if (bodyState == null || frontState == null) {
            return false;
        }
        // 只沿当前 x/z 列向上探，不跨区块边界
        final Vec3 feet = entity.position();
        final HitResult hit = level.clip(new ClipContext(feet, feet.add(0, entity.getBbHeight() + 0.1D, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return false;
        }
        final boolean listed = this.blocks.matches(bodyState) || this.blocks.matches(frontState);
        return listed == this.useWhitelist;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        variables.number("speed", CLIMB_SPEED * 20)
                .number("count", this.listedBlocks)
                .flag("whitelist", this.useWhitelist)
                .flag("blacklist", !this.useWhitelist);
    }
}
