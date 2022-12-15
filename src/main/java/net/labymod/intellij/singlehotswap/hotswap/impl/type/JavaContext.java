package net.labymod.intellij.singlehotswap.hotswap.impl.type;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import net.labymod.intellij.singlehotswap.compiler.AbstractCompiler;
import net.labymod.intellij.singlehotswap.compiler.impl.BuiltInJavaCompiler;
import net.labymod.intellij.singlehotswap.compiler.impl.DefaultCompiler;
import net.labymod.intellij.singlehotswap.hotswap.impl.AbstractContext;
import net.labymod.intellij.singlehotswap.storage.SingleHotswapConfiguration;

/**
 * Java context implementation
 *
 * @author LabyStudio
 */
public class JavaContext extends AbstractContext<PsiJavaFile> {

    @Override
    public AbstractCompiler compiler(SingleHotswapConfiguration configuration, boolean forceDefault) {
        // Choose between built-in Java compiler or default compiler
        return configuration.isUseBuiltInCompiler() && !forceDefault
                ? new BuiltInJavaCompiler(this)
                : new DefaultCompiler(this);
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
