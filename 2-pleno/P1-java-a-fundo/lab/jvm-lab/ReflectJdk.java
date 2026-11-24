import java.lang.reflect.Field;

/** P1.12 — encapsulamento forte do JDK: java ReflectJdk.java [--add-opens java.base/java.lang=ALL-UNNAMED] */
public class ReflectJdk {
    public static void main(String[] args) throws Exception {
        Field f = String.class.getDeclaredField("value");
        try { f.setAccessible(true); System.out.println("acessou String.value: " + ((byte[]) f.get("abc")).length + " bytes"); }
        catch (RuntimeException e) { System.out.println(e.getClass().getName() + ": " + e.getMessage()); }
    }
}
