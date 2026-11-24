// Arquivo-fonte compacto (Java 25, JEP 512, final): sem classe, sem `public static void main(String[])`.
String greeting() { return "PayFlow no Java " + Runtime.version().feature(); }

void main() {
    IO.println(greeting());                  // java.lang.IO (Java 25): println/readln sem System.out
}
