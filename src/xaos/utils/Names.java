package xaos.utils;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import xaos.main.Game;

public final class Names {

    public static HashMap<String, ArrayList<String>> namesHash;

    private static void loadNames(String sCampaignID, String sMissionID) {
        if (namesHash == null) {
            namesHash = new HashMap<String, ArrayList<String>>();

            ArrayList<String> alPaths = Utils.getPathToFile("names.xml", sCampaignID, sMissionID); //$NON-NLS-1$
            for (int i = 0; i < alPaths.size(); i++) {
                loadXMLNames(alPaths.get(i));
            }
        }
    }

    private static void loadXMLNames(String sPathToXML) {
        try {
            Document doc = UtilsXML.loadXMLFile(sPathToXML);
            NodeList nodeList = doc.getDocumentElement().getChildNodes();
            Node node;
            String sPool, sValue;
            for (int i = 0; i < nodeList.getLength(); i++) {
                node = nodeList.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE && node.getChildNodes().item(0) != null) {
                    sPool = node.getNodeName();
                    sValue = node.getChildNodes().item(0).getNodeValue();

                    if (sPool != null && sValue != null) {
                        ArrayList<String> alNames;
                        if (namesHash.containsKey(sPool)) {
                            alNames = namesHash.get(sPool);
                        } else {
                            alNames = new ArrayList<String>();
                        }
                        alNames.add(sValue);
                        namesHash.put(sPool, alNames);
                    }
                }
            }
        } catch (Exception e) {
            Log.log(Log.LEVEL_ERROR, Messages.getString("Names.0") + e.toString() + "]", "Names"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            Game.exit();
        }
    }

    public static String getName(String sPool, String sCampaignID, String sMissionID) {
        loadNames(sCampaignID, sMissionID);

        ArrayList<String> alPool = namesHash.get(sPool);
        if (alPool != null && alPool.size() > 0) {
            return alPool.get(Utils.getRandomBetween(0, alPool.size() - 1));
        }

        return null;
    }

    public static void clear() {
        namesHash = null;
    }
}
