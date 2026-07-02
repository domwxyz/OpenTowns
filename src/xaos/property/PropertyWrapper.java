package xaos.property;

/**
 *
 * @author Florian Frankenberger
 */
public interface PropertyWrapper<T> {

    T wrap(String raw);

    String unwrap(T value);

}
