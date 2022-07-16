package net.labymod.intellij.singlehotswap.compiler;

import com.intellij.openapi.vfs.VirtualFile;
import net.labymod.intellij.singlehotswap.hotswap.ClassFile;
import net.labymod.intellij.singlehotswap.hotswap.Context;

import java.util.List;

/**
 * Abstract compiler class that can be used to implement a custom compiler.
 *
 * @author LabyStudio
 */
public abstract class AbstractCompiler {

    protected final Context context;

    public AbstractCompiler(Context context) {
        this.context = context;
    }

    /**
     * This method compiles the given source file and writes the byte code to the given output file.
     * This includes all inner classes of the source file.
     * <p>
     * The output file is overwritten if it already exists.
     *
     * @param sourceFile The source file to compile.
     * @param outputFile The output file to write the byte code to.
     * @return A list of class files that were compiled. (More than one class file can be compiled if the source file contains inner classes.)
     * @throws Exception If an error occurs while compiling the source file.
     */
    public abstract List<ClassFile> compile(VirtualFile sourceFile, ClassFile outputFile) throws Exception;
}
