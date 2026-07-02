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

    private static final File ROOT_DIR = new File("./");

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
