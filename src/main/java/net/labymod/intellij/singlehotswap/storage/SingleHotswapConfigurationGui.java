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
    private JTextField compilerId;

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
        return !this.state.getForceCompilerId().equals( this.compilerId.getText() );
    }

    @Override
    public void apply( ) {
        this.state.setForceCompilerId( this.compilerId.getText() );
    }

    @Override
    public void reset( ) {
        this.compilerId.setText( this.state.getForceCompilerId() );
    }
}
