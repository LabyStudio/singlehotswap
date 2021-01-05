package net.labymod.intellij.singlehotswap;

import com.google.common.collect.ImmutableMap;
import com.intellij.compiler.actions.CompileAction;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.impl.HotSwapFile;
import com.intellij.debugger.impl.HotSwapManager;
import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.debugger.ui.HotSwapProgressImpl;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.task.ProjectTaskManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SingleHotSwap extends CompileAction {

    /**
     * Update the visibility of the single hotswap button
     * The button is only visible if the debug session is running and a compilable file is available.
     *
     * @param event AnActionEvent
     */
    @Override
    public void update( @NotNull AnActionEvent event ) {
        Project project = event.getProject();
        Presentation presentation = event.getPresentation();

        if ( project == null ) {
            // Disable button if project is null
            presentation.setEnabled( false );
        } else {
            // Get required instances
            DebuggerSession debuggerSession = DebuggerManagerEx.getInstanceEx( project ).getContext().getDebuggerSession();
            VirtualFile[] availableFiles = event.getData( CommonDataKeys.VIRTUAL_FILE_ARRAY );
            VirtualFile[] compilableFiles = getCompilableFiles( project, availableFiles );
            CompilerManager compileManager = CompilerManager.getInstance( project );

            // Get the current target file
            PsiFile currentFile = event.getData( CommonDataKeys.PSI_FILE );

            // Define button state
            boolean enabled = debuggerSession != null
                    && compilableFiles.length > 0
                    && !compileManager.isCompilationActive()
                    && currentFile instanceof PsiJavaFile;

            // Update state
            presentation.setEnabled( enabled );

            // Update status text
            if ( enabled ) {
                presentation.setText( "Hotswap '" + currentFile.getName() + "'" );
            } else {
                if ( debuggerSession == null ) {
                    presentation.setText( "Single Hotswap (Only Available in Debug Session)" );
                } else {
                    presentation.setText( "Single Hotswap (Only Available with an Opened Java File)" );
                }
            }
        }
    }

    /**
     * This event is triggered when the single hotswap button is pressed.
     * We check if the target file a valid java file.
     *
     * @param event AnActionEvent
     */
    @Override
    public void actionPerformed( @NotNull AnActionEvent event ) {
        Project project = event.getProject();
        PsiFile file = event.getData( CommonDataKeys.PSI_FILE );

        // Is the target file a valid java file?
        if ( project != null && file instanceof PsiJavaFile ) {
            PsiJavaFile javaFile = (PsiJavaFile) file;

            // Compile a single file
            compileSingleFile( project, javaFile.getVirtualFile(), success -> {
                if ( !success ) {
                    notifyUser( "Could not compile " + javaFile.getName(), NotificationType.ERROR );
                    return;
                }

                // Single file compilation successful, now we have to hotswap this file
                if ( !hotswapSingleFile( project, javaFile ) ) {
                    notifyUser( "Could not hotswap " + javaFile.getName(), NotificationType.ERROR );
                }
            } );
        } else if ( file != null ) {
            notifyUser( "Invalid file to hotswap: " + file.getName(), NotificationType.WARNING );
        }
    }

    /**
     * Compile a single file java file using an instance of VirtualFile
     * We use the ProjectTaskManager to run the compile process.
     *
     * @param project     Instance of the current project containing the target file
     * @param virtualFile The target file to compile
     * @param callback    This callback is triggered with a success state after the compilation
     */
    private void compileSingleFile( Project project, VirtualFile virtualFile, Consumer<Boolean> callback ) {
        ProjectTaskManager projectTaskManager = ProjectTaskManager.getInstance( project );
        DebuggerSettings settings = DebuggerSettings.getInstance();

        // No hotswap for this compile
        String prevRunHotswap = settings.RUN_HOTSWAP_AFTER_COMPILE;
        settings.RUN_HOTSWAP_AFTER_COMPILE = DebuggerSettings.RUN_HOTSWAP_NEVER;

        // Compile
        projectTaskManager.compile( virtualFile ).onProcessed( result -> {
            // Change setting back to previous state
            settings.RUN_HOTSWAP_AFTER_COMPILE = prevRunHotswap;

            // Run callback with success state
            callback.accept( !result.hasErrors() );
        } );
    }

    /**
     * Hotswap a single file using the an instance of PsiJavaFile.
     * This function gets the class name of the specified file and executes {@link #hotswapSingleFile(Project, String, File)}
     *
     * @param project Instance of the current project containing the target file
     * @param psiFile The target java file to hotswap
     * @return Hotswap successfully executed if the class name is available in the output paths
     */
    private boolean hotswapSingleFile( Project project, PsiJavaFile psiFile ) {
        // Get name, file name and class name
        String name = psiFile.getName();
        String fileName = name.substring( 0, name.toLowerCase().lastIndexOf( ".java" ) );
        String packageName = psiFile.getPackageName();

        // Create the full class name
        String className = packageName.isEmpty() ? fileName : ( packageName + "." + fileName );

        // Find the class name in the output paths of the project
        String[] outputPaths = CompilerPaths.getOutputPaths( ModuleManager.getInstance( project ).getModules() );
        for ( String outputPath : outputPaths ) {

            // Create file instance using the output path and the class name
            File file = Paths.get( outputPath + "/" + className.replace( '.', '/' ) + ".class" ).toFile();

            // Hotswap the file if exists
            if ( file.exists() ) {
                hotswapSingleFile( project, className, file );

                // Successfully executed
                return true;
            }
        }

        // Could not find class file in output path
        return false;
    }

    /**
     * Hotswap a single file using the class name and a java instance.
     * No further hot swaps are triggered when this function is executed.
     *
     * @param project   Instance of the current project containing the target file
     * @param className The target class name of the file to hotswap
     * @param file      The target file to hotswap
     */
    private void hotswapSingleFile( Project project, String className, File file ) {
        // Get debugger
        DebuggerManagerEx debuggerManager = DebuggerManagerEx.getInstanceEx( project );
        DebuggerSession debuggerSession = debuggerManager.getContext().getDebuggerSession();

        // Create map of files to hotswap
        ImmutableMap<String, HotSwapFile> value = ImmutableMap.<String, HotSwapFile>builder()
                .put( className, new HotSwapFile( file ) )
                .build();

        // Create a map of the modified classes and store our previous map into this one
        Map<DebuggerSession, Map<String, HotSwapFile>> modifiedClasses = new HashMap<>();
        modifiedClasses.put( debuggerSession, value );

        // Create hotswap progress
        HotSwapProgressImpl progress = new HotSwapProgressImpl( project );

        // Execute application thread
        Application application = ApplicationManager.getApplication();
        application.executeOnPooledThread( ( ) -> {
            // Run the progress
            ProgressManager.getInstance().runProcess( ( ) -> {
                // Reload all classes in our map
                HotSwapManager.reloadModifiedClasses( modifiedClasses, progress );

                // Progress completed
                progress.finished();
            }, progress.getProgressIndicator() );
        } );
    }

    /**
     * Notify the user with a notification.
     *
     * @param message The message of the notification
     * @param type    The notification type
     */
    private void notifyUser( String message, NotificationType type ) {
        Notifications.Bus.notify( new Notification( "SingleHotswap", "Single hotswap", message, type ) );
    }
}