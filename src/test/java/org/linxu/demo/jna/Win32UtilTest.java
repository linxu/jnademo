package org.linxu.demo.jna;

import com.sun.jna.platform.win32.WinDef;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Win32UtilTest {
    @Test
    public void test01() {
        WinDef.HWND hwnd = Win32Util.findHandleByClassName("RichEditD2DPT");
        Assertions.assertNotNull(hwnd);
        boolean ok = Win32Util.simulateTextInput(hwnd, "test6");
        Assertions.assertTrue(ok);
    }
}
