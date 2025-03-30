package NewQuery;

import Exceptions.ORMUsageException;

import java.util.Map;

public class CreateQuery extends Query implements Update{
    public CreateQuery(String className, Map<String, String>  fieldTypes) {
        super.setClassName(className);
        super.setFieldTypes(fieldTypes);
    }

    @Override
    public Update column(String... columnNames) {
        super.addAttributeNames(columnNames);
        return this;
    }

    @Override
    public Update values(Object... values) {
        if(super.getAttributeNames().isEmpty()){
            throw new ORMUsageException("You must specify at least one column name");
        }
//        if(super.getAttributeNames().size() != values.length  + super.getAttributeValues().size()){
//            throw new ORMUsageException("The number of columns must be equal to number of values");
//        }
        return this;
    }

    @Override
    public Update object(Object object) {
        return this;
    }
}
