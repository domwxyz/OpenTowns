package xaos.property;

/**
 * A setting in the towns.ini file with it's appropriate typing information.
 *
 * @author Florian Frankenberger
 */
public class Property<T> {

    private final PropertyFile propertyFile;
    private final String key;
    private final PropertyWrapper<T> propertyWrapper;

    public Property(PropertyFile propertyFile, String key, PropertyWrapper<T> propertyWrapper) {
        this.propertyFile = propertyFile;
        this.key = key;
        this.propertyWrapper = propertyWrapper;
    }

    public PropertyFile getPropertyFile() {
        return propertyFile;
    }

    public String getKey() {
        return key;
    }

    public PropertyWrapper<T> getPropertyWrapper() {
        return propertyWrapper;
    }

}
