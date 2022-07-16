package net.labymod.intellij.singlehotswap.hotswap.impl.type;

import com.intellij.psi.PsiFile;
import net.labymod.intellij.singlehotswap.hotswap.impl.AbstractContext;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

/**
 * Groovy context implementation
 *
 * @author LabyStudio
 */
public class GroovyContext extends AbstractContext<GroovyFile> {

    @Override
    protected String getPackageName(GroovyFile file) {
        return file.getPackageName();
    }

    @Override
    protected String getExtensionName() {
        return "groovy";
    }

    @Override
    public boolean isPossible(PsiFile file) {
        return file instanceof GroovyFile;
    }
}
