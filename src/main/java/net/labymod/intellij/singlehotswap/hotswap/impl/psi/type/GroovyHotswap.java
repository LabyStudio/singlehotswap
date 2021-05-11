package net.labymod.intellij.singlehotswap.hotswap.impl.psi.type;

import com.intellij.psi.PsiFile;
import net.labymod.intellij.singlehotswap.hotswap.impl.psi.AbstractPsiHotswap;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

/**
 * Groovy hotswap implementation
 *
 * @author LabyStudio
 */
public class GroovyHotswap extends AbstractPsiHotswap<GroovyFile> {

    @Override
    protected String getPackageName( GroovyFile file ) {
        return file.getPackageName();
    }

    @Override
    protected String getExtensionName( ) {
        return "groovy";
    }

    @Override
    public boolean isPossible( PsiFile file ) {
        return file instanceof GroovyFile;
    }
}
