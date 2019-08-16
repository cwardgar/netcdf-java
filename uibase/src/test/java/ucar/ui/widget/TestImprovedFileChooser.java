package ucar.ui.widget;

import java.awt.Dimension;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.junit.Test;

public class TestImprovedFileChooser {

  @Test
  public void testStuff() throws ClassNotFoundException, UnsupportedLookAndFeelException,
      InstantiationException, IllegalAccessException {
    // Switch to Nimbus Look and Feel, if it's available.
    if (ImprovedFileChooser.isMacOs) {
      System.setProperty ("apple.laf.useScreenMenuBar", "true");
    } else {
      for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    }
    ImprovedFileChooser fileChooser = new ImprovedFileChooser();
    fileChooser.setPreferredSize(new Dimension(1000, 750));
    fileChooser.showDialog(null, "Choose");
  }

}
