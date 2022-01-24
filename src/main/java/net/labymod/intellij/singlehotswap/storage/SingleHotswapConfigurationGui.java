package net.labymod.intellij.singlehotswap.storage;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SingleHotswapConfigurationGui implements SearchableConfigurable, Configurable.NoScroll {

    private final SingleHotswapConfiguration state;

    private JPanel panel;

    private JCheckBox checkBoxForceBuiltInCompiler;

    public SingleHotswapConfigurationGui( ) {
        this.state = ServiceManager.getService( SingleHotswapConfiguration.class );
    }

    @Override
    public @NotNull String getId( ) {
        return "SingleHotswapGui";
    }

    @Override
    public String getDisplayName( ) {
        return "Single Hotswap";
    }

    @Override
    public @Nullable JComponent createComponent( ) {
        return this.panel;
    }

    @Override
    public boolean isModified( ) {
        return this.state.isForceBuiltInCompiler() != this.checkBoxForceBuiltInCompiler.isSelected();
    }

    @Override
    public void apply( ) {
        this.state.setForceBuiltInCompiler( this.checkBoxForceBuiltInCompiler.isSelected() );
    }

    @Override
    public void reset( ) {
        this.checkBoxForceBuiltInCompiler.setSelected( this.state.isForceBuiltInCompiler() );
    }
}
