package net.labymod.intellij.singlehotswap.storage;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.NORTHWEST;
import static java.awt.GridBagConstraints.RELATIVE;

public class SingleHotswapConfigurationGui implements SearchableConfigurable, Configurable.NoScroll {

    private final SingleHotswapConfiguration state;

    private JCheckBox checkBoxUseBuiltInCompiler;
    private JCheckBox checkBoxShowCompileDuration;
    private JCheckBox checkBoxForceDefaultCompilerShift;

    public SingleHotswapConfigurationGui() {
        this.state = ServiceManager.getService(SingleHotswapConfiguration.class);
    }

    @Override
    public @NotNull String getId() {
        return "SingleHotswapGui";
    }

    @Override
    public String getDisplayName() {
        return "Single Hotswap";
    }

    @Override
    public @Nullable JComponent createComponent() {
        JPanel container = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel panel = new JPanel(new GridBagLayout());

        this.checkBoxUseBuiltInCompiler = new JCheckBox("Use Built-In Compiler");
        this.checkBoxUseBuiltInCompiler.setToolTipText("Increases the speed of a hotswap for Gradle projects by using Intellij's internal compiler for single hotswaps.");

        this.checkBoxShowCompileDuration = new JCheckBox("Show Compile Duration");
        this.checkBoxShowCompileDuration.setToolTipText("Shows the duration of the compilation time in the status bar.");

        this.checkBoxForceDefaultCompilerShift = new JCheckBox("Force Default Compiler When Holding Shift");
        this.checkBoxForceDefaultCompilerShift.setToolTipText("Forces the default compiler when holding the shift key while pressing the hotswap button.");

        panel.add(this.checkBoxUseBuiltInCompiler, new GridBagConstraints(0, RELATIVE, 1, 1, 1.0, 0.0, NORTHWEST, NONE, JBUI.emptyInsets(), 0, 0));
        panel.add(this.checkBoxShowCompileDuration, new GridBagConstraints(0, RELATIVE, 1, 1, 1.0, 0.0, NORTHWEST, NONE, JBUI.insetsTop(4), 0, 0));
        panel.add(this.checkBoxForceDefaultCompilerShift, new GridBagConstraints(0, RELATIVE, 1, 1, 1.0, 0.0, NORTHWEST, NONE, JBUI.insetsTop(4), 0, 0));
        container.add(panel);

        return container;
    }

    @Override
    public boolean isModified() {
        return this.state.isUseBuiltInCompiler() != this.checkBoxUseBuiltInCompiler.isSelected()
                || this.state.isShowCompileDuration() != this.checkBoxShowCompileDuration.isSelected()
                || this.state.isForceDefaultCompilerShift() != this.checkBoxForceDefaultCompilerShift.isSelected();
    }

    @Override
    public void apply() {
        this.state.setUseBuiltInCompiler(this.checkBoxUseBuiltInCompiler.isSelected());
        this.state.setShowCompileDuration(this.checkBoxShowCompileDuration.isSelected());
        this.state.setForceDefaultCompilerShift(this.checkBoxForceDefaultCompilerShift.isSelected());
    }

    @Override
    public void reset() {
        this.checkBoxUseBuiltInCompiler.setSelected(this.state.isUseBuiltInCompiler());
        this.checkBoxShowCompileDuration.setSelected(this.state.isShowCompileDuration());
        this.checkBoxForceDefaultCompilerShift.setSelected(this.state.isForceDefaultCompilerShift());
    }
}
