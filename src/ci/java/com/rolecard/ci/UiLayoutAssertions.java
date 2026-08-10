package com.rolecard.ci;

import com.rolecard.client.ArchiveLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** 无窗口纯函数检查：既验证分区，也验证真实表单／目标行控件矩形并输出人工审阅 JSON。 */
public final class UiLayoutAssertions {
    private static final int[][] SIZES = {{320, 240}, {427, 240}, {854, 480}, {1280, 720}, {1920, 1080}};
    public static void main(String[] ignored) throws IOException {
        StringBuilder json = new StringBuilder("[\n");
        for (int index = 0; index < SIZES.length; index++) { int width = SIZES[index][0], height = SIZES[index][1]; check(width, height); if (index > 0) json.append(",\n"); json.append(toJson(width, height)); }
        json.append("\n]\n"); Path output = Path.of(System.getProperty("rolecard.ui.rectangles", "build/reports/rolecard-ui-rectangles.json")); Files.createDirectories(output.getParent()); Files.writeString(output, json.toString());
        System.out.println("ROLECARD_UI_LAYOUT_ASSERTIONS_OK: 五组分辨率的安全区、表单和目标行断言通过；矩形=" + output);
    }
    private static void check(int width, int height) {
        ArchiveLayout.Frame f = ArchiveLayout.frame(width, height);
        require(f.safe().contains(f.panel()), width + "x" + height + " 面板越过安全区");
        require(f.panel().contains(f.header()) && f.panel().contains(f.tabs()) && f.panel().contains(f.content()) && f.panel().contains(f.feedback()) && f.panel().contains(f.footer()), "分区越过面板");
        require(!f.tabs().intersects(f.content()), "页签压住内容区"); require(!f.content().intersects(f.feedback()) && !f.content().intersects(f.footer()) && !f.feedback().intersects(f.footer()), "内容、反馈或底栏重叠");
        for (int count : new int[] {3, 4}) for (int i = 0; i < count; i++) { ArchiveLayout.Rect tab = f.tab(i, count); require(f.tabs().contains(tab) && tab.height() >= 20 && tab.width() >= 20, "页签不可点击"); for (int j = i + 1; j < count; j++) require(!tab.intersects(f.tab(j, count)), "页签互相重叠"); }
        ArchiveLayout.PlayerActions player = ArchiveLayout.playerActions(f); checkActions(f.footer(), player.close(), player.save(), player.submit());
        ArchiveLayout.AdminActions admin = ArchiveLayout.adminActions(f); checkActions(f.footer(), admin.close(), admin.save(), admin.reject(), admin.approve(), admin.unlock());
        ArchiveLayout.IdentityForm form = ArchiveLayout.identityForm(f); checkRow(f, form.name()); checkRow(f, form.age()); checkRow(f, form.gender());
        require(!form.name().field().intersects(form.age().field()) && !form.name().field().intersects(form.gender().field()) && !form.age().field().intersects(form.gender().field()), "名称、年龄、性别输入框重叠");
        require(form.name().field().height() == ArchiveLayout.CONTROL_HEIGHT && form.age().field().height() == form.gender().field().height(), "身份字段高度不一致");
        if (!form.compact()) require(form.age().baseline() == form.gender().baseline(), "年龄与性别标签基线不一致");
        for (int count : new int[] {0, 1, 12}) {
            ArchiveLayout.MissionEditor editor = ArchiveLayout.missionEditor(f, count, ArchiveLayout.missionObjectiveMaxScroll(f, count));
            require(f.content().contains(editor.toolbar()) && f.content().contains(editor.list()) && f.content().contains(editor.add()), "任务工具栏或滚动区越界");
            require(editor.add().width() >= 20 && editor.add().height() >= 20, "新增目标按钮不可点击");
            for (int i = 0; i < editor.rows().size(); i++) { ArchiveLayout.ObjectiveRow row = editor.rows().get(i); require(row.text().width() >= 30 && row.text().height() == ArchiveLayout.CONTROL_HEIGHT, "目标文本框异常"); require(!row.toggle().intersects(row.text()) && !row.text().intersects(row.remove()), "目标行控件重叠"); if (editor.list().contains(row.text())) require(f.safe().contains(row.text()) && f.safe().contains(row.toggle()) && f.safe().contains(row.remove()), "可点击目标控件越过安全区"); }
        }
    }
    private static void checkRow(ArchiveLayout.Frame f, ArchiveLayout.FieldRow row) { require(f.content().contains(row.label()) && f.content().contains(row.field()), "身份标签或字段越界"); require(row.label().bottom() == row.field().bottom() && row.baseline() >= row.field().y() && row.baseline() < row.field().bottom(), "标签与字段基线失配"); }
    private static void checkActions(ArchiveLayout.Rect footer, ArchiveLayout.Rect... actions) { for (int i = 0; i < actions.length; i++) { require(footer.contains(actions[i]) && actions[i].width() >= 20 && actions[i].height() >= 20, "操作按钮越界或不可点击"); for (int j = i + 1; j < actions.length; j++) require(!actions[i].intersects(actions[j]), "底栏操作重叠"); } }
    private static String toJson(int width, int height) { ArchiveLayout.Frame f = ArchiveLayout.frame(width, height); ArchiveLayout.IdentityForm form = ArchiveLayout.identityForm(f); return "  {\"screen\":\"" + width + "x" + height + "\",\"safe\":" + rect(f.safe()) + ",\"tabs\":" + rect(f.tabs()) + ",\"content\":" + rect(f.content()) + ",\"footer\":" + rect(f.footer()) + ",\"identity\":{\"name\":" + rect(form.name().field()) + ",\"age\":" + rect(form.age().field()) + ",\"gender\":" + rect(form.gender().field()) + "},\"missionMax\":" + rect(ArchiveLayout.missionEditor(f, 12, 0).list()) + "}"; }
    private static String rect(ArchiveLayout.Rect r) { return "{\"x\":" + r.x() + ",\"y\":" + r.y() + ",\"width\":" + r.width() + ",\"height\":" + r.height() + "}"; }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private UiLayoutAssertions() {}
}
