package xzeroair.trinkets.races;

import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 各族的展示信息实例。颜色与特征变体数忠实取自 1.12 各族的 DefaultInformation。
 */
public class RaceInformations {

    public static final RaceInformation NONE = new RaceInformation();

    public static final RaceInformation HUMAN = new RaceInformation(
            100, 100, 11107684, 16374701, 11107684, 0, 0, () -> TrinketsConfig.SERVER.races.human);

    public static final RaceInformation FAIRY = new RaceInformation(
            25, 25, 12514535, 962222, 12514535, 2, 0, () -> TrinketsConfig.SERVER.races.fairy);

    public static final RaceInformation DWARF = new RaceInformation(
            75, 75, 10832170, 7039851, 10832170, 0, 0, () -> TrinketsConfig.SERVER.races.dwarf);

    public static final RaceInformation TITAN = new RaceInformation(
            300, 300, 10066329, 3223595, 10066329, 0, 0, () -> TrinketsConfig.SERVER.races.titan);

    public static final RaceInformation GOBLIN = new RaceInformation(
            50, 50, 6588004, 3096367, 6588004, 0, 0, () -> TrinketsConfig.SERVER.races.goblin);

    public static final RaceInformation ELF = new RaceInformation(
            100, 100, 16374701, 11107684, 16374701, 0, 0, () -> TrinketsConfig.SERVER.races.elf);

    public static final RaceInformation FAELIS = new RaceInformation(
            85, 85, 16571252, 4465933, 16571252, 0, 1, () -> TrinketsConfig.SERVER.races.faelis);

    public static final RaceInformation DRAGON = new RaceInformation(
            120, 120, 3289650, 9509561, 3289650, 0, 3, () -> TrinketsConfig.SERVER.races.dragon);

    public static final RaceInformation TAURUS = new RaceInformation(
            120, 120, 1315860, 2894892, 16770876, 3, 1, () -> TrinketsConfig.SERVER.races.taurus);

    private RaceInformations() {
    }
}
