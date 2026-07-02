package xaos.property.wrapper;

import xaos.property.PropertyWrapper;

/**
 *
 * @author Florian Frankenberger
 */
public class StringPropertyWrapper implements PropertyWrapper<String> {

    public static final StringPropertyWrapper INSTANCE = new StringPropertyWrapper();

    private StringPropertyWrapper() { /* singleton */ }

    @Override
    public String wrap(String raw) {
        return raw;
    }

    @Override
    public String unwrap(String value) {
        return value;
    }

}
