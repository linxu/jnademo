package org.linxu.demo.jna;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;

import java.util.concurrent.*;

import static org.linxu.demo.jna.User32Ext.USER32EXT;
import static org.linxu.demo.jna.Win32Constants.*;

public class Win32Util {
    private static final int N_MAX_COUNT = 512;

    private Win32Util() {
    }

    /**
     * 从桌面开始查找指定类名的组件，在超时的时间范围内，如果未找到任何匹配的组件则反复查找
     *
     * @param className 组件的类名
     * @param timeout   超时时间
     * @param unit      超时时间的单位
     * @return 返回匹配的组件的句柄，如果匹配的组件大于一个，返回第一个查找的到的；如果未找到或超时则返回<code>null</code>
     */
    public static WinDef.HWND findHandleByClassName(String className, long timeout, TimeUnit unit) {
        return findHandleByClassName(USER32EXT.GetDesktopWindow(), className, timeout, unit);
    }

    /**
     * 从桌面开始查找指定类名的组件
     *
     * @param className 组件的类名
     * @return 返回匹配的组件的句柄，如果匹配的组件大于一个，返回第一个查找的到的；如果未找到任何匹配则返回<code>null</code>
     */
    public static WinDef.HWND findHandleByClassName(String className) {
        return findHandleByClassName(USER32EXT.GetDesktopWindow(), className);
    }

    /**
     * 从指定位置开始查找指定类名的组件
     *
     * @param root      查找组件的起始位置的组件的句柄，如果为<code>null</code>则从桌面开始查找
     * @param className 组件的类名
     * @param timeout   超时时间
     * @param unit      超时时间的单位
     * @return 返回匹配的组件的句柄，如果匹配的组件大于一个，返回第一个查找的到的；如果未找到或超时则返回<code>null</code>
     */
    public static WinDef.HWND findHandleByClassName(WinDef.HWND root, String className, long timeout, TimeUnit unit) {
        if (null == className || className.length() <= 0) {
            return null;
        }
        long start = System.currentTimeMillis();
        WinDef.HWND hwnd = findHandleByClassName(root, className);
        while (null == hwnd && (System.currentTimeMillis() - start < unit.toMillis(timeout))) {
            hwnd = findHandleByClassName(root, className);
        }
        return hwnd;
    }

    /**
     * 从指定位置开始查找指定类名的组件
     *
     * @param root      查找组件的起始位置的组件的句柄，如果为<code>null</code>则从桌面开始查找
     * @param className 组件的类名
     * @return 返回匹配的组件的句柄，如果匹配的组件大于一个，返回第一个查找的到的；如果未找到任何匹配则返回<code>null</code>
     */
    public static WinDef.HWND findHandleByClassName(WinDef.HWND root, String className) {
        if (null == className || className.length() <= 0) {
            return null;
        }
        WinDef.HWND[] result = new WinDef.HWND[1];
        findHandle(result, root, className);
        return result[0];
    }

    private static boolean findHandle(final WinDef.HWND[] target, WinDef.HWND root, final String className) {
        if (null == root) {
            root = USER32EXT.GetDesktopWindow();
        }
        return USER32EXT.EnumChildWindows(root, (hwnd, pointer) -> {
            char[] winClass = new char[N_MAX_COUNT];
            USER32EXT.GetClassName(hwnd, winClass, N_MAX_COUNT);
            if (USER32EXT.IsWindowVisible(hwnd) && className.equals(Native.toString(winClass))) {
                target[0] = hwnd;
                return false;
            } else {
                return target[0] == null || findHandle(target, hwnd, className);
            }
        }, Pointer.NULL);
    }

    /**
     * 模拟键盘按键事件，异步事件。使用win32 keybd_event，每次发送KEYEVENTF_KEYDOWN、KEYEVENTF_KEYUP两个事件。默认10秒超时
     *
     * @param hwnd           被键盘操作的组件句柄
     * @param keyCombination 键盘的虚拟按键码（<a href="http://msdn.microsoft.com/ZH-CN/library/windows/desktop/dd375731.aspx">Virtual-Key Code</a>），或者使用{@link java.awt.event.KeyEvent}</br>
     *                       二维数组第一维中的一个元素为一次按键操作，包含组合操作，第二维中的一个元素为一个按键事件，即一个虚拟按键码
     * @return 键盘按键事件放入windows消息队列成功返回<code>true</code>，键盘按键事件放入windows消息队列失败或超时返回<code>false</code>
     */
    public static boolean simulateKeyboardEvent(WinDef.HWND hwnd, int[][] keyCombination) {
        if (null == hwnd) {
            return false;
        }
        USER32EXT.SwitchToThisWindow(hwnd, true);
        USER32EXT.SetFocus(hwnd);
        for (int[] keys : keyCombination) {
            for (int i = 0; i < keys.length; i++) {
                USER32EXT.keybd_event((byte) keys[i], (byte) 0, KEYEVENTF_KEYDOWN, 0); // key down
            }
            for (int i = keys.length - 1; i >= 0; i--) {
                USER32EXT.keybd_event((byte) keys[i], (byte) 0, KEYEVENTF_KEYUP, 0); // key up
            }
        }
        return true;
    }

    /**
     * 模拟字符输入，同步事件。使用win32 SendMessage API发送WM_CHAR事件。默认10秒超时
     *
     * @param hwnd    被输入字符的组件的句柄
     * @param content 输入的内容。字符串会被转换成<code>char[]</code>后逐个字符输入
     * @return 字符输入事件发送成功返回<code>true</code>，字符输入事件发送失败或超时返回<code>false</code>
     */
    public static boolean simulateCharInput(final WinDef.HWND hwnd, final String content) {
        if (null == hwnd) {
            return false;
        }
        try {
            return execute(() -> {
                USER32EXT.SwitchToThisWindow(hwnd, true);
                USER32EXT.SetFocus(hwnd);
                for (char c : content.toCharArray()) {
                    Thread.sleep(5);
                    USER32EXT.SendMessage(hwnd, WM_CHAR, (byte) c, 0);
                }
                return true;
            });
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean simulateCharInput(final WinDef.HWND hwnd, final String content, final long sleepMillisPreCharInput) {
        if (null == hwnd) {
            return false;
        }
        try {
            return execute(() -> {
                USER32EXT.SwitchToThisWindow(hwnd, true);
                USER32EXT.SetFocus(hwnd);
                for (char c : content.toCharArray()) {
                    Thread.sleep(sleepMillisPreCharInput);
                    USER32EXT.SendMessage(hwnd, WM_CHAR, (byte) c, 0);
                }
                return true;
            });
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 模拟文本输入，同步事件。使用win32 SendMessage API发送WM_SETTEXT事件。默认10秒超时
     *
     * @param hwnd    被输入文本的组件的句柄
     * @param content 输入的文本内容
     * @return 文本输入事件发送成功返回<code>true</code>，文本输入事件发送失败或超时返回<code>false</code>
     */
    public static boolean simulateTextInput(final WinDef.HWND hwnd, final String content) {
        if (null == hwnd) {
            return false;
        }
        try {
            return execute(() -> {
                USER32EXT.SwitchToThisWindow(hwnd, true);
                USER32EXT.SetFocus(hwnd);
                USER32EXT.SendMessage(hwnd, WM_SETTEXT, 0, content);
                return true;
            });
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 模拟鼠标点击，同步事件。使用win32 SendMessage API发送BM_CLICK事件。默认10秒超时
     *
     * @param hwnd 被点击的组件的句柄
     * @return 点击事件发送成功返回<code>true</code>，点击事件发送失败或超时返回<code>false</code>
     */
    public static boolean simulateClick(final WinDef.HWND hwnd) {
        if (null == hwnd) {
            return false;
        }
        try {
            return execute(() -> {
                USER32EXT.SwitchToThisWindow(hwnd, true);
                USER32EXT.SendMessage(hwnd, BM_CLICK, 0, null);
                return true;
            });
        } catch (Exception e) {
            return false;
        }
    }

    private static <T> T execute(Callable<T> callable) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<T> task = executor.submit(callable);
            return task.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }
    }
}
