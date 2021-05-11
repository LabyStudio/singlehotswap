package net.labymod.intellij.singlehotswap.hotswap.impl.psi.type;

import com.intellij.psi.PsiFile;
import net.labymod.intellij.singlehotswap.hotswap.impl.psi.AbstractPsiHotswap;
import org.jetbrains.kotlin.psi.KtFile;

/**
 * Kotlin hotswap implementation
 *
 * @author LabyStudio
 */
public class KotlinHotswap extends AbstractPsiHotswap<KtFile> {

    @Override
    protected String getPackageName( KtFile file ) {
        return file.getPackageFqName().asString();
    }

    @Override
    protected String getExtensionName( ) {
        return "kt";
    }

    @Override
    public boolean isPossible( PsiFile file ) {
        return file instanceof KtFile;
    }
}
