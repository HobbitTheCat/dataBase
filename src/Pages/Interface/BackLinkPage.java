package Pages.Interface;
import NewQuery.Condition;
import Pages.Address;

import java.util.ArrayList;

/**
 * Name of class: BackLinkPage
 * <p>
 * Description: Represents pages where we can pu attribute values directly
 * <p>
 * Version: 1.0
 * <p>
 * Date 03/17
 * <p>
 * Copyright: Semenov Egor
 */

public interface BackLinkPage<Type> extends DataPage<Type>{
    short add(Type value, Address address);
    short replace(short index, Type newValue, Address newAddress);
    void replaceSamePlace(short index, Type newValue);
    ArrayList<Address> search(Condition condition);

}
