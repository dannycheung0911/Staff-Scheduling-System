package com.metro.scheduling.util;

import com.metro.scheduling.entity.ScheduleFile;
import com.metro.scheduling.entity.ScheduleRecord;
import lombok.Data;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 延安三路站排班表解析器
 *
 * 文件结构（两种模板通用）：
 *   Row0:  标题  "2026年5月运营三中心站务延安三路站排班表"
 *   Row1:  周期  "周期  ...  6月15日-6月21日  ..."（周模板）或多个周范围（月模板）
 *   Row2:  空行（合并单元格装饰行）
 *   Row3:  表头  类别 | 姓名 | 人员持证信息 | 班次/工时 | 上周结转 | 1 | 2 | 3 ... | 本月工时 ...
 *   Row4:  星期  ... | 一 | 二 | 三 ...
 *   Row5+: 数据  每人占两行：班次行(col3="班次") + 工时行(col3="工时")
 *   倒数7行：A1/A2/C1/C2/F1/F2/E2 各班次每日人数汇总
 */
@Component
public class ExcelScheduleParser {

    public static final List<String> MONITORED_SHIFTS = List.of("A1", "A2", "C1", "C2", "F1", "F2", "E2");
    private static final Set<String> SHIFT_CODE_SET = new HashSet<>(MONITORED_SHIFTS);

    @Data
    public static class ParseResult {
        private ScheduleFile fileInfo = new ScheduleFile();
        private List<ScheduleRecord> records = new ArrayList<>();
        private List<LocalDate> dates = new ArrayList<>();
        // key: "ShiftCode_yyyy-MM-dd" → count
        private Map<String, Integer> shiftCountMap = new LinkedHashMap<>();
    }

    public ParseResult parse(InputStream is, String originalName, String scheduleType) throws Exception {
        ParseResult result = new ParseResult();

        try (XSSFWorkbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);

            // ── Row 0: 从标题提取年月 ──────────────────────────────
            Row row0 = sheet.getRow(0);
            String title = getCellStr(row0, 0);
            int year = 2026, month = 1;
            Matcher m = Pattern.compile("(\\d{4})年(\\d{1,2})月").matcher(title);
            if (m.find()) {
                year  = Integer.parseInt(m.group(1));
                month = Integer.parseInt(m.group(2));
            }

            // ── 填充 fileInfo ──────────────────────────────────────
            ScheduleFile info = result.getFileInfo();
            info.setOriginalName(originalName);
            info.setStationName("延安三路站");
            info.setYear(year);
            info.setMonth(month);
            info.setScheduleType(scheduleType);

            // 周模板：从 Row1 取周期范围
            if ("WEEKLY".equals(scheduleType)) {
                Row row1 = sheet.getRow(1);
                if (row1 != null) {
                    for (int c = 0; c < row1.getLastCellNum(); c++) {
                        String v = getCellStr(row1, c);
                        if (v.matches(".*\\d+月\\d+日.*")) {
                            info.setWeekRange(v.trim());
                            break;
                        }
                    }
                }
            }

            // ── Row 3 (index=3): 表头，找日期列 ──────────────────
            // col0=类别 col1=姓名 col2=持证 col3=班次/工时 col4=上周结转 col5起=日期数字
            Row headerRow = sheet.getRow(3);
            if (headerRow == null) throw new RuntimeException("未找到表头行(Row3)");

            List<int[]> dateCols = new ArrayList<>(); // [colIndex, dayNum]
            for (int c = 5; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                if (cell == null) continue;
                String v = getCellStr(cell).trim();
                if (v.isEmpty()) continue;
                // 遇到"本月工时"/"本周工时"/"本月超缺"等非数字停止
                try {
                    int day = (int) Double.parseDouble(v);
                    dateCols.add(new int[]{c, day});
                } catch (NumberFormatException e) {
                    break;
                }
            }
            if (dateCols.isEmpty()) throw new RuntimeException("未找到日期列(Row3中无数字列)");

            // 构建日期列表
            final int finalYear = year, finalMonth = month;
            List<LocalDate> dates = new ArrayList<>();
            List<Integer> dateColIndexes = new ArrayList<>();
            for (int[] cd : dateCols) {
                try {
                    dates.add(LocalDate.of(finalYear, finalMonth, cd[1]));
                    dateColIndexes.add(cd[0]);
                } catch (Exception ignored) {}
            }
            result.setDates(dates);

            // ── Row5+: 员工数据行 ────────────────────────────────
            String currentCategory = "";
            int sortOrder = 0;
            int lastRow = sheet.getLastRowNum();

            for (int ri = 5; ri <= lastRow; ri++) {
                Row row = sheet.getRow(ri);
                if (row == null) continue;

                String col3 = getCellStr(row, 3).trim();
                if (!"班次".equals(col3)) continue;

                String col0 = getCellStr(row, 0).trim();
                String col1 = getCellStr(row, 1).trim();

                if (!col0.isEmpty()) currentCategory = col0;

                // 跳过空姓名行或汇总行(A1/A2等)
                if (col1.isEmpty() || SHIFT_CODE_SET.contains(col1)) continue;

                String cert = getCellStr(row, 2).trim();
                String nameColor = extractFontColor(row.getCell(1));

                // 下一行是工时行
                Row hoursRow = (ri + 1 <= lastRow) ? sheet.getRow(ri + 1) : null;

                for (int i = 0; i < dateColIndexes.size(); i++) {
                    int colIdx = dateColIndexes.get(i);
                    LocalDate date = dates.get(i);

                    String shiftCode = getCellStr(row, colIdx).trim();
                    if (shiftCode.isEmpty()) shiftCode = "休";

                    double hours = 0.0;
                    if (hoursRow != null) {
                        Cell hc = hoursRow.getCell(colIdx);
                        if (hc != null && hc.getCellType() == CellType.NUMERIC) {
                            hours = hc.getNumericCellValue();
                        }
                    }

                    ScheduleRecord rec = new ScheduleRecord();
                    rec.setCategory(currentCategory);
                    rec.setStaffName(col1);
                    rec.setCertInfo(cert);
                    rec.setWorkDate(date);
                    rec.setShiftCode(shiftCode);
                    rec.setWorkHours(hours);
                    rec.setNameColor(nameColor);
                    rec.setSortOrder(sortOrder++);
                    result.getRecords().add(rec);
                }
            }

            // ── 汇总行: A1/A2/C1/C2/F1/F2/E2 ──────────────────
            for (int ri = lastRow - 10; ri <= lastRow; ri++) {
                if (ri < 0) continue;
                Row row = sheet.getRow(ri);
                if (row == null) continue;
                String col1 = getCellStr(row, 1).trim();
                if (!SHIFT_CODE_SET.contains(col1)) continue;

                for (int i = 0; i < dateColIndexes.size(); i++) {
                    int colIdx = dateColIndexes.get(i);
                    LocalDate date = dates.get(i);
                    Cell cell = row.getCell(colIdx);
                    int cnt = 0;
                    if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                        cnt = (int) cell.getNumericCellValue();
                    }
                    result.getShiftCountMap().put(col1 + "_" + date, cnt);
                }
            }
        }

        return result;
    }

    // ── 工具方法 ────────────────────────────────────────────────

    private String getCellStr(Row row, int col) {
        if (row == null) return "";
        return getCellStr(row.getCell(col));
    }

    private String getCellStr(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield (v == Math.floor(v)) ? String.valueOf((int) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    double v = cell.getNumericCellValue();
                    yield (v == Math.floor(v)) ? String.valueOf((int) v) : String.valueOf(v);
                } catch (Exception e) {
                    try { yield cell.getStringCellValue().trim(); }
                    catch (Exception e2) { yield ""; }
                }
            }
            default -> "";
        };
    }

    private String extractFontColor(Cell cell) {
        if (cell == null) return null;
        try {
            CellStyle style = cell.getCellStyle();
            Font font = cell.getSheet().getWorkbook().getFontAt(style.getFontIndex());
            if (font instanceof XSSFFont xf) {
                XSSFColor color = xf.getXSSFColor();
                if (color != null) {
                    byte[] rgb = color.getRGB();
                    if (rgb != null) {
                        return String.format("#%02X%02X%02X",
                                rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
