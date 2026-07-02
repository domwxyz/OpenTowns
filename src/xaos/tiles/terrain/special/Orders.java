package xaos.tiles.terrain.special;

import xaos.tiles.Tile;

public class Orders {

    private static final long serialVersionUID = -3349547564930871768L;

    public final static Tile[] TERRAIN_ORDERS = {
        new Tile("orders"), //$NON-NLS-1$
        new Tile("ordersBlock"), //$NON-NLS-1$
        new Tile("ordersMiniBlock"), //$NON-NLS-1$
    };

    public Orders() {
        for (int i = 0; i < TERRAIN_ORDERS.length; i++) {
            TERRAIN_ORDERS[i].setTextureID(TERRAIN_ORDERS[i].getIniHeader(), "orders"); //$NON-NLS-1$
        }
    }

    public Tile getOrderTile(boolean bMined) {
        if (bMined) {
            return TERRAIN_ORDERS[0];
        }
        return TERRAIN_ORDERS[1];
    }

    public Tile getOrderTileMiniBlock() {
        return TERRAIN_ORDERS[2];
    }
}
