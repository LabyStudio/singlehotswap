package net.labymod.intellij.singlehotswap.compiler.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import net.labymod.intellij.singlehotswap.compiler.AbstractCompiler;
import net.labymod.intellij.singlehotswap.hotswap.ClassFile;
import net.labymod.intellij.singlehotswap.hotswap.Context;
import net.labymod.intellij.singlehotswap.storage.SingleHotswapConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

/**
 * Kotlin compiler implementation that compiles Kotlin files directly using the kotlinc command line tool.
 *
 * @author LabyStudio
 */
public class CustomKotlinCompiler extends AbstractCompiler {

    private final SingleHotswapConfiguration configuration = ApplicationManager.getApplication().getService(SingleHotswapConfiguration.class);

    public CustomKotlinCompiler(Context context) {
        super(context);
    }

    @Override
    public List<ClassFile> compile(Module module, VirtualFile sourceFile, ClassFile outputFile) throws Exception {
        List<String> kotlinArgs = new ArrayList<>();

        // Collect dependencies for classpath
        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        StringJoiner joiner = new StringJoiner(File.pathSeparator);
        for (String path : rootManager.orderEntries().recursively().getPathsList().getPathList()) {
            if (path.contains("!")) {
                continue;
            }
            joiner.add(new File(path).getAbsolutePath());
        }
        if (joiner.length() > 0) {
            kotlinArgs.add("-classpath");
            kotlinArgs.add(joiner.toString());
        }

        // Derive JVM target from existing class file
        String jvmTarget = this.mapClassVersionToJvmTarget(outputFile.readJavaVersion());
        if (!jvmTarget.isEmpty()) {
            kotlinArgs.add("-jvm-target");
            kotlinArgs.add(jvmTarget);
        }

        // Output directory
        File file = VfsUtil.virtualToIoFile(sourceFile);
        File outputDirectory = new File(CompilerPaths.getOutputPaths(new Module[]{module})[0]);
        kotlinArgs.add("-d");
        kotlinArgs.add(outputDirectory.getAbsolutePath());
        kotlinArgs.add(file.getAbsolutePath());

        // Compile
        long startTime = System.currentTimeMillis();
        int exitCode = this.runKotlinc(kotlinArgs);
        if (exitCode != 0) {
            throw new IOException("Kotlin compilation failed with exit code: " + exitCode);
        }

        // Collect generated class files (TODO: Find a way to get exact output files from kotlinc)
        List<ClassFile> classFiles = new ArrayList<>();
        for (File classFile : this.findClassFilesNewerThan(outputDirectory.toPath(), startTime)) {
            byte[] bytes = Files.readAllBytes(classFile.toPath());
            FileUtil.writeToFile(classFile, bytes);

            // Derive package and class name from file path relative to output dir
            classFiles.add(this.toClassFile(outputFile.getProject(), classFile, outputDirectory));
        }

        return classFiles;
    }

    /**
     * Runs the Kotlin compiler (kotlinc) with the specified arguments.
     *
     * @param args the arguments to pass to kotlinc
     * @return the exit code of the compiler process
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the process is interrupted
     */
    private int runKotlinc(List<String> args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(this.getKotlincExecutable());
        cmd.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true); // merge stdout & stderr
        Process process = pb.start();

        // Print compiler output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Kotlin CLI compilation failed with exit code " + exitCode);
        }
        return exitCode;
    }

    /**
     * Converts a class file on disk to a ClassFile object by deriving package and class name
     * from its path relative to the output directory.
     *
     * @param project         the IntelliJ project
     * @param classFile       the .class file on disk
     * @param outputDirectory the root output directory where class files are written
     * @return the ClassFile representation
     */
    @NotNull
    private ClassFile toClassFile(Project project, File classFile, File outputDirectory) {
        String relativePath = outputDirectory.toPath().relativize(classFile.toPath()).toString();
        String classPath = relativePath.replace(File.separatorChar, '.').replaceAll("\\.class$", "");
        String packageName = classPath.contains(".") ? classPath.substring(0, classPath.lastIndexOf('.')) : "";
        String className = classPath.substring(packageName.isEmpty() ? 0 : packageName.length() + 1);

        return new ClassFile(project, classFile, packageName, className);
    }

    /**
     * Maps a Java class file version to a Kotlin JVM target string.
     * E.g., class version 52 (Java 8) maps to "1.8", version 53 to "9", etc.
     *
     * @param classVersion the class file version number
     * @return the corresponding JVM target string
     */
    private String mapClassVersionToJvmTarget(int classVersion) {
        // Java class file versions for 1.8 -> 52, Java 9 -> 53, etc.
        int jvmTarget;
        if (classVersion <= 52) {
            // Map 45–52 to 1.1–1.8
            jvmTarget = Math.max(8, classVersion - 44); // Clamp minimum to 8
            return "1." + jvmTarget;
        } else {
            // Java 9+
            jvmTarget = classVersion - 44;

            // Clamp to maximum supported Kotlin target (23)
            if (jvmTarget > 23) {
                jvmTarget = 23;
            }
            return String.valueOf(jvmTarget);
        }
    }

    /**
     * Returns the path to the Kotlin compiler executable.
     * First tries the IDEA plugin folder, falls back to "kotlinc" on system PATH.
     */
    private String getKotlincExecutable() {
        File kotlinPlugin = this.getKotlinPluginFolder();
        if (kotlinPlugin != null) {
            File binDirectory = new File(kotlinPlugin, "kotlinc/bin");
            String executableName = this.isWindows() ? "kotlinc.bat" : "kotlinc";

            File file = new File(binDirectory, executableName);
            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return this.configuration.getKotlinCompilerPath();
    }

    /**
     * Returns the Kotlin plugin folder inside the current IDEA instance,
     * or null if it doesn't exist.
     */
    private File getKotlinPluginFolder() {
        File pluginsDir = new File(PathManager.getPluginsPath());
        File kotlinPlugin = new File(pluginsDir, "Kotlin");
        if (kotlinPlugin.exists() && kotlinPlugin.isDirectory()) {
            return kotlinPlugin;
        }
        return null;
    }

    /**
     * Returns true if the current OS is Windows.
     *
     * @return true if Windows, false otherwise
     */
    private boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    @Deprecated
    private Collection<File> findClassFilesNewerThan(Path root, long sinceMillis) throws IOException {
        List<File> result = new ArrayList<>();
        if (!Files.exists(root)) return result;

        Files.walk(root).filter(p -> p.toString().endsWith(".class")).forEach(p -> {
            try {
                FileTime ft = Files.getLastModifiedTime(p);
                if (ft.toMillis() >= sinceMillis - 2000) {
                    result.add(p.toFile());
                }
            } catch (IOException ignored) {
            }
        });
        return result;
    }

}