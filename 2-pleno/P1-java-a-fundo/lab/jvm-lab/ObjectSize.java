public class ObjectSize { public static void main(String[] a) throws Exception {
  Runtime rt = Runtime.getRuntime(); Object[] keep = new Object[2_000_000];
  System.gc(); long b = rt.totalMemory() - rt.freeMemory();
  for (int i = 0; i < keep.length; i++) keep[i] = new Object();
  System.gc(); long e = rt.totalMemory() - rt.freeMemory();
  System.out.printf("bytes por new Object() ~ %.1f%n", (e - b) / (double) keep.length - 4.0); } }
