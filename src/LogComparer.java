import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LogComparer {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: LogComparer <<classloader-output-1> <classloader-output-2>");
            return;
        }

        Map<String, List<String>> classesBySourceA = AppMinifier.getLoadedClasses(args[0]);
        Map<String, List<String>> classesBySourceB = AppMinifier.getLoadedClasses(args[1]);

        String rootA = findAppRoot(classesBySourceA);
        String rootB = findAppRoot(classesBySourceB);

        System.out.println(rootA);
        System.out.println(rootB);

        for (String source : classesBySourceA.keySet()) {
            if (source.startsWith("jar:file:")) {
                List<String> classesA = classesBySourceA.get(source);
                String sourceB = source.replace(rootA, rootB);
                List<String> classesB = classesBySourceB.get(sourceB);
                if (classesB == null) {
                    System.out.println("Jar file " + source + " is absent in second log");
                    continue;
                }
                List<String> inANotInB = new ArrayList<>(classesA);
                inANotInB.removeAll(classesB);
                if (inANotInB.size() > 0) {
                    System.out.println("Jar file " + source + " has following classes in first log which are not in second log:" + inANotInB.toString());
                }
                List<String> inBNotInA = new ArrayList<>(classesB);
                inBNotInA.removeAll(classesA);
                if (inBNotInA.size() > 0) {
                    System.out.println("Jar file " + source + " has following classes in second log which are not in first log:" + inBNotInA.toString());
                }
                classesBySourceB.remove(sourceB);
            }
        }

        for (String source : classesBySourceB.keySet()) {
            if (source.startsWith("jar:file:")) {
                String sourceA = source.replace(rootB, rootA);
                List<String> classesA = classesBySourceA.get(sourceA);
                if (classesA != null) throw new RuntimeException();
                System.out.println("Jar file " + source + " is absent in first log");
            }
        }
    }

    static String findAppRoot(Map<String, List<String>> classesBySource) {
        String root = null;
        for (String source : classesBySource.keySet()) {
            if (source.startsWith("jar:file:")) {
                if (root == null) {
                    root = source;
                    continue;
                } else {
                    for (int i = 0; (i < root.length()) && (i < source.length()); i++) {
                        if (root.charAt(i) != source.charAt(i)) {
                            root = root.substring(0, i);
                        }
                    }
                }
            }
        }
        return root;
    }
}
