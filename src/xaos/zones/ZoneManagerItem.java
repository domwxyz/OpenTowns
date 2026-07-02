package xaos.zones;

import java.util.ArrayList;

import xaos.tiles.Tile;
import xaos.utils.Messages;

public class ZoneManagerItem {

    private static String TYPE_STR_DINING = "DINING"; //$NON-NLS-1$
    private static String TYPE_STR_HOSPITAL = "HOSPITAL"; //$NON-NLS-1$
    private static String TYPE_STR_PERSONAL = "PERSONAL"; //$NON-NLS-1$
    private static String TYPE_STR_WORKSHOP = "WORKSHOP"; //$NON-NLS-1$
    private static String TYPE_STR_HEROROOM = "HEROROOM"; //$NON-NLS-1$
    private static String TYPE_STR_SOCIAL = "SOCIAL"; //$NON-NLS-1$
    private static String TYPE_STR_BARRACKS = "BARRACKS"; //$NON-NLS-1$

    public static int TYPE_DINING = 1;
    public static int TYPE_HOSPITAL = 2;
    public static int TYPE_PERSONAL = 3;
    public static int TYPE_WORKSHOP = 4;
    public static int TYPE_HERO_ROOM = 5;
    public static int TYPE_SOCIAL = 6;
    public static int TYPE_BARRACKS = 7;

    private String iniHeader;
    private String name;
    private int type; // Tipo (personal, hospital, comedor, workshop, ...)
    private int minWidth;
    private int minHeight;
    private Tile tile;
    private ArrayList<String> neighbors;

    public void setIniHeader(String iniHeader) {
        this.iniHeader = iniHeader;
    }

    public String getIniHeader() {
        return iniHeader;
    }

    public void setName(String name) {
        this.name = name;

        setTile(new Tile(iniHeader));
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setType(String sType) throws Exception {
        if (sType == null || sType.length() == 0) {
            throw new Exception(Messages.getString("ZoneManagerItem.0")); //$NON-NLS-1$
        }

        if (sType.equalsIgnoreCase(TYPE_STR_DINING)) {
            setType(TYPE_DINING);
        } else if (sType.equalsIgnoreCase(TYPE_STR_HOSPITAL)) {
            setType(TYPE_HOSPITAL);
        } else if (sType.equalsIgnoreCase(TYPE_STR_PERSONAL)) {
            setType(TYPE_PERSONAL);
        } else if (sType.equalsIgnoreCase(TYPE_STR_WORKSHOP)) {
            setType(TYPE_WORKSHOP);
        } else if (sType.equalsIgnoreCase(TYPE_STR_HEROROOM)) {
            setType(TYPE_HERO_ROOM);
        } else if (sType.equalsIgnoreCase(TYPE_STR_SOCIAL)) {
            setType(TYPE_SOCIAL);
        } else if (sType.equalsIgnoreCase(TYPE_STR_BARRACKS)) {
            setType(TYPE_BARRACKS);
        } else {
            throw new Exception(Messages.getString("ZoneManagerItem.1") + sType + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
    }

    public void setMinWidth(String sMinWidth) {
        if (sMinWidth == null || sMinWidth.trim().length() == 0) {
            setMinWidth(3);
        } else {
            setMinWidth(Integer.parseInt(sMinWidth));
        }
    }

    public int getMinWidth() {
        return minWidth;
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
    }

    public void setMinHeight(String sMinHeight) {
        if (sMinHeight == null || sMinHeight.trim().length() == 0) {
            setMinHeight(3);
        } else {
            setMinHeight(Integer.parseInt(sMinHeight));
        }
    }

    public int getMinHeight() {
        return minHeight;
    }

    public void setTile(Tile tile) {
        this.tile = tile;
    }

    public Tile getTile() {
        return tile;
    }

    public void setNeighbors(ArrayList<String> neighbors) {
        this.neighbors = neighbors;
    }

    public ArrayList<String> getNeighbors() {
        return neighbors;
    }
}
