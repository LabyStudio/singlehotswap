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
public enum EnumFileType {
    NONE(
            "net.labymod.intellij.singlehotswap.hotswap.impl.DefaultHotswap",
            null
    ),
    JAVA(
            "net.labymod.intellij.singlehotswap.hotswap.impl.psi.type.JavaHotswap",
            "com.intellij.java"
    ),
    GROOVY(
            "net.labymod.intellij.singlehotswap.hotswap.impl.psi.type.GroovyHotswap",
            "org.intellij.groovy"
    ),
    KOTLIN(
            "net.labymod.intellij.singlehotswap.hotswap.impl.psi.type.KotlinHotswap",
            "org.jetbrains.kotlin"
    );

    /**
     * Hotswap implementation
     */
    private IHotswap hotswap;

    /**
     * Creates and instance of the given implementation class for hot-swapping if the required plugin is available
     *
     * @param className        Hotswap implementation class
     * @param requiredPluginId The required plugin for this hotswap type
     */
    EnumFileType( String className, String requiredPluginId ) {
        try {
            // Find plugin by plugin id
            @Nullable IdeaPluginDescriptor plugin = requiredPluginId == null ? null :
                    PluginManager.getInstance().findEnabledPlugin( PluginId.getId( requiredPluginId ) );

            // Skip implementation if not plugin is not available
            if ( plugin == null || !plugin.isEnabled() ) {
                return;
            }

            // Load implementation
            this.hotswap = (IHotswap) Class.forName( className ).getConstructor().newInstance();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Get hotswap implementation for current type
     *
     * @return Hotswap implementation
     */
    public IHotswap getInstance( ) {
        return hotswap;
    }

    /**
     * Find hotswap implementation using PSI file
     *
     * @param file PSI file
     * @return Hotswap implementation of given PSI file type
     */
    public static IHotswap find( PsiFile file ) {
        if ( file != null ) {
            for ( EnumFileType type : values() ) {
                IHotswap instance = type.getInstance();
                if ( instance != null && instance.isPossible( file ) ) {
                    return instance;
                }
            }
        }

        return NONE.getInstance();
    }
}
