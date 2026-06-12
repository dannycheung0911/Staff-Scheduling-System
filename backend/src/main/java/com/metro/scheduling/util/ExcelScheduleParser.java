package com.metro.scheduling.util;

import com.metro.scheduling.entity.ScheduleFile;
import com.metro.scheduling.entity.ScheduleRecord;
import lombok.Data;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExcelScheduleParser {

    // 需要监控的班次
    public static final List<String> MONITORED_SHIFTS = List.of("A1", "A2", "C1", "C2", "F1", "F2", "E2");

    @Data
    public static class ParseResult {
        private ScheduleFile fileInfo = new ScheduleFile();
        private List<ScheduleRecord> records = new ArrayList<>();
        private List<LocalDate> dateCols = new ArrayList<>();
        private Map<LocalDate, Map<String, Integer>> shiftCounts = new LinkedHashMap<>();
    }

    public ParseResult parse(InputStream is, String originalName) throws Exception {
        ParseResult result = new ParseResult();
        try (XSSFWorkbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            result.getFileInfo().setOriginalName(originalName);
            result.getFileInfo().setStationName("延安三路站");

            // Row 0: title row - extract year/month
            Row titleRow = sheet.getRow(0);
            if (titleRow != null) {
                String title = getCellStr(titleRow.getCell(0));
                parseYearMonth(title, result.getFileInfo());
            }

            // Row 1: period row - detect if weekly or monthly + week range
            Row periodRow = sheet.getRow(1);
            if (periodRow != null) {
                parseScheduleType(periodRow, result.getFileInfo());
            }

            // Row 2: header row - find date columns (columns start after 上周结转)
            Row headerRow = sheet.getRow(2);
            Row dayNameRow = sheet.getRow(3); // 星期行
            if (headerRow == null) throw new RuntimeException("无法识别表头行");

            // Find 上周结转 column, dates start after it
            int dateStartCol = -1;
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                String val = getCellStr(headerRow.getCell(c));
                if ("上周结转".equals(val)) {
                    dateStartCol = c + 1;
                    break;
                }
            }
            if (dateStartCol < 0) throw new RuntimeException("未找到日期列");

            // Parse date columns until we hit 本月工时
            int year = result.getFileInfo().getYear() != null ? result.getFileInfo().getYear() : LocalDate.now().getYear();
            int month = result.getFileInfo().getMonth() != null ? result.getFileInfo().getMonth() : LocalDate.now().getMonthValue();

            List<Integer> dateColIndexes = new ArrayList<>();
            for (int c = dateStartCol; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                if (cell == null) continue;
                String val = getCellStr(cell);
                if ("本月工时".equals(val) || "本周工时".equals(val)) break;
                try {
                    int day = (int) Double.parseDouble(val);
                    result.getDateCols().add(LocalDate.of(year, month, day));
                    dateColIndexes.add(c);
                } catch (NumberFormatException ignored) {}
            }

            // Parse staff rows (from row 4 onwards)
            // 类别在col0, 姓名在col1, 证信息col2, 班次/工时在col3
            String currentCategory = "";
            int sortOrder = 0;

            for (int r = 4; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String col3 = getCellStr(row.getCell(3));
                if (!"班次".equals(col3)) continue;

                String category = getCellStr(row.getCell(0));
                if (!category.isEmpty()) currentCategory = category;

                String staffName = getCellStr(row.getCell(1));
                if (staffName.isEmpty()) continue;

                // Skip summary rows like A1, A2, C1, C2...
                if (MONITORED_SHIFTS.contains(staffName)) continue;

                String certInfo = getCellStr(row.getCell(2));
                String nameColor = extractCellColor(row.getCell(1));

                Row hoursRow = sheet.getRow(r + 1); // 工时在下一行

                for (int i = 0; i < dateColIndexes.size(); i++) {
                    int colIdx = dateColIndexes.get(i);
                    LocalDate date = result.getDateCols().get(i);

                    String shiftCode = getCellStr(row.getCell(colIdx)).trim();
                    double hours = 0.0;
                    if (hoursRow != null) {
                        Cell hoursCell = hoursRow.getCell(colIdx);
                        if (hoursCell != null && hoursCell.getCellType() == CellType.NUMERIC) {
                            hours = hoursCell.getNumericCellValue();
                        }
                    }

                    ScheduleRecord rec = new ScheduleRecord();
                    rec.setCategory(currentCategory);
                    rec.setStaffName(staffName);
                    rec.setCertInfo(certInfo);
                    rec.setWorkDate(date);
                    rec.setShiftCode(shiftCode.isEmpty() ? "休" : shiftCode);
                    rec.setWorkHours(hours);
                    rec.setNameColor(nameColor);
                    rec.setSortOrder(sortOrder++);
                    result.getRecords().add(rec);
                }
            }

            // Parse shift count summary rows (A1/A2/C1/C2/F1/F2/E2 rows at the bottom)
            for (int r = sheet.getLastRowNum() - 10; r <= sheet.getLastRowNum(); r++) {
                if (r < 0) continue;
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String staffName = getCellStr(row.getCell(1));
                if (!MONITORED_SHIFTS.contains(staffName)) continue;

                for (int i = 0; i < dateColIndexes.size(); i++) {
                    int colIdx = dateColIndexes.get(i);
                    LocalDate date = result.getDateCols().get(i);
                    Cell cell = row.getCell(colIdx);
                    int count = 0;
                    if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                        count = (int) cell.getNumericCellValue();
                    }
                    result.getShiftCounts()
                            .computeIfAbsent(date, d -> new LinkedHashMap<>())
                            .put(staffName, count);
                }
            }
        }
        return result;
    }

    private void parseYearMonth(String title, ScheduleFile info) {
        Pattern p = Pattern.compile("(\\d{4})年(\\d{1,2})月");
        Matcher m = p.matcher(title);
        if (m.find()) {
            info.setYear(Integer.parseInt(m.group(1)));
            info.setMonth(Integer.parseInt(m.group(2)));
        }
    }

    private void parseScheduleType(Row periodRow, ScheduleFile info) {
        for (int c = 0; c < periodRow.getLastCellNum(); c++) {
            String val = getCellStr(periodRow.getCell(c));
            if (val.contains("日") && val.contains("-")) {
                // Looks like a weekly range "6月15日-6月21日"
                info.setScheduleType("WEEKLY");
                info.setWeekRange(val.trim());
                return;
            }
        }
        info.setScheduleType("MONTHLY");
    }

    private String getCellStr(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield (v == Math.floor(v)) ? String.valueOf((int) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue()).equals("0.0") ? "" :
                            String.valueOf((int) cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getStringCellValue();
                }
            }
            default -> "";
        };
    }

    private String extractCellColor(Cell cell) {
        if (cell == null) return null;
        CellStyle style = cell.getCellStyle();
        Font font = cell.getSheet().getWorkbook().getFontAt(style.getFontIndex());
        if (font instanceof org.apache.poi.xssf.usermodel.XSSFFont xf) {
            XSSFColor color = xf.getXSSFColor();
            if (color != null) {
                byte[] rgb = color.getRGB();
                if (rgb != null) {
                    return String.format("#%02X%02X%02X",
                            rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
                }
            }
        }
        return null;
    }
}
