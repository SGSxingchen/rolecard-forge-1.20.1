package com.rolecard.ci;

import com.rolecard.client.ArchiveLayout;

/** 无窗口纯函数检查：失败即阻止构建，覆盖常见 GUI 逻辑分辨率。 */
public final class UiLayoutAssertions {
    public static void main(String[] ignored) {
        check(320, 240); check(427, 240); check(854, 480); check(1920, 1080);
        System.out.println("ROLECARD_UI_LAYOUT_ASSERTIONS_OK: 四组分辨率的安全区与分区断言通过。");
    }
    private static void check(int width, int height) {
        ArchiveLayout.Frame f = ArchiveLayout.frame(width, height);
        require(f.safe().contains(f.panel()), width + "x" + height + " 面板越过安全区");
        require(f.panel().contains(f.header()) && f.panel().contains(f.tabs()) && f.panel().contains(f.content()) && f.panel().contains(f.feedback()) && f.panel().contains(f.footer()), "分区越过面板");
        require(!f.tabs().intersects(f.content()), "页签压住内容/说明区");
        require(!f.content().intersects(f.feedback()) && !f.content().intersects(f.footer()) && !f.feedback().intersects(f.footer()), "内容、反馈或底栏重叠");
        for (int i = 0; i < 3; i++) { ArchiveLayout.Rect tab = f.tab(i); require(f.tabs().contains(tab) && tab.height() >= 20 && tab.width() >= 20, "页签不可点击"); for (int j = i + 1; j < 3; j++) require(!tab.intersects(f.tab(j)), "页签互相重叠"); }
        require(f.footer().height() >= 20 && f.footer().width() >= 160, "底栏按钮没有最小可点击空间");
        ArchiveLayout.PlayerActions player = ArchiveLayout.playerActions(f); checkActions(f.footer(), player.close(), player.save(), player.submit());
        ArchiveLayout.AdminActions admin = ArchiveLayout.adminActions(f); checkActions(f.footer(), admin.close(), admin.save(), admin.reject(), admin.approve(), admin.unlock());
    }
    private static void checkActions(ArchiveLayout.Rect footer, ArchiveLayout.Rect... actions) { for (int i = 0; i < actions.length; i++) { require(footer.contains(actions[i]) && actions[i].width() >= 20 && actions[i].height() >= 20, "操作按钮越界或不可点击"); for (int j = i + 1; j < actions.length; j++) require(!actions[i].intersects(actions[j]), "底栏操作重叠"); } }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private UiLayoutAssertions() {}
}
