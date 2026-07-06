
package xaos;

/**
 * Game properties that are getting filtered by maven
 * to create the right versions.
 *
 * @author Florian Frankenberger
 */
public class TownsProperties {

    public final static boolean GODS_ACTIVATED = false;
    public final static boolean DEBUG_MODE = false; //${debugMode}; //should be false in production
    public final static boolean DEMO_VERSION = false; //${demoVersion};
    public final static boolean TEST_COMMANDS = false; //${testCommands}; //should be false in production
    public final static String GAME_NAME = "Towns"; //$NON-NLS-1$
    public final static String GAME_VERSION = "v14f";
    public final static String GAME_VERSION_FULL = GAME_VERSION; //"${project.version}.${buildNumber} (" + "${buildRevision}".substring(0, 7) + ")"; //$NON-NLS-1$
    public final static String GAME_VERSION_SHORT = GAME_VERSION; //"${project.version}";
//    public final static String NAMED_VERSION_FOR = "${namedVersionFor}";
//
//    static {
//        String[] parts = GAME_VERSION_SHORT.split("\\.");
//        if (parts.length != 3) {
//            GAME_VERSION = "vUnknown";
//        } else {
//            if (!NAMED_VERSION_FOR.isEmpty()) {
//                GAME_VERSION = "v" + parts[1] + (char)('a' + Integer.valueOf(parts[2]) - 1) + " (" + NAMED_VERSION_FOR + ")";
//            } else {
//                GAME_VERSION = "v" + parts[1] + (char)('a' + Integer.valueOf(parts[2]) - 1);
//            }
//        }
//    }

}
