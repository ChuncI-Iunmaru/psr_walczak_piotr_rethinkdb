import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.exc.ReqlError;
import com.rethinkdb.gen.exc.ReqlQueryLogicError;
import com.rethinkdb.model.MapObject;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;

import java.util.HashMap;
import java.util.List;

public class AppMain {
    public static final RethinkDB r = RethinkDB.r;
    private static final String tableName = "criminals";

    private static void insertCriminal(Connection connection) {
//        HashMap<String, Object> run = r.table("criminals").insert(
//                r.hashMap("name", "Jan")
//                .with("surname", "Kowalski")
//                .with("gender", "M")
//                .with("build", "szczupła")
//                .with("height", 183)
//                .with("characteristics", r.array("okulary"))
//                .with("crimes", r.array("rozbój", "podpalenie"))
//                .with("notes", "Niebezpieczny")
//                .with("dob", "12-11-1987")).run(connection);
        MapObject criminal = r.hashMap();
        System.out.println("\nPodaj imię: ");
        criminal.put("name", ConsoleUtils.getText(1));
        System.out.println("Podaj nazwisko: ");
        criminal.put("surname", ConsoleUtils.getText(1));
        criminal.put("gender", ConsoleUtils.pickGender(""));
        criminal.put("build", ConsoleUtils.getBuild(""));
        criminal.put("height", ConsoleUtils.getHeight(0L));
        System.out.println("Podaj cechy szczególne: ");
        criminal.put("characteristics", ConsoleUtils.getListOfTexts(";", 0));
        System.out.println("Podaj przestępstwa: ");
        criminal.put("crimes", ConsoleUtils.getListOfTexts(";", 1));
        System.out.println("Podaj uwagi: ");
        criminal.put("notes", ConsoleUtils.getText(0));
        criminal.put("dob", ConsoleUtils.getFormattedDate(""));

        HashMap<String, Object> run = r.table(tableName).insert(criminal).run(connection);
        System.out.println("Utworzone klucze: " + run.get("generated_keys"));
    }

    private static void getCriminalById(Connection connection) {
        System.out.println("Podaj id profilu przestępcy: ");
        String id = ConsoleUtils.getText(1);
        HashMap<String, Object> result = r.table(tableName).get(id).run(connection);
        if (result != null) {
            ConsoleUtils.printCriminalProfile(result);
        } else System.out.println("Nie znaleziono profilu!");
    }

    private static void getAllCriminals(Connection connection) {
        Cursor<HashMap<String, Object>> cursor = r.table(tableName).run(connection);
        for (HashMap<String, Object> doc : cursor) {
            ConsoleUtils.printCriminalProfile(doc);
        }
    }

    private static void removeCriminal(Connection connection) {
        System.out.println("Podaj id profilu przestępcy: ");
        String id = ConsoleUtils.getText(1);
        HashMap<String, Object> result = r.table(tableName).get(id).delete().run(connection);
        if ((Long)result.get("deleted") == 1) {
            System.out.println("Usunięto profil");
        } else System.out.println("Usuwanie nie powiodło się!");
    }

    private static void getByQuery(Connection connection) {
        System.out.println("Podaj imię - '*' by wyszukiwać wszystkie");
        String name = ConsoleUtils.getText(1);
        System.out.println("Podaj nazwisko - '*' by wyszukiwać wszystkie");
        String surname = ConsoleUtils.getText(1);
        Cursor<HashMap<String, Object>> cursor;
        if (name.compareTo("*") == 0 && surname.compareTo("*") == 0) {
            cursor = r.table(tableName).run(connection);
        } else if (name.compareTo("*") != 0 && surname.compareTo("*") == 0) {
           cursor = r.table(tableName).filter(row -> row.g("name").eq(name)).run(connection);
        } else if (name.compareTo("*") == 0) {
            cursor = r.table(tableName).filter(row -> row.g("surname").eq(surname)).run(connection);
        } else {
            cursor = r.table(tableName).filter(row -> row.g("surname").eq(surname).and(row.g("name").eq(name))).run(connection);
        }
        for (HashMap<String, Object> doc : cursor) {
            ConsoleUtils.printCriminalProfile(doc);
        }
    }

    private static void updateCriminal(Connection connection) {
        System.out.println("Podaj id profilu przestępcy: ");
        String id = ConsoleUtils.getText(1);
        HashMap<String, Object> result = r.table(tableName).get(id).run(connection);
        if (result != null) {
            System.out.println("\nPodaj imię. Obecna wartość '" + result.get("name")+"'. Pozostaw puste by nie zmieniać");
            String newName = ConsoleUtils.getText(0);
            if (!newName.isEmpty()) {
                r.table(tableName).get(id).update(r.hashMap("name", newName)).run(connection);
            }

            System.out.println("\nPodaj nazwisko. Obecna wartość '" + result.get("surname")+"'. Pozostaw puste by nie zmieniać");
            String newSurname = ConsoleUtils.getText(0);
            if (!newSurname.isEmpty()) {
                r.table(tableName).get(id).update(r.hashMap("surname", newName)).run(connection);
            }

            String newGender = ConsoleUtils.pickGender(result.get("gender").toString());
            if (newGender.compareTo(result.get("gender").toString()) != 0) {
                r.table(tableName).get(id).update(r.hashMap("gender", newGender)).run(connection);
            }
            String newBuild = ConsoleUtils.getBuild(result.get("build").toString());
            if (newBuild.compareTo(result.get("build").toString()) != 0) {
                r.table(tableName).get(id).update(r.hashMap("build", newBuild)).run(connection);
            }
            Long newHeight = ConsoleUtils.getHeight((Long)result.get("height"));
            if (!newHeight.equals(result.get("height"))) {
                r.table(tableName).get(id).update(r.hashMap("height", newHeight)).run(connection);
            }

            System.out.println("Podaj cechy szczególne. Pozostaw puste by nie zmieniać.");
            List<String> newChars = ConsoleUtils.getListOfTexts(";", 0);
            // Są faktycznie zmiany
            if (!(newChars.size() == 1 && newChars.get(0).isEmpty())) {
                r.table(tableName).get(id).update(r.hashMap("characteristics", newChars)).run(connection);
            }

            System.out.println("Podaj przestępstwa. Pozostaw puste by nie zmieniać.");
            List<String> newCrimes = ConsoleUtils.getListOfTexts(";", 0);
            if (!(newCrimes.size() == 1 && newCrimes.get(0).isEmpty())) {
                r.table(tableName).get(id).update(r.hashMap("crimes", newCrimes)).run(connection);
            }

            System.out.println("\nPodaj uwagi. Obecna wartość '" + result.get("notes")+"'. Pozostaw puste by nie zmieniać");
            String newNotes = ConsoleUtils.getText(0);
            if (!newNotes.isEmpty()) {
                r.table(tableName).get(id).update(r.hashMap("notes", newNotes)).run(connection);
            }

            String newDob = ConsoleUtils.getFormattedDate(result.get("dob").toString());
            if (newDob.compareTo(result.get("dob").toString()) != 0) {
                r.table(tableName).get(id).update(r.hashMap("dob", newDob)).run(connection);
            }
            System.out.println("Zakończono aktualizację");

        } else System.out.println("Nie znaleziono profilu!");
    }

    public static void main(String[] args) {
        Connection conn = r.connection().hostname("localhost").port(28015).connect();
        // Nie twórz od nowa bazy i tablicy
        //System.out.println(r.dbCreate("policyjnaDB").run(conn).toString());;
        //System.out.println(r.db("policyjnaDB").tableCreate("criminals").run(conn).toString());
        conn.use("policyjnaDB");

        while (true) {
            switch (ConsoleUtils.getMenuOption()) {
                case 'd':
                    insertCriminal(conn);
                    break;
                case 'u':
                    removeCriminal(conn);
                    break;
                case 'a':
                    updateCriminal(conn);
                    break;
                case 'i':
                    getCriminalById(conn);
                    break;
                case 'w':
                    getAllCriminals(conn);
                    break;
                case 'n':
                    getByQuery(conn);
                    break;
                case 'o':
                    break;
                case 'z':
                    return;
                default:
                    System.out.println("Podano nieznaną operację. Spróbuj ponownie.");
            }
        }
    }
}
