package Interface;
import Pages.Address;

public interface BackLinkPage<Type> extends DataPage<Type>{
    short add(Type value, Address address);
    short replace(short index, Type newValue, Address newAddress);
    void replaceSamePlace(short index, Type newValue, Address newAddress);
}
