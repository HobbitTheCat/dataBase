package Pages.Interface;

/**
 * Name of class: DataPage
 * <p>
 * Description: Represents More general than BackLinkPage, used by object page.
 * <p>
 * Version: 1.0
 * <p>
 * Date 03/17
 * <p>
 * Copyright: Semenov Egor
 */

public interface DataPage<Type>{
    Type get(short index);
    short size();
    void delete(short index);
    short getType();
    String toString();
    int getPageNumber();
    int getNextPage();
    void setNextPage(int page);
}
