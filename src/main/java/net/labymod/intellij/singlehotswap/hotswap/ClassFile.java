package net.labymod.intellij.singlehotswap.hotswap;

import com.intellij.debugger.impl.HotSwapFile;
import com.intellij.openapi.compiler.ClassObject;
import com.intellij.openapi.project.Project;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * A class file wrapper that holds all information about a compiled Java class file.
 *
 * @author LabyStudio
 */
public class ClassFile {

    private final Project project;
    private final File file;
    private final String packageName;
    private final String className;
    private final String classPath;

    /**
     * Creates a new class file wrapper.
     *
     * @param project     The project the class file is in.
     * @param file        The file that points to the .class file.
     * @param packageName The package name of the class file. (e.g. "net.labymod.intellij.singlehotswap.hotswap")
     * @param className   The class name of the class file. (e.g. "ClassFile")
     */
    public ClassFile(Project project, File file, String packageName, String className) {
        this.project = project;
        this.file = file;
        this.packageName = packageName;
        this.className = className;
        this.classPath = packageName.isEmpty() ? className : (packageName + "." + className);
    }

    /**
     * Reads the header of the bytecode to determine the Java version in which it was compiled.
     *
     * @return The Java version of the compiled class file. (e.g. 8, 9, 10, 11, 12, 13, 14, 15)
     * @throws IOException If an I/O error occurs.
     */
    public int readJavaVersion() throws IOException {
        try (DataInputStream dataInputStream = new DataInputStream(Files.newInputStream(this.file.toPath()))) {
            byte[] header = new byte[8];
            if (dataInputStream.read(header) == 8) {
                int classVersion = (header[6] & 0xFF) << 8 | header[7] & 0xFF;
                return classVersion - 44;
            }
            return -1;
        }
    }

    public Project getProject() {
        return this.project;
    }

    public File getFile() {
        return this.file;
    }

    /**
     * Returns the package name of the class file.
     * (e.g. "net.labymod.intellij.singlehotswap.hotswap")
     *
     * @return The package name of the class file.
     */
    public String getPackageName() {
        return this.packageName;
    }

    /**
     * Returns the class name of the class file.
     * (e.g. "ClassFile")
     *
     * @return The class name of the class file.
     */
    public String getClassName() {
        return this.className;
    }

    /**
     * Returns the full class name of the class file.
     * (e.g. "net.labymod.intellij.singlehotswap.hotswap.ClassFile")
     *
     * @return The full class name of the class file.
     */
    public String getClassPath() {
        return this.classPath;
    }

    public HotSwapFile toHotSwapFile() {
        return new HotSwapFile(this.file);
    }

    public static ClassFile fromClassObject(Project project, ClassObject classObject) {
        String classPath = classObject.getClassName();
        String packageName = classPath.contains(".") ? classPath.substring(0, classPath.lastIndexOf(".")) : "";
        String className = classPath.substring(packageName.isEmpty() ? 0 : packageName.length() + 1);
        File file = new File(classObject.getPath());
        return new ClassFile(project, file, packageName, className);
    }
}
