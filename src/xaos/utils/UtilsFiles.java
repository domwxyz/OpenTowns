package xaos.utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import xaos.Towns;
import xaos.main.Game;
import xaos.property.MainProperties;
import xaos.property.PropertyFile;

public final class UtilsFiles {

    private static final LocalResourceClassLoader LOCAL_RESOURCE_CLASS_LOADER = new LocalResourceClassLoader();

    private UtilsFiles() { /*static utility class*/ }

    /**
     * Retorna una lista con todos los savegame names o null si no hay
     *
     * @return una lista con todos los savegame names o null si no hay
     */
    public static ArrayList<File> getModsFolders() {
        String sModsFolder = Game.getUserFolder() + Game.getFileSeparator() + Game.MODS_FOLDER1 + Game.getFileSeparator();
        File fFolder = new File(sModsFolder);
        File[] aFiles = fFolder.listFiles();

        if (aFiles.length == 0) {
            return null;
        }

        ArrayList<File> alFiles = new ArrayList<File>(aFiles.length);
        for (int i = 0; i < aFiles.length; i++) {
            if (aFiles[i] != null && aFiles[i].getName() != null && aFiles[i].isDirectory()) {
                // Bingo
                alFiles.add(aFiles[i]);
            }
        }

        if (alFiles.size() == 0) {
            return null;
        }

        // Ordenamos por nombre
        for (int i = 0; i < (alFiles.size() - 1); i++) {
            for (int j = i; j < alFiles.size(); j++) {
                if (alFiles.get(i).getName().compareTo(alFiles.get(j).getName()) > 0) {
                    // Intercambiamos
                    File fAux = alFiles.get(i);
                    alFiles.set(i, alFiles.get(j));
                    alFiles.set(j, fAux);
                }
            }
        }

        return alFiles;
    }

    /**
     * Retorna la ruta entera a un fichero, teniendo en cuenta si está en una
     * misión o no. Tambien mira los mods cargados. Si es misión, mirará primero
     * la carpeta general y después dentro de las carpetas de campaña
     *
     * @param sOriginalFile
     * @param sCampaignID
     * @param sMissionID
     * @return la ruta entera a un fichero, teniendo en cuenta si está en una
     * misión o no
     */
    public static ArrayList<String> getPathToFile(String sOriginalFile, String sCampaignID, String sMissionID) {
        ArrayList<String> alReturn = new ArrayList<String>();

        if (sMissionID == null || sMissionID.trim().length() == 0) {
            // Sin misión, lo pillamos de la carpeta data
            String sPath = Towns.getPropertiesString("DATA_FOLDER") + sOriginalFile; //$NON-NLS-1$
            File f = new File(sPath);
            if (f.exists()) {
                alReturn.add(sPath);
            }
        } else {
			// Misión
            // Primero miramos la carpeta data
            String sPath = Towns.getPropertiesString("DATA_FOLDER") + sOriginalFile; //$NON-NLS-1$
            File f = new File(sPath);
            if (f.exists()) {
                alReturn.add(sPath);
            }

            // Después la carpeta de campaña
            sPath = Towns.getPropertiesString("CAMPAIGNS_FOLDER") + sCampaignID + File.separator + sOriginalFile; //$NON-NLS-1$
            f = new File(sPath);
            if (f.exists()) {
                alReturn.add(sPath);
            }

            // Ahora la de campaña + misión
            sPath = Towns.getPropertiesString("CAMPAIGNS_FOLDER") + sCampaignID + File.separator + sMissionID + File.separator + sOriginalFile; //$NON-NLS-1$
            f = new File(sPath);
            if (f.exists()) {
                alReturn.add(sPath);
            }
        }

        // Ahora cargamos los mods
        getPathToFileMods(alReturn, sOriginalFile, sCampaignID, sMissionID);

        return alReturn;
    }

    /**
     * Retorna la ruta entera a un fichero de mods, teniendo en cuenta si está
     * en una misión o no Si es misión, mirará primero la carpeta general y
     * después dentro de las carpetas de campaña
     *
     * @param sOriginalFile
     * @param sCampaignID
     * @param sMissionID
     * @return la ruta entera a un fichero, teniendo en cuenta si está en una
     * misión o no
     */
    private static void getPathToFileMods(ArrayList<String> alList, String sOriginalFile, String sCampaignID, String sMissionID) {
        File fUserFolder = new File(Game.getUserFolder());
        if (!fUserFolder.exists() || !fUserFolder.isDirectory()) {
            return;
        }

        // Mods
        ArrayList<String> alMods = Game.getModsLoaded();
        if (alMods == null || alMods.size() == 0) {
            return;
        }

        String sModName;
        for (int i = 0; i < alMods.size(); i++) {
            sModName = alMods.get(i);

            if (sMissionID == null || sMissionID.trim().length() == 0) {
                // Sin misión, lo pillamos de la carpeta data
                String sModActionsPath = fUserFolder.getAbsolutePath() + System.getProperty("file.separator") + Game.MODS_FOLDER1 + System.getProperty("file.separator") + sModName + System.getProperty("file.separator") + Towns.getPropertiesString("DATA_FOLDER") + sOriginalFile; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                File f = new File(sModActionsPath);
                if (f.exists()) {
                    alList.add(sModActionsPath);
                }
            } else {
                // Primero miramos la carpeta del mod
                String sPath = fUserFolder.getAbsolutePath() + System.getProperty("file.separator") + Game.MODS_FOLDER1 + System.getProperty("file.separator") + sModName + System.getProperty("file.separator") + Towns.getPropertiesString("DATA_FOLDER") + sOriginalFile; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                if (new File(sPath).exists()) {
                    alList.add(sPath);
                }

                // Ahora miramos la carpeta de la campaña
                sPath = fUserFolder.getAbsolutePath() + System.getProperty("file.separator") + Game.MODS_FOLDER1 + System.getProperty("file.separator") + sModName + System.getProperty("file.separator") + Towns.getPropertiesString("CAMPAIGNS_FOLDER") + sCampaignID + File.separator + sOriginalFile; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                if (new File(sPath).exists()) {
                    alList.add(sPath);
                }

                // Ahora la carpeta de campa+a+misión
                sPath = fUserFolder.getAbsolutePath() + System.getProperty("file.separator") + Game.MODS_FOLDER1 + System.getProperty("file.separator") + sModName + System.getProperty("file.separator") + Towns.getPropertiesString("CAMPAIGNS_FOLDER") + sCampaignID + File.separator + sMissionID + File.separator + sOriginalFile; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                if (new File(sPath).exists()) {
                    alList.add(sPath);
                }
            }
        }
    }

    /**
     * Save the user options to the user folder
     */
    public static void saveOptions() {
        File fSave = new File(Game.getUserFolder(), "towns.ini");
        if (fSave.exists()) {
            fSave.delete();
        }

        try {
            PropertiesWriter pw = new PropertiesWriter(PropertyFile.PROPERTY_FILE_MAIN);
            pw.addSection("General Settings");
            pw.setProperty(MainProperties.WINDOW_WIDTH, UtilsGL.getLastWindowWidth());
            pw.setProperty(MainProperties.WINDOW_HEIGHT, UtilsGL.getLastWindowHeight());
            pw.setProperty(MainProperties.FULLSCREEN, UtilsGL.isFullScreen());
            pw.setProperty(MainProperties.MUSIC, Game.isMusicON());
            pw.setProperty(MainProperties.VOLUME_MUSIC, Game.getVolumeMusic());
            pw.setProperty(MainProperties.FX, Game.isFXON());
            pw.setProperty(MainProperties.VOLUME_FX, Game.getVolumeFX());
            pw.setProperty(MainProperties.MOUSE_SCROLL, Game.isMouseScrollON());
            pw.setProperty(MainProperties.MOUSE_SCROLL_EARS, Game.isMouseScrollEarsON());
            pw.setProperty(MainProperties.MOUSE_2D_CUBES, Game.isMouse2DCubesON());
            pw.setProperty(MainProperties.DISABLED_ITEMS, Game.isDisabledItemsON());
            pw.setProperty(MainProperties.DISABLED_GODS, Game.isDisabledGodsON());
            pw.setProperty(MainProperties.PAUSE_START, Game.isPauseStartON());
            pw.setProperty(MainProperties.AUTOSAVE_DAYS, Game.getAutosaveDays());
            pw.setProperty(MainProperties.SIEGES, Game.getSiegeDifficulty());
            pw.setProperty(MainProperties.SIEGE_PAUSE, Game.isSiegePause());
            pw.setProperty(MainProperties.CARAVAN_PAUSE, Game.isCaravanPause());
            pw.setProperty(MainProperties.ALLOW_BURY, Game.isAllowBury());
            pw.setProperty(MainProperties.PATHFINDING_LEVEL, Game.getPathfindingCPULevel());
            UtilsKeyboard.saveShortcuts(pw);

            pw.addSection("Mod/Servers");
            pw.setProperty(MainProperties.MODS, Game.getModsLoadedString());
            pw.setProperty(MainProperties.SERVERS, Game.getServersString());
            pw.store(fSave);
        } catch (Exception e) {
            Log.log(Log.LEVEL_ERROR, Messages.getString("Utils.13") + e.toString() + "]", "Utils"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    public static File createUserFolder(String sUserFolder) {
        File fUserFolder = new File(sUserFolder);
        if (!fUserFolder.exists() || !fUserFolder.isDirectory()) {
            Log.log(Log.LEVEL_ERROR, Messages.getString("Utils.10") + fUserFolder.getAbsolutePath() + "]", "Utils"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return null;
        }

        File fFolderTowns = new File(fUserFolder.getAbsolutePath() + System.getProperty("file.separator") + ".towns"); //$NON-NLS-1$ //$NON-NLS-2$
        if (!fFolderTowns.exists()) {
            fFolderTowns.mkdir();
        }

        if (!fFolderTowns.exists()) {
            Log.log(Log.LEVEL_ERROR, Messages.getString("Utils.20") + " [" + fFolderTowns.getAbsolutePath() + "]", "Utils"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            return null;
        }

        File fFolderSave = new File(fFolderTowns.getAbsolutePath() + System.getProperty("file.separator") + Game.SAVE_FOLDER1); //$NON-NLS-1$
        if (!fFolderSave.exists()) {
            fFolderSave.mkdir();
        }

        if (!fFolderSave.exists()) {
            Log.log(Log.LEVEL_ERROR, Messages.getString("Utils.25") + " [" + fFolderSave.getAbsolutePath() + "]", "Utils"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            return null;
        }

        File fFolderMods = new File(fFolderTowns.getAbsolutePath() + System.getProperty("file.separator") + Game.MODS_FOLDER1 + System.getProperty("file.separator")); //$NON-NLS-1$ //$NON-NLS-2$
        if (!fFolderMods.exists()) {
            fFolderMods.mkdir();
        }

        if (!fFolderMods.exists()) {
            Log.log(Log.LEVEL_ERROR, Messages.getString("Utils.29") + " [" + fFolderMods.getAbsolutePath() + "]", "Utils"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            return null;
        }

        File fFolderBury = new File(fFolderTowns.getAbsolutePath() + System.getProperty("file.separator") + Game.BURY_FOLDER1 + System.getProperty("file.separator")); //$NON-NLS-1$ //$NON-NLS-2$
        if (!fFolderBury.exists()) {
            fFolderBury.mkdir();
        }

        if (!fFolderBury.exists()) {
            Log.log(Log.LEVEL_ERROR, Messages.getString("Utils.29") + " [" + fFolderBury.getAbsolutePath() + "]", "Utils"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            return null;
        }

        File fFolderScreenshots = new File(fFolderTowns.getAbsolutePath() + System.getProperty("file.separator") + Game.SCREENSHOTS_FOLDER1 + System.getProperty("file.separator")); //$NON-NLS-1$ //$NON-NLS-2$
        if (!fFolderScreenshots.exists()) {
            fFolderScreenshots.mkdir();
        }

        if (!fFolderScreenshots.exists()) {
            Log.log(Log.LEVEL_ERROR, Messages.getString("Utils.18") + " [" + fFolderScreenshots.getAbsolutePath() + "]", "Utils"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            return null;
        }

        return fFolderTowns;
    }

    /**
     * Devuelve una lista de Strings con los languages encontrados (es_ES,
     * en_US, ....)
     *
     * @return una lista de Strings con los languages encontrados (es_ES, en_US,
     * ....)
     */
    public static ArrayList<LanguageData> getLanguages() {
        ArrayList<LanguageData> alReturn = new ArrayList<LanguageData>();

        // Vanilla. Uses DATA_FOLDER (rooted at the app home for a packaged
        // build) rather than a hardcoded relative path, like every other
        // data/ lookup in the game.
        File fLanguagesFolder = new File(Towns.getPropertiesString("DATA_FOLDER") + "languages/"); //$NON-NLS-1$ //$NON-NLS-2$
        if (fLanguagesFolder.exists()) {
            String[] asFiles = fLanguagesFolder.list();
            for (int i = 0; i < asFiles.length; i++) {
                if (asFiles[i] != null && asFiles[i].startsWith("messages") && asFiles[i].endsWith(".properties")) { //$NON-NLS-1$ //$NON-NLS-2$
                    String sID = asFiles[i].substring("messages".length()); //$NON-NLS-1$
                    sID = sID.substring(0, sID.length() - ".properties".length()); //$NON-NLS-1$
                    if (sID.startsWith("_")) { //$NON-NLS-1$
                        sID = sID.substring(1);
                    }

                    LanguageData ld = new LanguageData();

                    // Name
                    Properties prop = new Properties();
                    try {
                        prop.load(new FileInputStream(fLanguagesFolder.getAbsolutePath() + File.separator + asFiles[i]));
                        ld.name = prop.getProperty("LANGUAGE_NAME"); //$NON-NLS-1$
                    } catch (Exception e) {
                        ld.name = null;
                    }

                    if (ld.name == null || ld.name.trim().length() == 0) {
                        ld.name = Messages.getString("Utils.34"); //$NON-NLS-1$
                    }

                    // Mod
                    ld.mod = null;

                    String sLanguage, sCountry;
                    StringTokenizer tokenizer = new StringTokenizer(sID, "_"); //$NON-NLS-1$
                    if (tokenizer.hasMoreTokens()) {
                        sLanguage = tokenizer.nextToken();

                        if (tokenizer.hasMoreTokens()) {
                            sCountry = tokenizer.nextToken();
                        } else {
                            sLanguage = null;
                            sCountry = null;
                        }
                    } else {
                        sLanguage = null;
                        sCountry = null;
                    }

                    if (sLanguage == null) {
                        sLanguage = "en"; //$NON-NLS-1$
                        sCountry = "US"; //$NON-NLS-1$
                    }

                    // Language and country
                    ld.language = sLanguage;
                    ld.country = sCountry;

                    alReturn.add(ld);
                }
            }
        }

        // Mods
        File fUserFolder = new File(Game.getUserFolder());
        if (!fUserFolder.exists() || !fUserFolder.isDirectory()) {
            return alReturn;
        }

        ArrayList<String> alModsLoaded = Game.getModsLoaded();
        if (alModsLoaded == null || alModsLoaded.size() == 0) {
            return alReturn;
        }

        String sModName;
        for (int m = 0; m < alModsLoaded.size(); m++) {
            sModName = alModsLoaded.get(m);

            String sModLanguagesPath = fUserFolder.getAbsolutePath() + System.getProperty("file.separator") + Game.MODS_FOLDER1 + System.getProperty("file.separator") + sModName + System.getProperty("file.separator") + Towns.getPropertiesString("DATA_FOLDER") + "/languages/"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            File f = new File(sModLanguagesPath);
            if (f.exists()) {
                String[] asFiles = f.list();
                for (int i = 0; i < asFiles.length; i++) {
                    if (asFiles[i] != null && asFiles[i].startsWith("messages") && asFiles[i].endsWith(".properties")) { //$NON-NLS-1$ //$NON-NLS-2$
                        String sID = asFiles[i].substring("messages".length()); //$NON-NLS-1$
                        sID = sID.substring(0, sID.length() - ".properties".length()); //$NON-NLS-1$

                        if (sID.startsWith("_")) { //$NON-NLS-1$
                            sID = sID.substring(1);
                        }

                        LanguageData ld = new LanguageData();

                        // Name
                        Properties prop = new Properties();
                        try {
                            prop.load(new FileInputStream(sModLanguagesPath + asFiles[i]));
                            ld.name = prop.getProperty("LANGUAGE_NAME"); //$NON-NLS-1$
                        } catch (Exception e) {
                            ld.name = null;
                        }

                        if (ld.name == null || ld.name.trim().length() == 0) {
                            ld.name = Messages.getString("Utils.34"); //$NON-NLS-1$
                        }

                        // Mod
                        ld.mod = sModName;

                        String sLanguage, sCountry;
                        StringTokenizer tokenizer = new StringTokenizer(sID, "_"); //$NON-NLS-1$
                        if (tokenizer.hasMoreTokens()) {
                            sLanguage = tokenizer.nextToken();

                            if (tokenizer.hasMoreTokens()) {
                                sCountry = tokenizer.nextToken();
                            } else {
                                sLanguage = null;
                                sCountry = null;
                            }
                        } else {
                            sLanguage = null;
                            sCountry = null;
                        }

                        if (sLanguage == null) {
                            sLanguage = "en"; //$NON-NLS-1$
                            sCountry = "US"; //$NON-NLS-1$
                        }

                        // Language and country
                        ld.language = sLanguage;
                        ld.country = sCountry;

                        alReturn.add(ld);
                    }
                }
            }
        }

        return alReturn;
    }

    /**
     * loads a resource bundle that is located in the "./data" directory rather
     * than from the classpath
     *
     * @param name
     * @return
     */
    public static ResourceBundle getResourceBundle(String name) {
        return ResourceBundle.getBundle(name, Locale.getDefault(), LOCAL_RESOURCE_CLASS_LOADER);
    }

}
