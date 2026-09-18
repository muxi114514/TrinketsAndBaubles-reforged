package xzeroair.trinkets.loot;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.traits.AbilityDispatcher;
import xzeroair.trinkets.traits.abilities.interfaces.IMiningAbility;

/**
 * 方块掉落 → 种族钩子 + 能力（全局战利品修改器）。对应 1.12 BlockBreakEvents#blockDrops（HarvestDropsEvent）。
 *
 * 只处理「有实体破坏的方块战利品表」：上下文里同时有 BLOCK_STATE、ORIGIN 与生物 THIS_ENTITY，其余战利品表直接放行。
 * 掉落几率沿用 1.12 语义：能力链把几率降到 ≤ 0 即不掉落；介于 0~1 之间时每组掉落按该几率保留。
 *
 * 移植说明：1.20.1 的 Forge 删除了 HarvestDropsEvent，掉落由战利品表生成，GLM 是改写掉落的标准入口。
 */
public class AbilityBlockDropsModifier extends LootModifier {

    public static final Supplier<Codec<AbilityBlockDropsModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, AbilityBlockDropsModifier::new)));

    public AbilityBlockDropsModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (BlockDropContext.isRerolling()) {
            return generatedLoot;
        }
        final BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        final Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        final Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (state == null || origin == null || !(entity instanceof LivingEntity breaker)) {
            return generatedLoot;
        }
        final EntityProperties properties = EntityProperties.get(breaker);
        if (properties == null) {
            return generatedLoot;
        }
        final ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        final BlockDropContext drop = new BlockDropContext(context.getLevel(), BlockPos.containing(origin), state,
                tool == null ? ItemStack.EMPTY : tool, context.getParamOrNull(LootContextParams.BLOCK_ENTITY), breaker);
        properties.getRaceHandler().blockDrops(breaker, generatedLoot);
        final float chance = AbilityDispatcher.chainFloat(breaker, IMiningAbility.class, 1.0F,
                (ability, current) -> ability.blockDrops(breaker, drop, generatedLoot, current));
        if (chance <= 0.0F) {
            generatedLoot.clear();
        } else if (chance < 1.0F) {
            generatedLoot.removeIf(stack -> context.getRandom().nextFloat() > chance);
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
