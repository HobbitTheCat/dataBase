package Pages.Interface;

import TableManager.TableDescription;

/**
 * Name of class: MetaDataPage
 * <p>
 * Description: Interface for meta pages
 * <p>
 * Version: 1.0
 * <p>
 * Date 03/17
 * <p>
 * Copyright: Semenov Egor
 */

public interface MetaDataPage{
    TableDescription getClassByName(String className);
    TableDescription getClassByOffset(short offset);
    short add(TableDescription dataClass);
}
