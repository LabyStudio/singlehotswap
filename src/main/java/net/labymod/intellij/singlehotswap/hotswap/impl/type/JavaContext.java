package net.labymod.intellij.singlehotswap.hotswap.impl.type;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import net.labymod.intellij.singlehotswap.compiler.AbstractCompiler;
import net.labymod.intellij.singlehotswap.compiler.impl.BuiltInJavaCompiler;
import net.labymod.intellij.singlehotswap.hotswap.impl.AbstractContext;

/**
 * Java context implementation
 *
 * @author LabyStudio
 */
public class JavaContext extends AbstractContext<PsiJavaFile> {

    @Override
    public AbstractCompiler createCustomCompiler() {
        return new BuiltInJavaCompiler(this);
    }

    @Override
    protected String getPackageName(PsiJavaFile file) {
        return file.getPackageName();
    }

    @Override
    protected String getExtensionName() {
        return "java";
    }

    @Override
    public boolean isPossible(PsiFile file) {
        return file instanceof PsiJavaFile;
    }
}
