package xaos.generator;

import xaos.utils.Utils;

public class BezierData extends ParentMapData {

    public String type = null;
    public String wide = null;
    public int level = -1;
    public int depth = 1;
    public int point1xmin = -1;
    public int point1xmax = -1;
    public int point1ymin = -1;
    public int point1ymax = -1;
    public int point2xmin = -1;
    public int point2xmax = -1;
    public int point2ymin = -1;
    public int point2ymax = -1;
    public int controlpoint1xmin = -1;
    public int controlpoint1xmax = -1;
    public int controlpoint1ymin = -1;
    public int controlpoint1ymax = -1;
    public int controlpoint2xmin = -1;
    public int controlpoint2xmax = -1;
    public int controlpoint2ymin = -1;
    public int controlpoint2ymax = -1;

    public BezierData(GeneratorItem item) {
        for (int i = 0; i < item.getList().size(); i++) {
            if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_TYPE)) {
                type = item.getList().get(i).getValue();
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_LEVEL)) {
                level = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_DEPTH)) {
                depth = Utils.launchDice(item.getList().get(i).getValue());
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_WIDE)) {
                wide = item.getList().get(i).getValue();
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_POINT1XMIN)) {
                point1xmin = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_POINT1XMAX)) {
                point1xmax = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_POINT1YMIN)) {
                point1ymin = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_POINT1YMAX)) {
                point1ymax = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_POINT2XMIN)) {
                point2xmin = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_POINT2XMAX)) {
                point2xmax = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_POINT2YMIN)) {
                point2ymin = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_POINT2YMAX)) {
                point2ymax = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_CONTROLPOINT1XMIN)) {
                controlpoint1xmin = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_CONTROLPOINT1XMAX)) {
                controlpoint1xmax = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_CONTROLPOINT1YMIN)) {
                controlpoint1ymin = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_CONTROLPOINT1YMAX)) {
                controlpoint1ymax = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_CONTROLPOINT2XMIN)) {
                controlpoint2xmin = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_CONTROLPOINT2XMAX)) {
                controlpoint2xmax = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_CONTROLPOINT2YMIN)) {
                controlpoint2ymin = Utils.getInteger(item.getList().get(i).getValue(), -1);
            } else if (item.getList().get(i).getName().equalsIgnoreCase(MapGeneratorItem.ITEM_BEZIER_CONTROLPOINT2YMAX)) {
                controlpoint2ymax = Utils.getInteger(item.getList().get(i).getValue(), -1);
            }
        }
    }
}
