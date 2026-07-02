package xaos.tiles.terrain.special;

import xaos.tiles.Tile;
import xaos.utils.Messages;

public class StockpileTile extends Tile {

    private static final long serialVersionUID = -5131387153362993459L;

    public StockpileTile() {
        super("stockpile"); //$NON-NLS-1$
    }

    public String getTileName() {
        return Messages.getString("Stockpile.0"); //$NON-NLS-1$
    }
}
