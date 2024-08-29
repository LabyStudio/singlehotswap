package icons;

import com.intellij.ui.IconManager;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.lang.reflect.Method;

public class ResourceIcon implements Icon {

    private final Icon baseIcon;

    public ResourceIcon(String path) {
        IconManager manager = IconManager.getInstance();
        Icon baseIcon;
        try {
            baseIcon = manager.getIcon(path, ResourceIcon.class.getClassLoader());
        } catch (Throwable e) {
            // Support for older versions of IntelliJ IDEA (before 2023.2.7)
            try {
                Method getIcon = manager.getClass().getDeclaredMethod("getIcon", String.class, Class.class);
                baseIcon = (Icon) getIcon.invoke(manager, path, ResourceIcon.class);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }
        this.baseIcon = baseIcon;
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
