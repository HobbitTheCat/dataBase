import NewQuery.Query;
import NewQuery.Result;
import NewQuery.Session;
import NewQuery.Transaction;

import java.util.List;

public class UsageExample {
    public static final String host =  "localhost";
    public static final int port = 8080;


    public static void main(String[] args) {
        Hero hero = new Hero("Pedro Parqueador", "Spider-Boy", 19);
        try(Session session = new Session(host, port)) {
            Transaction transaction = session.createNewTransaction();
            transaction.add(Query.create(Hero.class));
            transaction.add(Query.select(Hero.class).where("name", "==", "Tommy Sharp").where("age", ">", 35).all());
            transaction.add(Query.create(Hero.class).object(hero));
//            transaction.add(Query.update(Hero.class).where("name", "contains", "Pedro").set("name", "Pedro Ivanov"));
            transaction.add(Query.delete(Hero.class).where("name", "=", "tommy sharp"));
            System.out.println(transaction);
            System.out.println(transaction.getQueries()[0].getAttributeValues().isEmpty());
            List<Result> results = session.execute(transaction);
            System.out.println(results);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void connectionTest(){
        try (Session session = new Session(host, port)) {
            Transaction transaction = new Transaction();
            transaction.add(Query.select(Hero.class).where("name", "==", "pedro"));

            List<Result> results = session.execute(transaction);
            System.out.println("Number of results obtained: " + results.size());

        } catch (Exception e) {
            System.err.println("Runtime error" + e.getMessage());
        }
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

class Pen{
    String color;
    int price;
}
