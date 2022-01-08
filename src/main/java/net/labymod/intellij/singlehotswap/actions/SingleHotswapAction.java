package net.labymod.intellij.singlehotswap.actions;

import com.intellij.compiler.actions.CompileAction;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.task.ProjectTaskManager;
import com.intellij.task.impl.ModuleFilesBuildTaskImpl;
import net.labymod.intellij.singlehotswap.hotswap.EnumFileType;
import net.labymod.intellij.singlehotswap.hotswap.IHotswap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Single hotswap action to trigger the hotswap on the target file
 *
 * @author LabyStudio
 */
public class SingleHotswapAction extends CompileAction {

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
            IHotswap hotswap = EnumFileType.find( currentFile );

            // Define button state
            boolean enabled = debuggerSession != null
                    && compilableFiles.length > 0
                    && !compileManager.isCompilationActive()
                    && hotswap.isPossible( currentFile );

            // Update state
            presentation.setEnabled( enabled );

            // Update status text
            if ( enabled ) {
                presentation.setText( "Hotswap '" + hotswap.getName( currentFile ) + "'" );
            } else {
                String reason = "Only Available with an Opened Java File";

                if ( debuggerSession == null ) {
                    reason = "Only Available in Debug Session";
                } else if ( currentFile != null ) {
                    reason = "Not available for " + currentFile.getClass().getSimpleName();
                }

                presentation.setText( String.format( "Single Hotswap (%s)", reason ) );
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
        PsiFile file = event.getData( CommonDataKeys.PSI_FILE );
        Project project = file == null ? null : file.getProject();
        IHotswap hotswap = EnumFileType.find( file );

        // Is the target file a valid java file?
        if ( project != null && hotswap.isPossible( file ) ) {

            // Compile a single file
            this.compileSingleFile( project, file.getVirtualFile(), success -> {
                if ( !success ) {
                    this.notifyUser( "Could not compile " + file.getName(), NotificationType.ERROR );
                    return;
                }

                if ( !hotswap.hotswap( file ) ) {
                    this.notifyUser( "Could not hotswap " + file.getName(), NotificationType.ERROR );
                }
            } );
        } else if ( file != null ) {
            this.notifyUser( "Invalid file to hotswap: " + file.getName(), NotificationType.WARNING );
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

        // Store previous state of this flag because you can toggle it in the IntelliJ settings
        String prevRunHotswap = settings.RUN_HOTSWAP_AFTER_COMPILE;

        // We disable the RUN_HOTSWAP_AFTER_COMPILE flag because the built-in hotswap of
        // IntelliJ always reloads every single file that is referenced by the target class.
        settings.RUN_HOTSWAP_AFTER_COMPILE = DebuggerSettings.RUN_HOTSWAP_NEVER;

        // Create task
        ProjectFileIndex index = ProjectFileIndex.SERVICE.getInstance( project );
        @Nullable Module module = index.getModuleForFile( virtualFile, false );
        if ( module == null ) {
            callback.accept( false );
            return;
        }

        // Switch to IDE compiler
        ExternalSystemModulePropertyManager propertyManager = ExternalSystemModulePropertyManager.getInstance( module );
        String prevSystemIdName = propertyManager.getExternalSystemId();
        @Nullable ProjectSystemId prevSystemId = prevSystemIdName == null ? null : ProjectSystemId.findById( prevSystemIdName );
        if ( prevSystemId != null ) {
            propertyManager.setExternalId( ProjectSystemId.IDE );
        }

        // Compile virtual file
        ModuleFilesBuildTaskImpl task = new ModuleFilesBuildTaskImpl( module, true, virtualFile );
        projectTaskManager.run( task ).onProcessed( result -> {
            // Change the flag back to its previous state
            settings.RUN_HOTSWAP_AFTER_COMPILE = prevRunHotswap;

            // Switch back to previous compiler
            if ( prevSystemId != null ) {
                ApplicationManager.getApplication().invokeLater( ( ) -> propertyManager.setExternalId( prevSystemId ) );
            }

            // Run callback with success state
            callback.accept( result != null && !result.hasErrors() );
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
