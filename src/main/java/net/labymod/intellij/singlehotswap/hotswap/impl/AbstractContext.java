package net.labymod.intellij.singlehotswap.hotswap.impl;

import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.impl.HotSwapFile;
import com.intellij.debugger.impl.HotSwapManager;
import com.intellij.debugger.impl.HotSwapProgress;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import net.labymod.intellij.singlehotswap.compiler.AbstractCompiler;
import net.labymod.intellij.singlehotswap.compiler.impl.DefaultCompiler;
import net.labymod.intellij.singlehotswap.hotswap.ClassFile;
import net.labymod.intellij.singlehotswap.hotswap.Context;
import net.labymod.intellij.singlehotswap.storage.SingleHotswapConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hotswap implementation for PSI files
 *
 * @param <T> File type class to get the package name
 * @author LabyStudio
 */
public abstract class AbstractContext<T> implements Context {

    @Override
    public AbstractCompiler compiler(SingleHotswapConfiguration configuration, boolean forceDefault) {
        return new DefaultCompiler(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ClassFile getClassFile(PsiFile psiFile) throws FileNotFoundException {
        // Get name, file name and class name
        Project project = psiFile.getProject();
        String fileName = psiFile.getName();
        String className = fileName.substring(0, fileName.toLowerCase().lastIndexOf("." + this.getExtensionName()));
        String packageName = this.getPackageName((T) psiFile);

        // Create the full class name
        String classPath = packageName.isEmpty() ? className : (packageName + "." + className);

        // Find the class name in the output paths of the project
        String[] outputPaths = CompilerPaths.getOutputPaths(ModuleManager.getInstance(project).getModules());
        for (String outputPath : outputPaths) {

            // Create file instance using the output path and the class name
            File file = new File(String.format("%s/%s.class", outputPath, classPath.replace('.', '/')));
            if (!file.exists()) {
                continue;
            }

            // Create class file instance
            return new ClassFile(project, file, packageName, className);
        }

        // Could not find class file in output path
        throw new FileNotFoundException("Could not find class file in output path. Are you using the wrong compiler?");
    }

    @Override
    public List<ClassFile> getInnerClassFiles(ClassFile classFile) {
        List<ClassFile> innerClasses = new ArrayList<>();

        // List all compiled files in this directory to find inner classes
        File file = classFile.getFile();
        File[] filesInPackage = file.getParentFile() == null ? null : file.getParentFile().listFiles();
        if (filesInPackage == null) {
            return innerClasses;
        }

        // Iterate inner classes
        for (File fileInPackage : filesInPackage) {
            String innerFullClassName = fileInPackage.getName();

            // Check if it's a static kotlin file
            if (innerFullClassName.equals(classFile.getClassName() + "Kt.class")) {
                innerClasses.add(new ClassFile(
                        classFile.getProject(),
                        fileInPackage,
                        classFile.getPackageName(),
                        classFile.getClassName() + "Kt"
                ));
                continue;
            }

            // Check if it's an inner class of the target class
            if (!innerFullClassName.startsWith(classFile.getClassName() + "$")) {
                continue;
            }

            // Check if it's a class file
            String innerFileName = innerFullClassName.split("\\$")[1];
            if (!innerFileName.contains(".class")) {
                continue;
            }

            String subClassName = innerFileName.substring(0, innerFileName.lastIndexOf(".class"));
            String innerClassName = classFile.getClassName() + "$" + subClassName;

            // Collect the inner class of the target class
            innerClasses.add(new ClassFile(
                    classFile.getProject(),
                    fileInPackage,
                    classFile.getPackageName(),
                    innerClassName
            ));
        }

        return innerClasses;
    }

    @Override
    public boolean hotswap(DebuggerSession debuggerSession, HotSwapProgress progress, List<ClassFile> classFiles) {
        // Create a map of the modified classes and store our previous map into this one
        Map<DebuggerSession, Map<String, HotSwapFile>> modifiedClasses = new HashMap<>();
        Map<String, HotSwapFile> files = new HashMap<>();
        for (ClassFile classFile : classFiles) {
            files.put(classFile.getClassPath(), classFile.toHotSwapFile());
        }
        modifiedClasses.put(debuggerSession, files);

        // Reload all classes in our map
        HotSwapManager.reloadModifiedClasses(modifiedClasses, progress);

        // Debugger session is available
        return true;
    }


    @Override
    public String getName(PsiFile file) {
        return file.getName();
    }

    /**
     * Get the package name of the given PSI file
     *
     * @param file PSI file to get the package name
     * @return Full package name of the PSI file
     */
    protected abstract String getPackageName(T file);

    /**
     * Get file extension name of the current hotswap file type implementation
     *
     * @return File type extension name as string
     */
    protected abstract String getExtensionName();

}
