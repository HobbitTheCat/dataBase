package Interface;
import NewQuery.Condition;
import Pages.Address;

import java.util.ArrayList;

public interface BackLinkPage<Type> extends DataPage<Type>{
    short add(Type value, Address address);
    short replace(short index, Type newValue, Address newAddress);
    void replaceSamePlace(short index, Type newValue, Address newAddress);
    ArrayList<Address> search(Condition condition);
    int getPageNumber();
    int getNextPage();
    void setNextPage(int page);
}
