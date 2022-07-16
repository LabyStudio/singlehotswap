package icons;

import com.intellij.icons.AllIcons;
import com.intellij.ui.IconManager;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class SingleHotSwapIcons {

    public static final @NotNull
    Icon SINGLE_HOTSWAP = new Icon() {
        private final Icon baseIcon = AllIcons.Actions.Compile;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            IconManager.getInstance()
                    .colorize(
                            (Graphics2D) g,
                            this.baseIcon,
                            new JBColor(
                                    new Color(0x3388FF),
                                    new Color(0x3388FF)
                            )
                    ).paintIcon(c, g, x, y);
        }

        @Override
        public int getIconWidth() {
            return this.baseIcon.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return this.baseIcon.getIconHeight();
        }
    };
}
