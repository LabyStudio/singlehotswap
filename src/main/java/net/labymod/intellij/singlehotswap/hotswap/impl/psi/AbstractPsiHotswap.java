package net.labymod.intellij.singlehotswap.hotswap.impl.psi;

import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.impl.HotSwapFile;
import com.intellij.debugger.impl.HotSwapManager;
import com.intellij.debugger.ui.HotSwapProgressImpl;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import net.labymod.intellij.singlehotswap.hotswap.IHotswap;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Hotswap implementation for PSI files
 *
 * @param <T> File type class to get the package name
 * @author LabyStudio
 */
public abstract class AbstractPsiHotswap<T> implements IHotswap {

    @SuppressWarnings( "unchecked" )
    @Override
    public boolean hotswap( PsiFile file ) {
        // Get name, file name and class name
        String fileName = file.getName();
        String classNameWithoutPackage = fileName.substring( 0, fileName.toLowerCase().lastIndexOf( "." + this.getExtensionName() ) );
        String packageName = this.getPackageName( (T) file );

        // Create the full class name
        String className = packageName.isEmpty() ? classNameWithoutPackage : ( packageName + "." + classNameWithoutPackage );
        return this.hotswap( file.getProject(), classNameWithoutPackage, className );
    }

    /**
     * Hotswap a single file using the class name.
     *
     * @param project                 Instance of the current project containing the target file
     * @param classNameWithoutPackage Class name without package
     * @param className               Target class name
     * @return Hotswap successfully executed if the class name is available in the output paths
     */
    protected boolean hotswap( Project project, String classNameWithoutPackage, String className ) {
        // Find the class name in the output paths of the project
        String[] outputPaths = CompilerPaths.getOutputPaths( ModuleManager.getInstance( project ).getModules() );
        for ( String outputPath : outputPaths ) {

            // Create file instance using the output path and the class name
            File file = Paths.get( outputPath + "/" + className.replace( '.', '/' ) + ".class" ).toFile();

            // Hotswap the file if exists
            if ( file.exists() ) {
                // Create map of files to hotswap
                Map<String, HotSwapFile> files = new HashMap<>();

                // List all compiled files in this directory to find inner classes
                File[] filesInPackage = file.getParentFile() == null ? null : file.getParentFile().listFiles();

                // This should never happen but we check it in case
                if ( filesInPackage != null ) {
                    // Find inner classes
                    for ( File fileInPackage : filesInPackage ) {
                        String innerFullClassName = fileInPackage.getName();

                        // Check if it's an inner class of the target class
                        if ( innerFullClassName.startsWith( classNameWithoutPackage + "$" ) ) {
                            String innerFileName = innerFullClassName.split( "\\$" )[1];

                            if ( innerFileName.contains( ".class" ) ) {
                                String innerClassName = className + "$" + innerFileName.substring( 0, innerFileName.lastIndexOf( ".class" ) );

                                // Compile the inner class of the target class
                                files.put( innerClassName, new HotSwapFile( fileInPackage ) );
                            }
                        }
                    }
                }

                // Just compile the target file
                files.put( className, new HotSwapFile( file ) );

                // Hotswap files
                return this.hotswap( project, files );
            }
        }

        // Could not find class file in output path
        return false;
    }

    /**
     * Hotswap a single file using the class name and a java instance.
     * No further hot swaps are triggered when this function is executed.
     *
     * @param project Instance of the current project containing the target file
     * @return Debugger session is available
     */
    protected boolean hotswap( Project project, Map<String, HotSwapFile> files ) {
        // Get debugger
        DebuggerManagerEx debuggerManager = DebuggerManagerEx.getInstanceEx( project );
        DebuggerSession debuggerSession = debuggerManager.getContext().getDebuggerSession();

        // Debugger session is not available
        if ( debuggerSession == null ) {
            return false;
        }

        // Create a map of the modified classes and store our previous map into this one
        Map<DebuggerSession, Map<String, HotSwapFile>> modifiedClasses = new HashMap<>();
        modifiedClasses.put( debuggerSession, files );

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

        // Debugger session is available
        return true;
    }

    @Override
    public String getName( PsiFile file ) {
        return file.getName();
    }

    /**
     * Get the package name of the given PSI file
     *
     * @param file PSI file to get the package name
     * @return Full package name of the PSI file
     */
    protected abstract String getPackageName( T file );

    /**
     * Get file extension name of the current hotswap file type implementation
     *
     * @return File type extension name as string
     */
    protected abstract String getExtensionName( );
}
