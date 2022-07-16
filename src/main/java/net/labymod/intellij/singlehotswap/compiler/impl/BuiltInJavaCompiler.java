package net.labymod.intellij.singlehotswap.compiler.impl;

import com.intellij.openapi.compiler.ClassObject;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import net.labymod.intellij.singlehotswap.compiler.AbstractCompiler;
import net.labymod.intellij.singlehotswap.hotswap.ClassFile;
import net.labymod.intellij.singlehotswap.hotswap.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Implementation for the built-in compiler of IntelliJ.
 * It avoids Gradle/Maven build tasks by using the javac compiler to compile the given source file.
 * The compiled byte code overwrites the previously compiled class file in the output directory.
 *
 * @author LabyStudio
 */
public class BuiltInJavaCompiler extends AbstractCompiler {

    public BuiltInJavaCompiler(Context context) {
        super(context);
    }

    @Override
    public List<ClassFile> compile(VirtualFile sourceFile, ClassFile outputFile) throws Exception {
        File file = VfsUtil.virtualToIoFile(sourceFile);

        // Find current module
        Project project = outputFile.getProject();
        Module module = ProjectFileIndex.SERVICE.getInstance(project).getModuleForFile(sourceFile);
        if (module == null) {
            return new ArrayList<>();
        }

        // Find the class files and the class version
        // We take the java version from the previously compiled class file by reading the header
        String javaVersion = String.valueOf(outputFile.readJavaVersion());

        // Compiler options
        List<String> options = new ArrayList<>(Arrays.asList(
                "-encoding", "UTF-8",
                "-source", javaVersion,
                "-target", javaVersion
        ));

        // Collect dependencies
        List<File> classpath = new ArrayList<>();
        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        for (String path : rootManager.orderEntries().compileOnly().recursively().exportedOnly().withoutSdk().getPathsList().getPathList()) {
            classpath.add(new File(path));
        }
        for (String path : rootManager.orderEntries().compileOnly().sdkOnly().getPathsList().getPathList()) {
            classpath.add(new File(path));
        }

        // Find output directory
        File outputDirectory = new File(CompilerPaths.getOutputPaths(new Module[]{module})[0]);

        // Compile the files
        CompilerManager compilerManager = CompilerManager.getInstance(project);
        Collection<ClassObject> result = compilerManager.compileJavaCode(
                options,
                Collections.emptyList(),
                classpath,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singleton(file),
                outputDirectory
        );

        // Write files to output directory and return the class files
        List<ClassFile> classFiles = new ArrayList<>();
        for (ClassObject classObject : result) {
            byte[] bytes = classObject.getContent();
            if (bytes == null) {
                continue;
            }

            // Write file to output directory
            File compiledFile = new File(classObject.getPath());
            FileUtil.writeToFile(compiledFile, bytes);

            // Add class file to list
            classFiles.add(ClassFile.fromClassObject(project, classObject));
        }
        return classFiles;
    }

}
