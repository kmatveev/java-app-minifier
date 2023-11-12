import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

public class AppMinifier {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: AppMinifier <app-dir> <classloader-output>");
            return;
        }

        Map<String, List<String>> classesBySource = getLoadedClasses(args[1]);
        filterJars(classesBySource);

        System.out.println("Done");
    }

    private static void filterJars(Map<String, List<String>> classesBySource) throws IOException {
        for (String source : classesBySource.keySet()) {
            if (source.startsWith("jar:file:")) {
                // "jar:file:/C:/tools/visualvm_217/visualvm/modules/org-graalvm-visualvm-modules-appui.jar!/"

                URL jarUrl = new URL(source);
                JarURLConnection connection = (JarURLConnection) jarUrl.openConnection();
                String srcFileName = connection.getJarFileURL().getFile();
                JarInputStream srcJarFile = new JarInputStream(new BufferedInputStream(new FileInputStream(srcFileName)));

                String destFileName = srcFileName.replace(".jar", ".jaf");
                JarOutputStream destJarFile = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(destFileName)));

                List<String> usedEntries = classesBySource.get(source);
                List<String> processedEntries = new ArrayList<>();

                while (true ) {
                    JarEntry entry = srcJarFile.getNextJarEntry();
                    if (entry == null) break;
                    if (!entry.isDirectory()) {
                        if (entry.getName().endsWith(".class")) {
                            String name = entry.getName();
                            String className = name.replace('/', '.').substring(0, name.length() - ".class".length());
                            if (!usedEntries.contains(className)) {
                                continue;
                            } else {
                                processedEntries.add(className);
                            }
                        }

                        // we copy all non-class files, and only specified class file
                        destJarFile.putNextEntry(entry);
                        byte[] buffer = new byte[4096];
                        while (srcJarFile.available() > 0) {
                            int readL = srcJarFile.read(buffer, 0, buffer.length);
                            if (readL >= 0) {
                                destJarFile.write(buffer, 0, readL);
                            }
                        }
                        destJarFile.closeEntry();

                    } else {
                        destJarFile.putNextEntry(entry);
                    }
                }

                usedEntries.removeAll(processedEntries);
                if (usedEntries.size() > 0) {
                    System.out.println("Some classes were not found");
                }

                srcJarFile.close();
                destJarFile.finish();
                destJarFile.close();

            }

        }
    }

    static Map<String, List<String>> getLoadedClasses(String classLoaderLog) throws IOException {
        LineNumberReader r = new LineNumberReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(classLoaderLog))));
        Map<String, List<String>> classesBySource = new HashMap<>();
        while (true) {
            String line = r.readLine();
            if (line == null) break;

            // [203.935s][info][class,load] org.netbeans.core.NbLifeExit source: jar:file:/C:/tools/visualvm_217/platform/modules/org-netbeans-core.jar!/

            int i1 = line.indexOf("[class,load]");
            if (i1 < 0) continue;
            if (line.indexOf("opened:", i1) >= 0) continue;
            int i2 = line.indexOf("source:", i1);
            String className = line.substring(i1 + "[class,load]".length(), i2).trim();
            String classLocation = line.substring(i2 + "source:".length()).trim();
            List<String> classes = classesBySource.computeIfAbsent(classLocation, k -> { return new ArrayList<String>(); });
            classes.add(className);

        }
        return classesBySource;
    }
}
