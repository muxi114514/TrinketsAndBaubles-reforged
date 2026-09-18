package xzeroair.trinkets.init;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import xzeroair.trinkets.races.EntityRace;
import xzeroair.trinkets.races.RaceInformations;
import xzeroair.trinkets.races.dragon.RaceDragon;
import xzeroair.trinkets.races.dwarf.RaceDwarf;
import xzeroair.trinkets.races.elf.RaceElf;
import xzeroair.trinkets.races.faelis.RaceFaelis;
import xzeroair.trinkets.races.fairy.RaceFairy;
import xzeroair.trinkets.races.goblin.RaceGoblin;
import xzeroair.trinkets.races.human.RaceHuman;
import xzeroair.trinkets.races.taurus.RaceTaurus;
import xzeroair.trinkets.races.titan.RaceTitan;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.RaceConfig;
import xzeroair.trinkets.util.helpers.EquipmentWeights;
import xzeroair.trinkets.util.helpers.damage.DamageTypeRules;

/**
 * 种族注册表（对应 1.12 Registries 的 races 部分 + init/EntityRaces + EntityRace.registerRaces）。
 *
 * 移植说明：体型与魔力亲和在 1.12 是注册时从配置读取的常量；1.20.1 的注册发生在配置加载之前，
 * 故此处填 1.12 的配置默认值，后续由种族配置在世界加载时覆盖（P3 后段接入）。
 * 未启用的种族（Slime/Orc/Succubus/Incubus/Nymph/Siren/Mixed）在 1.12 即为注释状态，不移。
 */
public class ModRaces {

    public static final ResourceLocation REGISTRY_ID = new ResourceLocation(Reference.MODID, "races");

    public static final DeferredRegister<EntityRace> RACES = DeferredRegister.create(REGISTRY_ID, Reference.MODID);

    private static final Supplier<IForgeRegistry<EntityRace>> REGISTRY = RACES.makeRegistry(RegistryBuilder::new);

    public static final RegistryObject<EntityRace> NONE = RACES.register("none",
            () -> new EntityRace("None", "00000000-0000-0000-0000-000000000000", 11107684, 16374701)
                    .setRaceSize(100).setMagicAffinity(100));

    public static final RegistryObject<EntityRace> HUMAN = RACES.register("human",
            () -> new EntityRace("Human", "c82ec7c3-2a9d-4a08-b0dd-7ce086c6771b", 11107684, 16374701)
                    .setRaceSize(100).setMagicAffinity(100)
                    .setHandlerFactory(RaceHuman::new)
                    .setInformation(RaceInformations.HUMAN));

    public static final RegistryObject<EntityRace> FAIRY = RACES.register("fairy",
            () -> new EntityRace("Fairy", "e5869fac-0949-41f2-889b-4e6b8ca6d2e7", 12514535, 962222)
                    .setRaceSize(25).setMagicAffinity(500).setCanFly(true)
                    .setHandlerFactory(RaceFairy::new)
                    .setInformation(RaceInformations.FAIRY));

    public static final RegistryObject<EntityRace> DWARF = RACES.register("dwarf",
            () -> new EntityRace("Dwarf", "917b555b-944a-4e44-afb6-ca638c6d91e5", 10832170, 7039851)
                    .setRaceSize(75).setMagicAffinity(100)
                    .setHandlerFactory(RaceDwarf::new)
                    .setInformation(RaceInformations.DWARF));

    public static final RegistryObject<EntityRace> TITAN = RACES.register("titan",
            () -> new EntityRace("Titan", "a3bc433b-7bb7-4bd9-a88c-5fd120d04d59", 10066329, 3223595)
                    .setRaceSize(300).setMagicAffinity(50)
                    .setHandlerFactory(RaceTitan::new)
                    .setInformation(RaceInformations.TITAN));

    public static final RegistryObject<EntityRace> ELF = RACES.register("elf",
            () -> new EntityRace("Elf", "25f92404-35f3-453b-ad48-9b788b2e12fc", 16374701, 11107684)
                    .setRaceSize(100).setMagicAffinity(200)
                    .setHandlerFactory(RaceElf::new)
                    .setInformation(RaceInformations.ELF));

    public static final RegistryObject<EntityRace> GOBLIN = RACES.register("goblin",
            () -> new EntityRace("Goblin", "d917999a-0399-4c39-bfc5-79784dfff6ed", 6588004, 3096367)
                    .setRaceSize(50).setMagicAffinity(75)
                    .setHandlerFactory(RaceGoblin::new)
                    .setInformation(RaceInformations.GOBLIN));

    public static final RegistryObject<EntityRace> FAELIS = RACES.register("faelis",
            () -> new EntityRace("Faelis", "cdccefa8-6a67-4394-b70d-c737953887a2", 16571252, 4465933)
                    .setRaceSize(85).setMagicAffinity(125)
                    .setHandlerFactory(RaceFaelis::new)
                    .setInformation(RaceInformations.FAELIS));

    public static final RegistryObject<EntityRace> DRAGON = RACES.register("dragon",
            () -> new EntityRace("Dragon", "3b75821e-6ec6-4dfe-9612-b7a988a7b30b", 3289650, 9509561)
                    .setRaceSize(120).setMagicAffinity(400).setCanFly(true)
                    .setHandlerFactory(RaceDragon::new)
                    .setInformation(RaceInformations.DRAGON));

    public static final RegistryObject<EntityRace> TAURUS = RACES.register("taurus",
            () -> new EntityRace("Taurus", "07f0d6c2-4177-412e-8de5-07c401209e44", 1315860, 4271658)
                    .setRaceSize(120).setMagicAffinity(100)
                    .setHandlerFactory(RaceTaurus::new)
                    .setInformation(RaceInformations.TAURUS));

    public static IForgeRegistry<EntityRace> registry() {
        return REGISTRY.get();
    }

    public static void register(IEventBus modEventBus) {
        RACES.register(modEventBus);
    }

    /**
     * 把配置里的体型与魔力亲和写回种族定义。
     *
     * 移植说明：1.12 在注册种族时直接读配置（那时配置已加载）；1.20.1 的注册发生在配置加载之前，
     * 故注册期先填默认值，待 SERVER 配置加载/重载后再由本方法覆盖。
     */
    public static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getType() != ModConfig.Type.SERVER) {
            return;
        }
        DamageTypeRules.clearCache();
        EquipmentWeights.clearCache();
        apply(HUMAN.get(), TrinketsConfig.SERVER.races.human);
        apply(FAIRY.get(), TrinketsConfig.SERVER.races.fairy);
        apply(DWARF.get(), TrinketsConfig.SERVER.races.dwarf);
        apply(TITAN.get(), TrinketsConfig.SERVER.races.titan);
        apply(GOBLIN.get(), TrinketsConfig.SERVER.races.goblin);
        apply(ELF.get(), TrinketsConfig.SERVER.races.elf);
        apply(FAELIS.get(), TrinketsConfig.SERVER.races.faelis);
        apply(DRAGON.get(), TrinketsConfig.SERVER.races.dragon);
        apply(TAURUS.get(), TrinketsConfig.SERVER.races.taurus);
    }

    private static void apply(EntityRace race, RaceConfig<?> config) {
        race.setRaceHeight(config.height.get());
        race.setRaceWidth(config.width.get());
        race.setMagicAffinity(config.magicAffinity.get());
    }

    private ModRaces() {
    }
}