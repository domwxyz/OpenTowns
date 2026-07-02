package xaos.utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.lwjgl.openal.AL;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.AudioLoader;
import org.newdawn.slick.openal.SoundStore;
import org.newdawn.slick.util.ResourceLoader;

import xaos.Towns;
import xaos.main.Game;

public final class UtilsAL {

    public final static String SOURCE_MUSIC_MAINMENU = "musicMM"; //$NON-NLS-1$
    public final static String SOURCE_MUSIC_INGAME = "musicIG"; //$NON-NLS-1$

    public final static String SOURCE_FX_CLICK = "fxclick"; //$NON-NLS-1$

//	public final static String SOURCE_FX_CHOP = 6;
    public final static String SOURCE_FX_MINE = "fxmine"; //$NON-NLS-1$
//	public final static String SOURCE_FX_DIG = 8;
    public final static String SOURCE_FX_EAT = "fxeat"; //$NON-NLS-1$

    public final static String SOURCE_FX_DEAD = "fxdead"; //$NON-NLS-1$

    public final static String SOURCE_FX_BUILDING = "fxbuilding"; //$NON-NLS-1$

    private static HashMap<String, Audio> hmAudio;
    private static boolean openALON;

    /**
     * Carga los ficheros de audio
     *
     * @return true si todo ok
     */
    private static boolean loadALData() {
        hmAudio = new HashMap<String, Audio>();

        // Deshabilitamos el log de la libreria
        org.newdawn.slick.util.Log.setVerbose(false);

        Properties propsAudio = new Properties();
        try {
            propsAudio.load(new FileInputStream("audio.ini")); //$NON-NLS-1$

            // Mods
            File fUserFolder = new File(Game.getUserFolder());
            if (fUserFolder.exists() && fUserFolder.isDirectory()) {
                ArrayList<String> alMods = Game.getModsLoaded();
                if (alMods != null && alMods.size() > 0) {
                    for (int i = 0; i < alMods.size(); i++) {
                        String sModAudioIniPath = fUserFolder.getAbsolutePath() + System.getProperty("file.separator") + Game.MODS_FOLDER1 + System.getProperty("file.separator") + alMods.get(i) + System.getProperty("file.separator") + "audio.ini";
                        File fIni = new File(sModAudioIniPath);
                        if (fIni.exists()) {
                            propsAudio.load(new FileInputStream(fIni));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Iterator<String> itKeys = propsAudio.stringPropertyNames().iterator();
        String sKey;
        while (itKeys.hasNext()) {
            sKey = itKeys.next();
            if (!loadAudio(sKey, propsAudio.getProperty(sKey))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Carga un fichero de audio en la hash
     *
     * @param sKey
     * @param sFile Fichero, si se le pasa null (o vacío) no pasa nada, mete
     * null en la hash
     * @return
     */
    private static boolean loadAudio(String sKey, String sFile) {
        String sFilePath = Towns.getPropertiesString("AUDIO_FOLDER") + sFile; //$NON-NLS-1$

        if (sFile == null || sFile.trim().length() == 0) {
            hmAudio.put(sKey, null);
            return true;
        }

        // Si el fichero no existe miraremos las carpetas de los mods activos
        File fAudio = new File(sFilePath);
        if (!fAudio.exists()) {
            // Mods
            File fUserFolder = new File(Game.getUserFolder());
            if (fUserFolder.exists() && fUserFolder.isDirectory()) {
                ArrayList<String> alMods = Game.getModsLoaded();
                if (alMods != null && alMods.size() > 0) {
                    for (int i = 0; i < alMods.size(); i++) {
                        String sModAudioPath = fUserFolder.getAbsolutePath() + System.getProperty("file.separator") + Game.MODS_FOLDER1 + System.getProperty("file.separator") + alMods.get(i) + System.getProperty("file.separator") + Towns.getPropertiesString("AUDIO_FOLDER") + sFile;
                        File fIni = new File(sModAudioPath);
                        if (fIni.exists()) {
                            sFilePath = sModAudioPath;
                            break;
                        }
                    }
                }
            }
        }
        Audio audio = null;
        try {
            audio = AudioLoader.getAudio("OGG", ResourceLoader.getResourceAsStream(sFilePath)); //$NON-NLS-1$
        } catch (Exception e) {
        }

        if (audio == null) {
            Log.log(Log.LEVEL_ERROR, Messages.getString("UtilsAL.5") + sFilePath + "]", "UtilsAL"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return false;
        }

        hmAudio.put(sKey, audio);

        return true;
    }

    public static void initAL(int musicVolume, int FXVolume) {
        if (Game.isMusicON() || Game.isFXON()) {
            if (!isOpenALON()) {
                if (!loadALData()) {
                    Log.log(Log.LEVEL_ERROR, Messages.getString("UtilsAL.8"), "UtilsAL"); //$NON-NLS-1$ //$NON-NLS-2$
                    Game.exit();
                }
                setOpenALON(true);
                setMusicVolume(musicVolume);
                setFXVolume(FXVolume);
            }
        }
    }

    public static void play(String sKey, int level) {
        if (level == Game.getWorld().getView().z) {
            play(sKey);
        }
    }

    public static void play(String sKey) {
        if (!Game.isMusicON() && !Game.isFXON()) {
            return;
        }

        Audio audio = hmAudio.get(sKey);
        if (audio != null) {
            if (isMusic(sKey)) {
                if (!audio.isPlaying()) {
                    if (Game.isMusicON()) {
                        audio.playAsMusic(1, 1, true);
                    }
                }
            } else {
                if (Game.isFXON()) {
                    audio.playAsSoundEffect(1, 1, false);
                }
            }
        }
    }

    public static void stop(String sKey) {
        if (hmAudio == null) {
            return;
        }

        Audio audio = hmAudio.get(sKey);
        if (audio != null && audio.isPlaying()) {
            audio.stop();
        }
    }

    public static void stopMusic() {
        if (hmAudio == null) {
            return;
        }

        Iterator<String> itAudios = hmAudio.keySet().iterator();
        Audio audio;
        String sKey;
        while (itAudios.hasNext()) {
            sKey = itAudios.next();
            if (sKey != null && isMusic(sKey)) {
                audio = hmAudio.get(sKey);
                if (audio != null && audio.isPlaying()) {
                    audio.stop();
                }
            }
        }
    }

    public static void stopFX() {
        if (hmAudio == null) {
            return;
        }

        Iterator<String> itAudios = hmAudio.keySet().iterator();
        Audio audio;
        String sKey;
        while (itAudios.hasNext()) {
            sKey = itAudios.next();
            if (sKey != null && !isMusic(sKey)) {
                audio = hmAudio.get(sKey);
                if (audio != null && audio.isPlaying()) {
                    audio.stop();
                }
            }
        }
    }

    public static void setMusicVolume(int iVolume) {
        SoundStore ss = SoundStore.get();
        if (ss != null) {
            ss.setMusicVolume(0.1f * iVolume);
            ss.setCurrentMusicVolume(0.1f * iVolume);
        }
    }

    public static void setFXVolume(int iVolume) {
        SoundStore ss = SoundStore.get();
        if (ss != null) {
            ss.setSoundVolume(0.1f * iVolume);
        }
    }

    public static void destroy() {
        AL.destroy();
    }

    private static boolean isMusic(String sKey) {
        return sKey != null && (sKey.equals(SOURCE_MUSIC_MAINMENU) || sKey.equals(SOURCE_MUSIC_INGAME));
    }

    public static void setOpenALON(boolean openALON) {
        UtilsAL.openALON = openALON;
    }

    public static boolean isOpenALON() {
        return openALON;
    }

    public static boolean exists(String fxKey) {
        if (hmAudio != null) {
            return hmAudio.containsKey(fxKey);
        } else {
            return true;
        }
    }

    public static void clearPropertiesAudio() {
        if (hmAudio != null) {
            stopFX();
            stopMusic();

            hmAudio.clear();
            hmAudio = null;
            loadALData();
        }
    }
}
