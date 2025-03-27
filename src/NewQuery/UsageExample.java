package NewQuery;

public class UsageExample {
    public static void main(String[] args) {
        Hero hero = new Hero("Pedro Parqueador", "Spider-Boy", 19);

        Session session = new Session();
        Transaction transaction = session.createNewTransaction();
        transaction.add(Query.select(Hero.class).where("name", "==", "Tommy Sharp").where("age", ">", 35));
        transaction.add(Query.create(Hero.class).column("name", "secretName", "age").values(hero.name, hero.secretName,  hero.age));
        Result[] results = session.execute(transaction);
    }
}

class Hero{
    String name;
    String secretName;
    Integer age;

    public Hero(String name, String secretName, int age) {
        this.name = name;
        this.secretName = secretName;
        this.age = age;
    }
}
