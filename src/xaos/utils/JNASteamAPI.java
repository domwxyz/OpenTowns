package xaos.utils;

import com.sun.jna.Library;

public interface JNASteamAPI extends Library {

    boolean SteamAPI_Init();
}
