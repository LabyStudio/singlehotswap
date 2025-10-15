package net.labymod.intellij.singlehotswap.storage;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

import static java.awt.GridBagConstraints.*;

public class SingleHotswapConfigurationGui implements SearchableConfigurable, Configurable.NoScroll {

    private final SingleHotswapConfiguration state;

    private JCheckBox checkBoxUseBuiltInCompiler;
    private JCheckBox checkBoxShowCompileDuration;
    private JCheckBox checkBoxForceDefaultCompilerShift;
    private JTextField textFieldKotlinCompilerPath;

    public SingleHotswapConfigurationGui() {
        this.state = ApplicationManager.getApplication().getService(SingleHotswapConfiguration.class);
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

        this.textFieldKotlinCompilerPath = new JTextField();
        this.textFieldKotlinCompilerPath.setToolTipText("Path to the kotlinc executable");
        this.textFieldKotlinCompilerPath.setColumns(40);

        JLabel labelKotlinCompiler = new JLabel("Kotlin Compiler Path:");

        // Add checkboxes
        panel.add(this.checkBoxUseBuiltInCompiler, new GridBagConstraints(0, RELATIVE, 1, 1, 1.0, 0.0, NORTHWEST, NONE, JBUI.emptyInsets(), 0, 0));
        panel.add(this.checkBoxShowCompileDuration, new GridBagConstraints(0, RELATIVE, 1, 1, 1.0, 0.0, NORTHWEST, NONE, JBUI.insetsTop(4), 0, 0));
        panel.add(this.checkBoxForceDefaultCompilerShift, new GridBagConstraints(0, RELATIVE, 1, 1, 1.0, 0.0, NORTHWEST, NONE, JBUI.insetsTop(4), 0, 0));

        // Label above text field
        panel.add(labelKotlinCompiler, new GridBagConstraints(0, RELATIVE, 1, 1, 1.0, 0.0, NORTHWEST, NONE, JBUI.insetsTop(12), 0, 0));
        panel.add(this.textFieldKotlinCompilerPath, new GridBagConstraints(0, RELATIVE, 1, 1, 1.0, 0.0, NORTHWEST, HORIZONTAL, JBUI.insetsTop(2), 0, 0));

        container.add(panel);
        return container;
    }

    @Override
    public boolean isModified() {
        return this.state.isUseBuiltInCompiler() != this.checkBoxUseBuiltInCompiler.isSelected()
                || this.state.isShowCompileDuration() != this.checkBoxShowCompileDuration.isSelected()
                || this.state.isForceDefaultCompilerShift() != this.checkBoxForceDefaultCompilerShift.isSelected()
                || !this.state.getKotlinCompilerPath().equals(this.textFieldKotlinCompilerPath.getText());
    }

    @Override
    public void apply() {
        this.state.setUseBuiltInCompiler(this.checkBoxUseBuiltInCompiler.isSelected());
        this.state.setShowCompileDuration(this.checkBoxShowCompileDuration.isSelected());
        this.state.setForceDefaultCompilerShift(this.checkBoxForceDefaultCompilerShift.isSelected());
        this.state.setKotlinCompilerPath(this.textFieldKotlinCompilerPath.getText());
    }

    @Override
    public void reset() {
        this.checkBoxUseBuiltInCompiler.setSelected(this.state.isUseBuiltInCompiler());
        this.checkBoxShowCompileDuration.setSelected(this.state.isShowCompileDuration());
        this.checkBoxForceDefaultCompilerShift.setSelected(this.state.isForceDefaultCompilerShift());
        this.textFieldKotlinCompilerPath.setText(this.state.getKotlinCompilerPath());
    }
}
