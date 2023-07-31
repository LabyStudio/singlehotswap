package net.labymod.intellij.singlehotswap.hotswap;

import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.impl.HotSwapProgress;
import com.intellij.psi.PsiFile;
import net.labymod.intellij.singlehotswap.compiler.AbstractCompiler;
import net.labymod.intellij.singlehotswap.storage.SingleHotswapConfiguration;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * Context for handling the hotswap environment for a specific file type
 *
 * @author LabyStudio
 */
public interface Context {

    /**
     * Returns the compiler for this context
     *
     * @param configuration The configuration to use to modify the compiler settings
     * @param forceDefault  Whether to force the default compiler
     * @return The compiler for this context
     */
    AbstractCompiler compiler(SingleHotswapConfiguration configuration, boolean forceDefault);

    /**
     * Returns the class file for the given PSI file
     *
     * @param psiFile The PSI file to get the class file for
     * @return The class file for the given PSI file
     */
    ClassFile getClassFile(PsiFile psiFile) throws FileNotFoundException;

    /**
     * Returns a list of the inner classes of the given class file
     *
     * @param classFile The class file to get the inner classes for
     * @return A list of the inner classes of the given class file. The list is empty if the class file does not contain inner classes.
     */
    List<ClassFile> getInnerClassFiles(ClassFile classFile);

    /**
     * Hotswap the given class files in the given debugger session
     *
     * @param debuggerSession The debugger session to hotswap the class files in
     * @param progress        The progress object to report the hotswap progress to
     * @param classFiles      The class files to hotswap
     * @return Returns true if the hotswap was successful, false otherwise
     */
    boolean hotswap(DebuggerSession debuggerSession, HotSwapProgress progress, List<ClassFile> classFiles);


    /**
     * Is given file instance hot-swappable
     *
     * @param file Psi file to hotswap
     * @return Return true if file instance is hot-swappable
     */
    boolean isPossible(PsiFile file);


    /**
     * Get the pretty name of the given file
     *
     * @param file PSI file to get the name
     * @return Pretty name
     */
    String getName(PsiFile file);
}
