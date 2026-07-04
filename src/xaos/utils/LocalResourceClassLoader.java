package xaos.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A class loader that also searches for resources in the ../data folder (so we
 * don't need the additional "../" classpath hack)
 *
 * @author Florian Frankenberger
 */
public class LocalResourceClassLoader extends ClassLoader {

    // The i18n message bundles live under data/languages/ and are looked up by
    // this loader as data/languages/messages*.properties resources. Root that
    // lookup at the app home (towns.home, set by the packaged launcher to
    // $APPDIR) so it works from a packaged build, where the process working
    // directory is not the app dir. Empty home keeps the original "./"
    // (CWD-relative) behavior for gradlew run / headless / tests. Read from the
    // system property directly to avoid coupling this loader to Towns, which is
    // constructed very early via Messages.<clinit>.
    private static final File ROOT_DIR = rootDir();

    private static File rootDir() {
        String home = System.getProperty("towns.home", "").trim();
        return home.isEmpty() ? new File("./") : new File(home);
    }

    @Override
    protected URL findResource(String name) {
        File resource = new File(ROOT_DIR, name);
        if (resource.exists()) {
            try {
                return resource.toURI().toURL();
            } catch (MalformedURLException ex) {
                Log.log(Log.LEVEL_ERROR, "can't convert resource location to URL", "Towns");
            }
        }
        return null;
    }

}
