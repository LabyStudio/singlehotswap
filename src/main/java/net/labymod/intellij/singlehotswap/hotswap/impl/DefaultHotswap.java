package net.labymod.intellij.singlehotswap.hotswap.impl;

import com.intellij.psi.PsiFile;
import net.labymod.intellij.singlehotswap.hotswap.IHotswap;

/**
 * Default implementation for invalid files to hotswap.
 * Returns always false at {@link #isPossible(PsiFile)} and has no hotswap logic implemented.
 *
 * @author LabyStudio
 */
public class DefaultHotswap implements IHotswap {

    @Override
    public boolean hotswap( PsiFile file ) {
        return false;
    }

    @Override
    public boolean isPossible( PsiFile file ) {
        return false;
    }

    @Override
    public String getName( PsiFile file ) {
        return file == null ? "Unknown" : file.getName();
    }
}
