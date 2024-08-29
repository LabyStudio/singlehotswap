package net.labymod.intellij.singlehotswap.hotswap;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

/**
 * All available file types for single hot-swapping
 *
 * @author LabyStudio
 */
public enum FileType {
    NONE(null, null),
    JAVA(
            "net.labymod.intellij.singlehotswap.hotswap.impl.type.JavaContext",
            "com.intellij.java"
    ),
    GROOVY(
            "net.labymod.intellij.singlehotswap.hotswap.impl.type.GroovyContext",
            "org.intellij.groovy"
    ),
    KOTLIN(
            "net.labymod.intellij.singlehotswap.hotswap.impl.type.KotlinContext",
            "org.jetbrains.kotlin"
    );

    private final String className;

    private final String requiredPluginId;

    /**
     * Hotswap implementation
     */
    private Context context;

    /**
     * Creates and instance of the given implementation class for hot-swapping if the required plugin is available
     *
     * @param className        Context implementation class
     * @param requiredPluginId The required plugin for this hotswap type. This can be null to skip the requirement.
     */
    FileType(String className, String requiredPluginId) {
        this.className = className;
        this.requiredPluginId = requiredPluginId;
        this.context = null;
    }

    /**
     * Get the context implementation for the current type
     *
     * @return Context implementation
     */
    public Context context() {
        if (this.context == null) {
            // Create context instance if not available
            this.context = this.createContext();
        }
        return this.context;
    }

    /**
     * Create context instance
     *
     * @return Context instance or null if not available
     */
    private Context createContext() {
        if (this.className == null) {
            return null;
        }

        try {
            // Check if plugin is required
            if (this.requiredPluginId != null) {

                // Find plugin by plugin id
                // Note: Access plugin manager here because of #23
                @Nullable IdeaPluginDescriptor plugin = PluginManager.getInstance().findEnabledPlugin(PluginId.getId(this.requiredPluginId));

                // Skip implementation if not plugin is not available
                if (plugin == null || !plugin.isEnabled()) {
                    return null;
                }
            }

            // Load implementation
            return (Context) Class.forName(this.className).getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Find context implementation using the PSI file
     *
     * @param file PSI file
     * @return Context implementation of the given PSI file type
     */
    public static Context findContext(PsiFile file) {
        if (file != null) {
            for (FileType type : values()) {
                Context instance = type.context();
                if (instance != null && instance.isPossible(file)) {
                    return instance;
                }
            }
        }

        return NONE.context();
    }
}
