package icons;

import com.intellij.ui.IconManager;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;

public class ResourceIcon implements Icon {

    private final Icon baseIcon;

    public ResourceIcon(String path) {
        this.baseIcon = IconManager.getInstance().getIcon(path, ResourceIcon.class);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        this.baseIcon.paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
        return this.baseIcon.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return this.baseIcon.getIconHeight();
    }
}
