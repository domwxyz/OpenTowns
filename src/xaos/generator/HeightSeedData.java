package xaos.generator;

import xaos.utils.Utils;

public class HeightSeedData extends ParentMapData {

    public String pointx = null;
    public String pointy = null;
    public String startingPointID = null;
    public int num = 1;
    public int turns = 0;
    public int northPCT = 0;
    public int southPCT = 0;
    public int eastPCT = 0;
    public int westPCT = 0;
    public int flatsBetweenLevels = 0;

    public HeightSeedData(GeneratorItem item) {
        for (int i = 0; i < item.getList().size(); i++) {
            if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_HEIGHTSEED_NUM)) {
                num = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_HEIGHTSEED_TURNS)) {
                turns = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_HEIGHTSEED_STARTING_POINT_ID)) {
                startingPointID = item.getList().get(i).getValue();
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_HEIGHTSEED_FLATS_BETWEEN_LEVELS)) {
                flatsBetweenLevels = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_HEIGHTSEED_POINTX)) {
                pointx = item.getList().get(i).getValue();
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_HEIGHTSEED_POINTY)) {
                pointy = item.getList().get(i).getValue();
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_HEIGHTSEED_NORTHPCT)) {
                northPCT = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_HEIGHTSEED_SOUTHPCT)) {
                southPCT = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_HEIGHTSEED_EASTPCT)) {
                eastPCT = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_HEIGHTSEED_WESTPCT)) {
                westPCT = Utils.launchDice(item.getList().get(i).getValue());
            }
        }
    }
}
