package xzeroair.trinkets.blocks.entity;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.init.ModBlockEntities;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 月光玫瑰的魔力精华。对应 1.12 TileEntityMoonRose + TileEntityProperties 的精华部分。
 *
 * 行为（与 1.12 一致）：带精华标记的玫瑰在 5 格内没有其他带精华的玫瑰时，
 * 周围 4 格内潜行的生物每累计「吸收冷却」tick 吸收 1 点精华换 1 点额外魔力上限；精华耗尽玫瑰消失。
 * 0.33.4 中没有任何途径给玫瑰设置精华标记（setHasEssence 无调用方），只能经 NBT 放置获得，照原样保留。
 *
 * 移植说明：邻近玫瑰扫描只读已加载区块（getChunkNow），不触发区块加载。
 */
public class MoonRoseBlockEntity extends BlockEntity {

    private static final int RANGE = 5;
    private static final int RANGE_Y = 2;

    private boolean providesEssence;
    private int essence = -1;
    private int absorbTicks;

    public MoonRoseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOON_ROSE.get(), pos, state);
    }

    public boolean hasEssence() {
        return this.essence > 0;
    }

    public void serverTick() {
        final Level level = this.getLevel();
        if (level == null || !this.providesEssence) {
            return;
        }
        if (!this.hasEssence()) {
            level.removeBlock(this.getBlockPos(), false);
            return;
        }
        if (this.hasEssenceNearby(level)) {
            return;
        }
        final List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, new AABB(this.getBlockPos()).inflate(4.0D),
                LivingEntity::isShiftKeyDown);
        final int cooldown = TrinketsConfig.SERVER.blocks.moonRoseEssenceCooldown.get();
        for (LivingEntity entity : entities) {
            final MagicStats magic = MagicStats.get(entity);
            if (magic == null || ++this.absorbTicks < cooldown) {
                continue;
            }
            this.absorbTicks = 0;
            magic.setBonusMana(magic.getBonusMana() + 1);
            this.essence--;
            level.playSound(null, this.getBlockPos(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.BLOCKS, 0.4F,
                    level.random.nextFloat() * 0.6F + 0.4F);
            this.setChanged();
        }
    }

    /** 1.12 每个玫瑰只在附近没有别的「有精华」玫瑰时才供给，避免扎堆刷魔力 */
    private boolean hasEssenceNearby(Level level) {
        final BlockPos origin = this.getBlockPos();
        for (int cx = (origin.getX() - RANGE) >> 4; cx <= (origin.getX() + RANGE) >> 4; cx++) {
            for (int cz = (origin.getZ() - RANGE) >> 4; cz <= (origin.getZ() + RANGE) >> 4; cz++) {
                final LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity other : chunk.getBlockEntities().values()) {
                    if (other != this && other instanceof MoonRoseBlockEntity rose && rose.hasEssence()
                            && Math.abs(other.getBlockPos().getX() - origin.getX()) <= RANGE
                            && Math.abs(other.getBlockPos().getY() - origin.getY()) <= RANGE_Y
                            && Math.abs(other.getBlockPos().getZ() - origin.getZ()) <= RANGE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("provides", this.providesEssence);
        if (this.providesEssence) {
            tag.putInt("essence", this.essence);
        }
    }

    /** 标记存在但没写精华量时取配置默认值（对应 1.12 setHasEssence 的默认参数） */
    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        this.providesEssence = tag.getBoolean("provides");
        if (tag.contains("essence")) {
            this.essence = tag.getInt("essence");
        } else if (this.providesEssence) {
            this.essence = TrinketsConfig.SERVER.blocks.moonRoseEssence.get();
        }
    }
}
