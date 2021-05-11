package net.labymod.intellij.singlehotswap.hotswap.impl.psi.type;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import net.labymod.intellij.singlehotswap.hotswap.impl.psi.AbstractPsiHotswap;

/**
 * Java hotswap implementation
 *
 * @author LabyStudio
 */
public class JavaHotswap extends AbstractPsiHotswap<PsiJavaFile> {

    @Override
    protected String getPackageName( PsiJavaFile file ) {
        return file.getPackageName();
    }

    @Override
    protected String getExtensionName( ) {
        return "java";
    }

    @Override
    public boolean isPossible( PsiFile file ) {
        return file instanceof PsiJavaFile;
    }
}
