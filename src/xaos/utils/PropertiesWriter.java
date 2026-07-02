
package xaos.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import xaos.property.Property;
import xaos.property.PropertyFile;

/**
 *
 * @author Florian Frankenberger
 */
public class PropertiesWriter {

    private final PropertyFile file;
    private final Map<EntryKey, String> map = new LinkedHashMap<EntryKey, String>();

    public PropertiesWriter(PropertyFile file) {
        this.file = file;
    }

    public <T> void setProperty(Property<T> property, T value) {
        if (property.getPropertyFile() != file) {
            throw new IllegalArgumentException("property " + property.getKey() + " is not part of " + file);
        }
        map.put(new PropertiesEntryKey(property.getKey()), property.getPropertyWrapper().unwrap(value));
    }

    public void setProperty(String key, String value) {
        map.put(new PropertiesEntryKey(key), value);
    }

    public void addSection(String sectionHeading) {
        map.put(new SectionEntryKey(sectionHeading), null);
    }

    public void store(File file) throws IOException {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("ISO8859-1")));
            int counter = 0;
            for (Entry<EntryKey, String> entry : map.entrySet()) {
                EntryKey entryKey = entry.getKey();
                if (entryKey instanceof SectionEntryKey) {
                    SectionEntryKey sectionEntryKey = (SectionEntryKey) entryKey;
                    if (counter > 0) {
                        pw.append("\n");
                    }
                    pw.append("# ").append(sectionEntryKey.getSectionHeading()).append("\n");
                } else
                    if (entryKey instanceof PropertiesEntryKey) {
                        PropertiesEntryKey propertiesEntryKey = (PropertiesEntryKey) entryKey;
                        pw.append(propertiesEntryKey.getKey()).append(" = ").append(entry.getValue()).append("\n");
                    }
                counter++;
            }
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    private static interface EntryKey {

    }

    private static class PropertiesEntryKey implements EntryKey {
        private final String key;

        public PropertiesEntryKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    private static class SectionEntryKey implements EntryKey {
        private final String sectionHeading;

        public SectionEntryKey(String sectionHeading) {
            this.sectionHeading = sectionHeading;
        }

        public String getSectionHeading() {
            return sectionHeading;
        }
    }


}
