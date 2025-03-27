package NewQuery;

public class CreateQuery extends Query implements Update{
    @Override
    public Update column(String... columnNames) {
        return this;
    }

    @Override
    public Update values(Object... values) {
        return this;
    }

    @Override
    public Update object(Object object) {
        return this;
    }
}
