package Interface;

import TableManager.TableDescription;

public interface MetaDataPage{
    TableDescription getClassByName(String className);
    TableDescription getClassByOffset(short offset);
    short add(TableDescription dataClass);
}
