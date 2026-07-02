package xaos.stockpiles;

import java.util.ArrayList;

public class StockpileTempData {

    private ArrayList<String> alElements; // iniheader del item
    private ArrayList<Boolean> alElementsStatus; // Status, true enabled, false disabled

    public StockpileTempData() {
        alElements = new ArrayList<String>();
        alElementsStatus = new ArrayList<Boolean>();
    }

    public void addElement(String sElement, boolean bStatus) {
        alElements.add(sElement);
        alElementsStatus.add(new Boolean(bStatus));
    }

    public ArrayList<String> getAlElements() {
        return alElements;
    }

    public ArrayList<Boolean> getAlElementsStatus() {
        return alElementsStatus;
    }
}
