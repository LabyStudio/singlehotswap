package net.labymod.intellij.singlehotswap.hotswap;

import com.intellij.psi.PsiFile;

/**
 * Hotswap interface to handle multiple file types
 *
 * @author LabyStudio
 */
public interface IHotswap {

    /**
     * Hotswap given file
     *
     * @param file Psi file to hotswap
     * @return Returns true when success
     */
    boolean hotswap( PsiFile file );

    /**
     * Is given file instance hot-swappable
     *
     * @param file Psi file to hotswap
     * @return Return true if file instance is hot-swappable
     */
    boolean isPossible( PsiFile file );

    /**
     * Get the pretty name of the given file
     *
     * @param file PSI file to get the name
     * @return Pretty name
     */
    String getName( PsiFile file );
}
