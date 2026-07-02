package xaos.generator;

import xaos.utils.Utils;

public class SeedData extends ParentMapData {

    public String type = null;
    public String level = null;
    public String pointx = null;
    public String pointy = null;
    public String id = null;
    public String startingPointID = null;
    public int num = 1;
    public int turns = 0;
    public int northPCT = 0;
    public int southPCT = 0;
    public int eastPCT = 0;
    public int westPCT = 0;
    public int upPCT = 0;
    public int downPCT = 0;
    public int heightMin = -1;
    public int heightMax = -1;

    public SeedData(GeneratorItem item) {
        for (int i = 0; i < item.getList().size(); i++) {
            if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_TYPE)) {
                type = item.getList().get(i).getValue();
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_ID)) {
                id = item.getList().get(i).getValue();
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_STARTING_POINT_ID)) {
                startingPointID = item.getList().get(i).getValue();
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_NUM)) {
                num = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_LEVEL)) {
                level = item.getList().get(i).getValue();
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_TURNS)) {
                turns = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_POINTX)) {
                pointx = item.getList().get(i).getValue();
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_POINTY)) {
                pointy = item.getList().get(i).getValue();
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_NORTHPCT)) {
                northPCT = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_SOUTHPCT)) {
                southPCT = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_EASTPCT)) {
                eastPCT = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_WESTPCT)) {
                westPCT = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_UPPCT)) {
                upPCT = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_DOWNPCT)) {
                downPCT = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_HEIGHT_MIN)) {
                heightMin = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_SEED_HEIGHT_MAX)) {
                heightMax = Utils.getInteger(item.getList().get(i).getValue(), -1);
            }
        }

        if (id == null) {
            id = item.getId();
        }
    }
}
