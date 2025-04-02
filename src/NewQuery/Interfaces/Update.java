package NewQuery.Interfaces;

public interface Update{
    Update attribute(String attrName, Object value);
    Update object(Object object); //Reflexion
}
