package NewQuery.Interfaces;

public interface Update{
    Update set(String attrName, Object value);
    Update object(Object object); //Reflexion
}
