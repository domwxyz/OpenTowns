package xaos.property.wrapper;

import xaos.property.PropertyWrapper;

/**
 *
 * @author Florian Frankenberger
 */
public class IntegerPropertyWrapper implements PropertyWrapper<Integer> {

    public static final IntegerPropertyWrapper INSTANCE = new IntegerPropertyWrapper();

    private IntegerPropertyWrapper() { /* INSTANCE */ }

    @Override
    public Integer wrap(String raw) {
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String unwrap(Integer value) {
        return Integer.toString(value);
    }

}
