import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class LogMerger {

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Usage: LogComparer <<classloader-output-1> <classloader-output-2> <classloader-output-merged>");
            return;
        }

        Map<String, List<String>> classesBySourceA = AppMinifier.getLoadedClasses(args[0]);
        Map<String, List<String>> classesBySourceB = AppMinifier.getLoadedClasses(args[1]);

        FileWriter mergedWriter = new FileWriter(args[2]);

        String rootA = LogComparer.findAppRoot(classesBySourceA);
        String rootB = LogComparer.findAppRoot(classesBySourceB);

        int counterA = 0, counterB = 0, common = 0, fromAOnly = 0, fromBOnly = 0;

        for (String source : classesBySourceA.keySet()) {
            if (source.startsWith("jar:file:")) {
                List<String> classesA = classesBySourceA.get(source);
                counterA += classesA.size();
                String sourceB = source.replace(rootA, rootB);
                List<String> classesB = classesBySourceB.get(sourceB);
                counterB += classesA.size();

                classesB.removeAll(classesA);
                fromBOnly += classesB.size();
                classesA.addAll(classesB);

                for (String cls : classesA) {
                    writeClassLoading(mergedWriter, source, cls);
                }

                classesBySourceB.remove(sourceB);
            }
        }

        for (String source : classesBySourceB.keySet()) {
            if (source.startsWith("jar:file:")) {
                String sourceA = source.replace(rootB, rootA);
                List<String> classesA = classesBySourceA.get(sourceA);
                if (classesA != null) throw new RuntimeException();

                for (String cls : classesA) {
                    writeClassLoading(mergedWriter, sourceA, cls);
                }

            }
        }

        mergedWriter.flush();
        mergedWriter.close();
    }

    private static void writeClassLoading(FileWriter mergedWriter, String source, String cls) throws IOException {
        // [203.935s][info][class,load] org.netbeans.core.NbLifeExit source: jar:file:/C:/tools/visualvm_217/platform/modules/org-netbeans-core.jar!/
        mergedWriter.write("[class,load] ");
        mergedWriter.write(cls);
        mergedWriter.write(" source: ");
        mergedWriter.write(source);
        mergedWriter.write('\n');
    }

}
