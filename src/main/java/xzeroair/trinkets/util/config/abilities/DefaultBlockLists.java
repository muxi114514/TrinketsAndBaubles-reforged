package xzeroair.trinkets.util.config.abilities;

import java.util.List;

/**
 * 能力配置的默认方块列表（对应 1.12 ConfigDefaultReusedConstants）。
 *
 * 移植说明：1.12 的方块 id 在 1.13「扁平化」后大量改名或拆分，逐条迁移规则如下：
 * <ul>
 *   <li>改名：grass→grass_block、brick_block→bricks、snow→snow_block、lit_pumpkin→jack_o_lantern、
 *       melon_block→melon、nether_brick→nether_bricks、red_nether_brick→red_nether_bricks、
 *       end_bricks→end_stone_bricks、magma→magma_block、noteblock→note_block、slime→slime_block、
 *       hardened_clay→terracotta、quartz_ore→nether_quartz_ore</li>
 *   <li>靠 metadata 区分变种的方块改用标签或通配：log/log2→#logs、leaves/leaves2→#leaves、wool→#wool、
 *       stained_glass→*_stained_glass、stained_hardened_clay→*_terracotta、concrete→*_concrete、
 *       stonebrick→*stone_bricks、trapdoor→#wooden_trapdoors、sign→#signs、bed→#beds、
 *       各色木门→#wooden_doors、skull→*_skull 与 *_head</li>
 *   <li>矿石改用矿石标签，从而自动涵盖 1.17 起的深板岩与下界变种</li>
 *   <li>删除：slab2（1.13 后不存在）、armor_stand（是实体而非方块）；
 *       glazed_terracotta 与 log2/leaves2/各色木门已被上面的通配或标签覆盖</li>
 * </ul>
 */
public final class DefaultBlockLists {

    public static final List<String> CLIMBABLE = List.of(
            "minecraft:dirt", "minecraft:grass_block", "minecraft:sand", "minecraft:*cobblestone*",
            "minecraft:*planks*", "minecraft:*stairs", "minecraft:*slab", "minecraft:stone", "minecraft:gravel",
            "#minecraft:gold_ores", "#minecraft:iron_ores", "#minecraft:coal_ores", "#minecraft:logs",
            "minecraft:sponge", "minecraft:glass", "#minecraft:lapis_ores", "minecraft:lapis_block",
            "minecraft:sandstone", "#minecraft:wool", "minecraft:gold_block", "minecraft:iron_block",
            "minecraft:bricks", "minecraft:bookshelf", "minecraft:obsidian", "#minecraft:diamond_ores",
            "minecraft:diamond_block", "#minecraft:redstone_ores", "minecraft:ice", "minecraft:snow_block",
            "minecraft:clay", "minecraft:pumpkin", "minecraft:netherrack", "minecraft:soul_sand",
            "minecraft:glowstone", "minecraft:jack_o_lantern", "minecraft:*_stained_glass",
            "minecraft:*stone_bricks", "minecraft:melon", "minecraft:mycelium", "minecraft:nether_bricks",
            "minecraft:end_stone", "#minecraft:emerald_ores", "minecraft:emerald_block",
            "minecraft:nether_quartz_ore", "minecraft:quartz_block", "minecraft:*terracotta",
            "minecraft:prismarine", "minecraft:sea_lantern", "minecraft:hay_block", "minecraft:coal_block",
            "minecraft:packed_ice", "minecraft:red_sandstone", "minecraft:purpur_block", "minecraft:purpur_pillar",
            "minecraft:end_stone_bricks", "minecraft:magma_block", "minecraft:nether_wart_block",
            "minecraft:red_nether_bricks", "minecraft:bone_block", "minecraft:*_concrete",
            "minecraft:*_concrete_powder", "#minecraft:leaves", "minecraft:dispenser", "minecraft:note_block",
            "minecraft:sticky_piston", "minecraft:piston", "minecraft:tnt", "minecraft:chest",
            "minecraft:crafting_table", "minecraft:furnace", "minecraft:cactus", "minecraft:jukebox",
            "#minecraft:wooden_trapdoors", "minecraft:iron_bars", "minecraft:glass_pane",
            "minecraft:enchanting_table", "minecraft:end_portal_frame", "minecraft:redstone_lamp",
            "minecraft:ender_chest", "minecraft:beacon", "minecraft:anvil", "minecraft:trapped_chest",
            "minecraft:daylight_detector", "minecraft:redstone_block", "minecraft:hopper", "minecraft:dropper",
            "minecraft:*_stained_glass_pane", "minecraft:iron_trapdoor", "minecraft:slime_block",
            "minecraft:end_rod", "minecraft:chorus_plant", "minecraft:chorus_flower", "minecraft:observer",
            "minecraft:*shulker_box", "#minecraft:signs", "#minecraft:wooden_doors", "minecraft:iron_door",
            "#minecraft:beds", "minecraft:flower_pot", "minecraft:*_skull", "minecraft:*_head");

    /** 自然时运作用的矿石（1.12 SkilledMiner BLOCKS.Blocks） */
    public static final List<String> SKILLED_MINER_FORTUNE = List.of(
            "#minecraft:coal_ores", "#minecraft:lapis_ores", "#minecraft:diamond_ores",
            "#minecraft:redstone_ores", "#minecraft:emerald_ores", "minecraft:nether_quartz_ore");

    /** 额外掉落经验的矿石（1.12 SkilledMiner BLOCKS.xPBlocks） */
    public static final List<String> SKILLED_MINER_BONUS_XP = List.of(
            "#minecraft:coal_ores", "#minecraft:iron_ores", "#minecraft:gold_ores", "#minecraft:lapis_ores",
            "#minecraft:redstone_ores", "#minecraft:diamond_ores", "#minecraft:emerald_ores",
            "minecraft:nether_quartz_ore");

    /** 至少掉 1 点经验的方块（1.12 SkilledMiner BLOCKS.MinBlocks） */
    public static final List<String> SKILLED_MINER_MIN_XP = List.of("minecraft:stone", "minecraft:end_stone");

    /** 大手范围挖掘排除的方块（1.12 MINING_EXTENDED_BLACKLIST） */
    public static final List<String> LARGE_HANDS_BLACKLIST = List.of("dynamictrees:*");

    private DefaultBlockLists() {
    }
}
