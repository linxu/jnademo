package org.linxu.demo.jna;

import com.sun.jna.platform.win32.WinDef;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;

public class Win32UtilTest {
    @Test
    public void test01() {
        WinDef.HWND hwnd = Win32Util.findHandleByClassName("RichEditD2DPT");
        Assertions.assertNotNull(hwnd);
        boolean ok = Win32Util.simulateTextInput(hwnd, "test10");
        Assertions.assertTrue(ok);
        StringSelection stringSelection = new StringSelection("abc");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
        int[][] keyCodes = {{KeyEvent.VK_CONTROL, KeyEvent.VK_V}};
        Win32Util.simulateKeyboardEvent(hwnd, keyCodes);
    }
}
