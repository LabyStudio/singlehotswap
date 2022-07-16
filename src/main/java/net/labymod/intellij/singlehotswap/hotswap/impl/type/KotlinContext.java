package net.labymod.intellij.singlehotswap.hotswap.impl.type;

import com.intellij.psi.PsiFile;
import net.labymod.intellij.singlehotswap.hotswap.impl.AbstractContext;
import org.jetbrains.kotlin.psi.KtFile;

/**
 * Kotlin context implementation
 *
 * @author LabyStudio
 */
public class KotlinContext extends AbstractContext<KtFile> {

    @Override
    protected String getPackageName(KtFile file) {
        return file.getPackageFqName().asString();
    }

    @Override
    protected String getExtensionName() {
        return "kt";
    }

    @Override
    public boolean isPossible(PsiFile file) {
        return file instanceof KtFile;
    }
}
