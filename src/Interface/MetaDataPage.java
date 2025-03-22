package Interface;

import Pages.DataClass;

public interface MetaDataPage{
    DataClass getClassByName(String className);
    DataClass getClassByOffset(short offset);
    short add(DataClass dataClass);
}
