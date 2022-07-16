package net.labymod.intellij.singlehotswap.compiler.impl;

import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.task.ProjectTaskManager;
import com.intellij.task.ProjectTaskManager.Result;
import com.intellij.task.impl.ModuleFilesBuildTaskImpl;
import net.labymod.intellij.singlehotswap.compiler.AbstractCompiler;
import net.labymod.intellij.singlehotswap.hotswap.ClassFile;
import net.labymod.intellij.singlehotswap.hotswap.Context;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the default project compiler.
 * It will trigger the build task of gradle or maven if present.
 *
 * @author LabyStudio
 */
public class DefaultCompiler extends AbstractCompiler {

    public DefaultCompiler(Context context) {
        super(context);
    }

    @Override
    public List<ClassFile> compile(VirtualFile sourceFile, ClassFile outputFile) throws Exception {
        List<ClassFile> classFiles = new ArrayList<>();

        Project project = outputFile.getProject();
        ProjectTaskManager projectTaskManager = ProjectTaskManager.getInstance(project);
        DebuggerSettings settings = DebuggerSettings.getInstance();

        // Store previous state of this flag because you can toggle it in the IntelliJ settings
        String prevRunHotswap = settings.RUN_HOTSWAP_AFTER_COMPILE;

        // We disable the RUN_HOTSWAP_AFTER_COMPILE flag because the built-in hotswap of
        // IntelliJ always reloads every single file that is referenced by the target class.
        settings.RUN_HOTSWAP_AFTER_COMPILE = DebuggerSettings.RUN_HOTSWAP_NEVER;

        // Create task
        ProjectFileIndex index = ProjectFileIndex.SERVICE.getInstance(project);
        @Nullable Module module = index.getModuleForFile(sourceFile, false);
        if (module == null) {
            return classFiles;
        }

        // Compile virtual file
        ModuleFilesBuildTaskImpl task = new ModuleFilesBuildTaskImpl(module, true, sourceFile);
        Result result = projectTaskManager.run(task).blockingGet(3, TimeUnit.MINUTES);

        // Change the flag back to its previous state
        settings.RUN_HOTSWAP_AFTER_COMPILE = prevRunHotswap;

        // Check for errors
        if (result == null || result.hasErrors() || result.isAborted()) {
            return classFiles;
        }

        // Add the compiled output file to the list
        classFiles.add(outputFile);

        // Add the inner class files to the list
        classFiles.addAll(this.context.getInnerClassFiles(outputFile));

        return classFiles;
    }
}
