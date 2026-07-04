package xaos.utils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

import xaos.Towns;
import xaos.main.Game;

/**
 * Game audio, ported from slick-util/LWJGL 2 to LWJGL 3 OpenAL + STB Vorbis.
 * The public API is unchanged; only the internals differ. All audio files
 * are small OGGs (~1.6 MB total) and are fully decoded into AL buffers at
 * load time, no streaming.
 *
 * Playback model matches slick-util's: one dedicated looping music source
 * (starting new music replaces the old) and a small pool of fire-and-forget
 * FX sources. Music volume changes apply live; FX volume applies to
 * subsequent plays, as in slick-util.
 */
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

    private static final int FX_SOURCE_COUNT = 8;

    // AL buffer per audio.ini key; a key configured with an empty file maps
    // to null, matching the old behavior.
    private static HashMap<String, Integer> hmAudio;
    private static boolean openALON;

    private static long alDevice;
    private static long alContext;
    private static int musicSource;
    private static int[] fxSources;
    private static int nextFXSource;
    private static String currentMusicKey;
    private static float musicVolume = 1f;
    private static float fxVolume = 1f;

    /**
     * Carga los ficheros de audio
     *
     * @return true si todo ok
     */
    private static boolean loadALData() {
        if (!initALContext()) {
            return false;
        }

        hmAudio = new HashMap<String, Integer>();

        Properties propsAudio = new Properties();
        try {
            propsAudio.load(new FileInputStream(Towns.resolveHome("audio.ini"))); //$NON-NLS-1$

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

    /** Opens the OpenAL device and creates the sources. Idempotent. */
    private static boolean initALContext() {
        if (alDevice != MemoryUtil.NULL) {
            return true;
        }

        alDevice = ALC10.alcOpenDevice((java.nio.ByteBuffer) null);
        if (alDevice == MemoryUtil.NULL) {
            Log.log(Log.LEVEL_ERROR, "Could not open the default OpenAL device", "UtilsAL"); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }

        alContext = ALC10.alcCreateContext(alDevice, (IntBuffer) null);
        if (alContext == MemoryUtil.NULL || !ALC10.alcMakeContextCurrent(alContext)) {
            Log.log(Log.LEVEL_ERROR, "Could not create the OpenAL context", "UtilsAL"); //$NON-NLS-1$ //$NON-NLS-2$
            ALC10.alcCloseDevice(alDevice);
            alDevice = MemoryUtil.NULL;
            return false;
        }

        ALCCapabilities deviceCaps = ALC.createCapabilities(alDevice);
        AL.createCapabilities(deviceCaps);

        musicSource = AL10.alGenSources();
        AL10.alSourcei(musicSource, AL10.AL_LOOPING, AL10.AL_TRUE);

        fxSources = new int[FX_SOURCE_COUNT];
        for (int i = 0; i < FX_SOURCE_COUNT; i++) {
            fxSources[i] = AL10.alGenSources();
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

        Integer buffer = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channels = stack.mallocInt(1);
            IntBuffer sampleRate = stack.mallocInt(1);
            ShortBuffer pcm = STBVorbis.stb_vorbis_decode_filename(sFilePath, channels, sampleRate);
            if (pcm != null) {
                try {
                    int alBuffer = AL10.alGenBuffers();
                    int format = channels.get(0) == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                    AL10.alBufferData(alBuffer, format, pcm, sampleRate.get(0));
                    buffer = alBuffer;
                } finally {
                    LibCStdlib.free(pcm); // stb allocates with malloc
                }
            }
        } catch (Exception e) {
        }

        if (buffer == null) {
            Log.log(Log.LEVEL_ERROR, Messages.getString("UtilsAL.5") + sFilePath + "]", "UtilsAL"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return false;
        }

        hmAudio.put(sKey, buffer);

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

        Integer buffer = hmAudio.get(sKey);
        if (buffer != null) {
            if (isMusic(sKey)) {
                if (!(sKey.equals(currentMusicKey) && isPlaying(musicSource))) {
                    if (Game.isMusicON()) {
                        AL10.alSourceStop(musicSource);
                        AL10.alSourcei(musicSource, AL10.AL_BUFFER, buffer);
                        AL10.alSourcef(musicSource, AL10.AL_GAIN, musicVolume);
                        AL10.alSourcePlay(musicSource);
                        currentMusicKey = sKey;
                    }
                }
            } else {
                if (Game.isFXON()) {
                    int source = obtainFXSource();
                    AL10.alSourceStop(source);
                    AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
                    AL10.alSourcef(source, AL10.AL_GAIN, fxVolume);
                    AL10.alSourcePlay(source);
                }
            }
        }
    }

    /** A free FX source, or (like slick-util) the oldest one, stolen. */
    private static int obtainFXSource() {
        for (int i = 0; i < FX_SOURCE_COUNT; i++) {
            int candidate = fxSources[(nextFXSource + i) % FX_SOURCE_COUNT];
            if (!isPlaying(candidate)) {
                nextFXSource = (nextFXSource + i + 1) % FX_SOURCE_COUNT;
                return candidate;
            }
        }
        int stolen = fxSources[nextFXSource];
        nextFXSource = (nextFXSource + 1) % FX_SOURCE_COUNT;
        return stolen;
    }

    private static boolean isPlaying(int source) {
        return AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING;
    }

    public static void stop(String sKey) {
        if (hmAudio == null) {
            return;
        }

        Integer buffer = hmAudio.get(sKey);
        if (buffer == null) {
            return;
        }

        if (isMusic(sKey)) {
            if (sKey.equals(currentMusicKey) && isPlaying(musicSource)) {
                AL10.alSourceStop(musicSource);
            }
        } else {
            for (int i = 0; i < FX_SOURCE_COUNT; i++) {
                if (AL10.alGetSourcei(fxSources[i], AL10.AL_BUFFER) == buffer && isPlaying(fxSources[i])) {
                    AL10.alSourceStop(fxSources[i]);
                }
            }
        }
    }

    public static void stopMusic() {
        if (hmAudio == null) {
            return;
        }

        if (isPlaying(musicSource)) {
            AL10.alSourceStop(musicSource);
        }
    }

    public static void stopFX() {
        if (hmAudio == null) {
            return;
        }

        for (int i = 0; i < FX_SOURCE_COUNT; i++) {
            if (isPlaying(fxSources[i])) {
                AL10.alSourceStop(fxSources[i]);
            }
        }
    }

    public static void setMusicVolume(int iVolume) {
        musicVolume = 0.1f * iVolume;
        if (alDevice != MemoryUtil.NULL) {
            AL10.alSourcef(musicSource, AL10.AL_GAIN, musicVolume);
        }
    }

    public static void setFXVolume(int iVolume) {
        fxVolume = 0.1f * iVolume;
    }

    public static void destroy() {
        if (alDevice != MemoryUtil.NULL) {
            AL10.alSourceStop(musicSource);
            AL10.alDeleteSources(musicSource);
            for (int i = 0; i < FX_SOURCE_COUNT; i++) {
                AL10.alSourceStop(fxSources[i]);
                AL10.alDeleteSources(fxSources[i]);
            }
            deleteBuffers();
            ALC10.alcMakeContextCurrent(MemoryUtil.NULL);
            ALC10.alcDestroyContext(alContext);
            ALC10.alcCloseDevice(alDevice);
            alDevice = MemoryUtil.NULL;
            alContext = MemoryUtil.NULL;
        }
    }

    private static void deleteBuffers() {
        if (hmAudio != null) {
            for (Integer buffer : hmAudio.values()) {
                if (buffer != null) {
                    AL10.alDeleteBuffers(buffer);
                }
            }
        }
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

            deleteBuffers();
            hmAudio.clear();
            hmAudio = null;
            currentMusicKey = null;
            loadALData();
        }
    }
}
