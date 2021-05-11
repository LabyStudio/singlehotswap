package net.labymod.intellij.singlehotswap.hotswap;

import com.intellij.psi.PsiFile;
import net.labymod.intellij.singlehotswap.hotswap.impl.DefaultHotswap;
import net.labymod.intellij.singlehotswap.hotswap.impl.psi.type.GroovyHotswap;
import net.labymod.intellij.singlehotswap.hotswap.impl.psi.type.JavaHotswap;
import net.labymod.intellij.singlehotswap.hotswap.impl.psi.type.KotlinHotswap;

/**
 * All available file types for single hot-swapping
 *
 * @author LabyStudio
 */
public enum EnumFileType {
    NONE( DefaultHotswap.class ),
    JAVA( JavaHotswap.class ),
    GROOVY( GroovyHotswap.class ),
    KOTLIN( KotlinHotswap.class );

    /**
     * Hotswap implementation
     */
    private IHotswap hotswap;

    /**
     * Creates and instance of the given implementation class for hot-swapping
     *
     * @param clazz Hotswap implementation class
     */
    EnumFileType( Class<? extends IHotswap> clazz ) {
        try {
            this.hotswap = clazz.getConstructor().newInstance();
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
                if ( instance.isPossible( file ) ) {
                    return instance;
                }
            }
        }

        return NONE.getInstance();
    }
}
