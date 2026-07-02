package xaos.property.wrapper;

import xaos.property.PropertyWrapper;

/**
 *
 * @author Florian Frankenberger
 */
public class BooleanPropertyWrapper implements PropertyWrapper<Boolean> {

    public static final BooleanPropertyWrapper INSTANCE = new BooleanPropertyWrapper();

    private BooleanPropertyWrapper() { /* INSTANCE */ }

    @Override
    public Boolean wrap(String raw) {
        try {
            return Boolean.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String unwrap(Boolean value) {
        return Boolean.toString(value);
    }

}
