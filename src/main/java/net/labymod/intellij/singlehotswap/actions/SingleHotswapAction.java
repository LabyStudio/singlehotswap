package net.labymod.intellij.singlehotswap.actions;

import com.intellij.compiler.actions.CompileAction;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.ui.HotSwapProgressImpl;
import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilationException;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.MessageCategory;
import net.labymod.intellij.singlehotswap.compiler.AbstractCompiler;
import net.labymod.intellij.singlehotswap.hotswap.ClassFile;
import net.labymod.intellij.singlehotswap.hotswap.Context;
import net.labymod.intellij.singlehotswap.hotswap.FileType;
import net.labymod.intellij.singlehotswap.storage.SingleHotswapConfiguration;
import org.jetbrains.annotations.Nullable;

import java.awt.event.InputEvent;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * Single hotswap action to trigger the hotswap on the target file
 *
 * @author LabyStudio
 */
public class SingleHotswapAction extends CompileAction {

    private final SingleHotswapConfiguration configuration = ApplicationManager.getApplication().getService(SingleHotswapConfiguration.class);

    /**
     * Update the visibility of the single hotswap button
     * The button is only visible if the debug session is running and a compilable file is available.
     *
     * @param event AnActionEvent
     */
    @Override
    public void update(AnActionEvent event) {
        Project project = event.getProject();
        Presentation presentation = event.getPresentation();

        if (project == null) {
            // Disable button if project is null
            presentation.setEnabled(false);
        } else {
            // Get required instances
            DebuggerSession debuggerSession = DebuggerManagerEx.getInstanceEx(project).getContext().getDebuggerSession();
            VirtualFile[] availableFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
            VirtualFile[] compilableFiles = getCompilableFiles(project, availableFiles);
            CompilerManager compileManager = CompilerManager.getInstance(project);

            // Get the current target file
            PsiFile currentFile = event.getData(CommonDataKeys.PSI_FILE);
            Context context = FileType.findContext(currentFile);

            // Define button state
            boolean enabled = debuggerSession != null
                    && context != null
                    && compilableFiles.length > 0
                    && !compileManager.isCompilationActive()
                    && context.isPossible(currentFile);

            // Update state
            presentation.setEnabled(enabled);

            // Update status text
            if (enabled) {
                presentation.setText("Hotswap '" + context.getName(currentFile) + "'");
            } else {
                String reason = "Only Available with an Opened Java File";

                if (debuggerSession == null) {
                    reason = "Only Available in Debug Session";
                } else if (currentFile != null) {
                    reason = "Not available for " + currentFile.getClass().getSimpleName();
                }

                presentation.setText(String.format("Single Hotswap (%s)", reason));
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
    public void actionPerformed(AnActionEvent event) {
        try {
            PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
            if (psiFile == null) {
                return;
            }

            Project project = psiFile.getProject();
            Context context = FileType.findContext(psiFile);

            // Check if it is possible to hotswap the opened file
            if (!context.isPossible(psiFile)) {
                this.notifyUser("Invalid file to hotswap: " + psiFile.getName(), NotificationType.WARNING);
                return;
            }

            // Get debugger session
            DebuggerManagerEx debuggerManager = DebuggerManagerEx.getInstanceEx(project);
            DebuggerSession debugger = debuggerManager.getContext().getDebuggerSession();
            assert debugger != null;

            // Save the opened documents
            FileDocumentManager.getInstance().saveAllDocuments();

            // Create compiler and progress
            @Nullable InputEvent inputEvent = event.getInputEvent();
            boolean forceDefault = inputEvent != null && inputEvent.isShiftDown()
                    && this.configuration.isForceDefaultCompilerShift();
            AbstractCompiler compiler = context.compiler(this.configuration, forceDefault);

            try {
                ClassFile outputFile = context.getClassFile(psiFile);
                VirtualFile sourceFile = psiFile.getVirtualFile();

                Module module = ProjectFileIndex.getInstance(project).getModuleForFile(sourceFile);
                if (module == null) {
                    this.notifyUser("Could not find module for file: " + sourceFile.getName(), NotificationType.WARNING);
                    return;
                }

                // Execute progress
                HotSwapProgressImpl progress = new HotSwapProgressImpl(project);
                Application application = ApplicationManager.getApplication();
                application.executeOnPooledThread(() -> {
                    ProgressManager.getInstance().runProcess(() -> {
                        progress.setTitle("Compile classes...");

                        // Compile
                        try {
                            long start = System.currentTimeMillis();

                            // Compile the current opened file
                            List<ClassFile> classFiles = compiler.compile(module, sourceFile, outputFile);
                            if (classFiles.isEmpty()) {
                                String message = "Could not compile " + psiFile.getName();
                                progress.addMessage(debugger, MessageCategory.ERROR, message);
                                return;
                            }

                            // Show compile duration
                            long duration = System.currentTimeMillis() - start;
                            if (this.configuration.isShowCompileDuration()) {
                                String message = "Compiled " + classFiles.size() + " classes in " + duration + "ms";
                                progress.addMessage(debugger, MessageCategory.STATISTICS, message);
                            }
                            progress.setTitle("Hotswap classes...");

                            // Hotswap the file
                            if (!context.hotswap(debugger, progress, classFiles)) {
                                String message = "Could not hotswap " + psiFile.getName();
                                progress.addMessage(debugger, MessageCategory.ERROR, message);
                            }
                        } catch (CompilationException e) {
                            StringBuilder output = new StringBuilder("Error during compilation:");
                            for (CompilationException.Message message : e.getMessages()) {
                                output.append("\n")
                                        .append(message.getUrl())
                                        .append(":")
                                        .append(message.getLine())
                                        .append(" ")
                                        .append(message.getText());
                            }
                            progress.addMessage(debugger, MessageCategory.ERROR, output.toString());
                        } catch (Exception e) {
                            String message = "Error during hotswap: " + e.getMessage();
                            progress.addMessage(debugger, MessageCategory.ERROR, message);
                        }

                        // Finish the progress
                        progress.setTitle("Hotswap completed");
                        progress.finished();
                    }, progress.getProgressIndicator());
                });
            } catch (FileNotFoundException e) {
                Notification notification = new Notification("SingleHotswap", "Single hotswap", e.getMessage(), NotificationType.ERROR);
                notification.addAction(NotificationAction.create("Recompile", (anActionEvent, notification1) -> {
                    CompilerManager.getInstance(project).compile(new VirtualFile[]{psiFile.getVirtualFile()}, null);
                }));
                notification.addAction(NotificationAction.create("Open settings", (anActionEvent, notification1) -> {
                    ShowSettingsUtilImpl.getInstance().showSettingsDialog(project, "Gradle");
                }));
                Notifications.Bus.notify(notification);
            }
        } catch (Exception e) {
            this.notifyUser("Can't setup hotswap task: " + e.getMessage(), NotificationType.ERROR);
        }
    }

    /**
     * Notify the user with a notification.
     *
     * @param message The message of the notification
     * @param type    The notification type
     */
    private void notifyUser(String message, NotificationType type) {
        Notifications.Bus.notify(new Notification("SingleHotswap", "Single hotswap", message, type));
    }
}
