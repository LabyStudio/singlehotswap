package net.labymod.intellij.singlehotswap.storage;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@State(name = "SingleHotswapConfiguration", storages = @Storage("singlehotswap.xml"))
public class SingleHotswapConfiguration implements PersistentStateComponent<SingleHotswapConfiguration> {

    private boolean useBuiltInCompiler = true;
    private boolean showCompileDuration = true;

    @Override
    public @Nullable SingleHotswapConfiguration getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SingleHotswapConfiguration languageConfiguration) {
        XmlSerializerUtil.copyBean(languageConfiguration, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        SingleHotswapConfiguration that = (SingleHotswapConfiguration) o;
        return this.useBuiltInCompiler == that.useBuiltInCompiler
                && this.showCompileDuration == that.showCompileDuration;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.useBuiltInCompiler, this.showCompileDuration);
    }

    public boolean isUseBuiltInCompiler() {
        return this.useBuiltInCompiler;
    }

    public void setUseBuiltInCompiler(boolean useBuiltInCompiler) {
        this.useBuiltInCompiler = useBuiltInCompiler;
    }

    public boolean isShowCompileDuration() {
        return this.showCompileDuration;
    }

    public void setShowCompileDuration(boolean showCompileDuration) {
        this.showCompileDuration = showCompileDuration;
    }
}
