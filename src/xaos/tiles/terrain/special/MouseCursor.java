package xaos.tiles.terrain.special;

import xaos.panels.MainPanel;
import xaos.tiles.Tile;

public class MouseCursor {

    private static final long serialVersionUID = -1691568571780933791L;

    public final static Tile[] TERRAIN_CURSORS = {
        new Tile("mouseCursor"), //$NON-NLS-1$
        new Tile("mouseCursorBlock"), //$NON-NLS-1$
        new Tile("mouseCursorMiniBlock"), //$NON-NLS-1$
        new Tile("mouseCursorAir"), //$NON-NLS-1$
    };

    public MouseCursor() {
        for (int i = 0; i < TERRAIN_CURSORS.length; i++) {
            TERRAIN_CURSORS[i].setTextureID(TERRAIN_CURSORS[i].getIniHeader(), "mouseCursor"); //$NON-NLS-1$
        }
    }

    public Tile getMouseCursor(boolean bMined) {
        if (bMined) {
            return TERRAIN_CURSORS[0];
        } else {
            if (MainPanel.bMiniBlocksON) {
                return TERRAIN_CURSORS[2];
            } else {
                return TERRAIN_CURSORS[1];
            }
        }
    }

    public Tile getMouseCursorBlock() {
        return TERRAIN_CURSORS[1];
    }

    public Tile getMouseCursorMiniBlock() {
        return TERRAIN_CURSORS[2];
    }

    public Tile getMouseCursorAir() {
        return TERRAIN_CURSORS[3];
    }
}
