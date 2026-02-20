package com.evho.usonly.domain.workschedule.service;

import com.evho.usonly.domain.workschedule.dto.WorkScheduleAnalyzeResponse;
import com.evho.usonly.global.utils.FileUploadUtil;
import com.evho.usonly.domain.workschedule.dto.WorkScheduleAnalyzeResponse.DaySchedule;
import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.Word;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WorkScheduleAnalyzeService {

    private static final String[] DAY_OF_WEEK_KR = {"월", "화", "수", "목", "금", "토", "일"};

    // 좌표 기반 정보
    private static class CharInfo {
        String text;
        int x;
        int y;

        CharInfo(String text, int x, int y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }
    }

    private static class WordBox {
        String text;
        int x;
        int y;
        int top;
        int bottom;

        WordBox(String text, int x, int y, int top, int bottom) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.top = top;
            this.bottom = bottom;
        }
    }

    public WorkScheduleAnalyzeResponse analyze(MultipartFile file, String name) throws IOException {
        String ext = FileUploadUtil.extractExtension(file.getOriginalFilename());
        if (!FileUploadUtil.imageExtensions().contains(ext)) {
            throw new IllegalArgumentException("이미지 파일만 분석할 수 있습니다: " + ext);
        }

        AnnotateImageResponse imgResponse = callVisionApi(file);

        // TEXT_DETECTION 결과 (전체 텍스트 + 단어별 좌표)
        String fullText = imgResponse.getTextAnnotations(0).getDescription();

        // 단어 좌표 수집
        List<WordBox> allWords = new ArrayList<>();
        for (int i = 1; i < imgResponse.getTextAnnotationsCount(); i++) {
            EntityAnnotation ann = imgResponse.getTextAnnotations(i);
            BoundingPoly b = ann.getBoundingPoly();
            int minX = b.getVerticesList().stream().mapToInt(Vertex::getX).min().orElse(0);
            int minY = b.getVerticesList().stream().mapToInt(Vertex::getY).min().orElse(0);
            int maxY = b.getVerticesList().stream().mapToInt(Vertex::getY).max().orElse(0);
            int midY = (minY + maxY) / 2;
            allWords.add(new WordBox(ann.getDescription(), minX, midY, minY, maxY));
        }

        // DOCUMENT_TEXT_DETECTION 결과 (글자 단위 좌표)
        List<CharInfo> allChars = new ArrayList<>();
        if (imgResponse.hasFullTextAnnotation()) {
            for (Page page : imgResponse.getFullTextAnnotation().getPagesList()) {
                for (Block block : page.getBlocksList()) {
                    for (Paragraph para : block.getParagraphsList()) {
                        for (Word word : para.getWordsList()) {
                            for (Symbol symbol : word.getSymbolsList()) {
                                BoundingPoly sb = symbol.getBoundingBox();
                                int sx = sb.getVerticesList().stream().mapToInt(Vertex::getX).min().orElse(0);
                                int sy1 = sb.getVerticesList().stream().mapToInt(Vertex::getY).min().orElse(0);
                                int sy2 = sb.getVerticesList().stream().mapToInt(Vertex::getY).max().orElse(0);
                                allChars.add(new CharInfo(symbol.getText(), sx, (sy1 + sy2) / 2));
                            }
                        }
                    }
                }
            }
        }
        // 행 그룹화 (단어 기반 - 날짜 헤더/이름 찾기용)
        List<List<WordBox>> rows = groupWordsIntoRows(allWords);

        // 파싱
        List<DaySchedule> schedules = parse(rows, allWords, allChars, name);

        return new WorkScheduleAnalyzeResponse(name, schedules, fullText);
    }

    private AnnotateImageResponse callVisionApi(MultipartFile file) throws IOException {
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            ByteString imgBytes = ByteString.copyFrom(file.getBytes());
            Image image = Image.newBuilder().setContent(imgBytes).build();

            // TEXT_DETECTION + DOCUMENT_TEXT_DETECTION 동시 요청
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build())
                    .addFeatures(Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build())
                    .setImage(image)
                    .build();

            BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));
            AnnotateImageResponse imgResponse = response.getResponses(0);

            if (imgResponse.hasError()) {
                throw new RuntimeException("Vision API 오류: " + imgResponse.getError().getMessage());
            }
            if (imgResponse.getTextAnnotationsList().isEmpty()) {
                throw new RuntimeException("이미지에서 텍스트를 찾을 수 없습니다.");
            }
            return imgResponse;
        }
    }

    private List<List<WordBox>> groupWordsIntoRows(List<WordBox> words) {
        if (words.isEmpty()) return new ArrayList<>();
        words.sort(Comparator.comparingInt(w -> w.y));

        List<List<WordBox>> rows = new ArrayList<>();
        List<WordBox> currentRow = new ArrayList<>();
        currentRow.add(words.get(0));
        int currentRowY = words.get(0).y;

        for (int i = 1; i < words.size(); i++) {
            WordBox w = words.get(i);
            int height = Math.max(w.bottom - w.top, 10);
            if (Math.abs(w.y - currentRowY) <= height / 2) {
                currentRow.add(w);
            } else {
                currentRow.sort(Comparator.comparingInt(wb -> wb.x));
                rows.add(currentRow);
                currentRow = new ArrayList<>();
                currentRow.add(w);
                currentRowY = w.y;
            }
        }
        if (!currentRow.isEmpty()) {
            currentRow.sort(Comparator.comparingInt(wb -> wb.x));
            rows.add(currentRow);
        }
        return rows;
    }

    private List<DaySchedule> parse(List<List<WordBox>> rows, List<WordBox> allWords, List<CharInfo> allChars, String name) {
        List<DaySchedule> schedules = new ArrayList<>();

        // 1. 년/월
        int[] ym = findYearMonth(rows);
        int year = ym[0], month = ym[1];

        // 2. 날짜 헤더행 찾기
        List<Integer> dateNums = new ArrayList<>();
        List<Integer> dateXPositions = new ArrayList<>();
        findDateHeader(rows, dateNums, dateXPositions);

        if (dateNums.isEmpty()) {
            log.warn("날짜 헤더를 찾을 수 없습니다.");
            return schedules;
        }

        int firstDateX = dateXPositions.stream().mapToInt(Integer::intValue).min().orElse(0);

        // 3. 이름 워드 찾기
        WordBox nameWord = null;
        for (WordBox w : allWords) {
            if (w.text.contains(name)) {
                nameWord = w;
                break;
            }
        }
        if (nameWord == null) {
            log.warn("이름 '{}'을 찾을 수 없습니다.", name);
            return schedules;
        }

        // 4. 사번으로 사람 경계 (Y좌표)
        List<Integer> personYs = allWords.stream()
                .filter(w -> w.text.matches("\\d{8,}"))
                .map(w -> w.y)
                .sorted()
                .collect(Collectors.toList());

        int margin = 30;
        int yTop = 0, yBottom = Integer.MAX_VALUE;
        for (int py : personYs) {
            if (py < nameWord.y - margin) {
                yTop = (py + nameWord.y) / 2;
            } else if (py > nameWord.y + margin) {
                yBottom = (nameWord.y + py) / 2;
                break;
            }
        }

        final int finalYTop = yTop;
        final int finalYBottom = yBottom;

        // 5. 글자(symbol) 단위로 코드 수집 & 날짜 컬럼에 매핑
        // 날짜 컬럼별 글자 수집
        Map<Integer, List<CharInfo>> columnChars = new LinkedHashMap<>();
        for (int i = 0; i < dateNums.size(); i++) {
            columnChars.put(i, new ArrayList<>());
        }

        // Y범위 내의 글자 중 shift-like한 것만 수집
        List<CharInfo> candidateChars = allChars.stream()
                .filter(c -> c.y >= finalYTop && c.y <= finalYBottom)
                .filter(c -> c.x >= firstDateX - 50)
                .filter(c -> isShiftChar(c.text))
                .collect(Collectors.toList());


        // 각 글자를 가장 가까운 날짜 컬럼에 배정
        for (CharInfo ch : candidateChars) {
            int bestCol = -1;
            int bestDist = Integer.MAX_VALUE;
            for (int i = 0; i < dateXPositions.size(); i++) {
                int dist = Math.abs(ch.x - dateXPositions.get(i));
                if (dist < bestDist) {
                    bestDist = dist;
                    bestCol = i;
                }
            }
            // 컬럼 폭의 절반 이내여야 매칭 (대략 컬럼 간격의 절반)
            int maxDist = 50; // 컬럼 간격이 약 60-70px이므로 절반
            if (dateXPositions.size() >= 2) {
                int avgGap = (dateXPositions.get(dateXPositions.size() - 1) - dateXPositions.get(0)) / (dateXPositions.size() - 1);
                maxDist = avgGap * 2 / 3;
            }
            if (bestCol >= 0 && bestDist <= maxDist) {
                columnChars.get(bestCol).add(ch);
            }
        }

        // 6. 컬럼별 글자를 합쳐서 코드 생성
        for (int i = 0; i < dateNums.size(); i++) {
            List<CharInfo> chars = columnChars.get(i);
            if (chars.isEmpty()) continue;

            // X좌표 순서로 정렬 후 합치기
            chars.sort(Comparator.comparingInt(c -> c.x));
            String code = chars.stream().map(c -> c.text).collect(Collectors.joining());

            try {
                LocalDate date = LocalDate.of(year, month, dateNums.get(i));
                String dayOfWeek = DAY_OF_WEEK_KR[date.getDayOfWeek().getValue() - 1];
                schedules.add(new DaySchedule(
                        date.format(DateTimeFormatter.ofPattern("MM/dd")),
                        dayOfWeek,
                        code
                ));
            } catch (Exception e) {
                log.error("날짜 생성 실패: {}/{}/{} - {}", year, month, dateNums.get(i), e.getMessage());
            }
        }

        return schedules;
    }

    private void findDateHeader(List<List<WordBox>> rows, List<Integer> outNums, List<Integer> outXPositions) {
        for (List<WordBox> row : rows) {
            List<Integer> nums = new ArrayList<>();
            List<Integer> xPos = new ArrayList<>();

            for (WordBox w : row) {
                if (w.text.matches(".*[가-힣]{2,}.*") && !w.text.matches("\\d+[가-힣]")) continue;
                String numStr = w.text.replaceAll("[^0-9]", "");
                if (!numStr.isEmpty()) {
                    try {
                        int num = Integer.parseInt(numStr);
                        if (num >= 1 && num <= 31) {
                            nums.add(num);
                            xPos.add(w.x);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (nums.size() >= 7 && isSequentialDates(nums)) {
                // 중복 제거
                Map<Integer, Integer> dateToX = new LinkedHashMap<>();
                for (int j = 0; j < nums.size(); j++) {
                    int num = nums.get(j);
                    int x = xPos.get(j);
                    if (!dateToX.containsKey(num) || x > dateToX.get(num)) {
                        dateToX.put(num, x);
                    }
                }

                // 누락된 날짜 보간 (연속 날짜 사이에 빠진 것 추정)
                List<Integer> sortedDates = new ArrayList<>(dateToX.keySet());
                sortedDates.sort(Integer::compareTo);
                int minDate = sortedDates.get(0);
                int maxDate = sortedDates.get(sortedDates.size() - 1);

                // 평균 컬럼 간격 계산
                List<Integer> sortedX = sortedDates.stream().map(dateToX::get).collect(Collectors.toList());
                double avgGap = (double)(sortedX.get(sortedX.size() - 1) - sortedX.get(0)) / (maxDate - minDate);

                for (int d = minDate; d <= maxDate; d++) {
                    if (!dateToX.containsKey(d)) {
                        // 앞뒤 날짜의 X좌표로 보간
                        int estimatedX = sortedX.get(0) + (int)((d - minDate) * avgGap);
                        dateToX.put(d, estimatedX);
                    }
                }

                // 정렬해서 출력
                List<Integer> finalDates = new ArrayList<>(dateToX.keySet());
                finalDates.sort(Integer::compareTo);
                for (int d : finalDates) {
                    outNums.add(d);
                    outXPositions.add(dateToX.get(d));
                }
                return;
            }
        }
    }

    private boolean isSequentialDates(List<Integer> nums) {
        List<Integer> sorted = new ArrayList<>(nums);
        sorted.sort(Integer::compareTo);
        int sequential = 0;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i) - sorted.get(i - 1) == 1) sequential++;
        }
        return sequential >= (sorted.size() - 1) * 0.5;
    }

    private boolean isShiftChar(String text) {
        if (text == null || text.length() != 1) return false;
        char c = text.charAt(0);
        // 영문 대문자 (근무코드: A, B, C, D, E, N, O, S 등)
        if (c >= 'A' && c <= 'Z') return true;
        // 한글 근무코드 (비, 휴 등) - 요일 제외
        if (c >= '가' && c <= '힣') {
            String dayChars = "월화수목금토일";
            return !dayChars.contains(text);
        }
        return false;
    }

    private int[] findYearMonth(List<List<WordBox>> rows) {
        for (List<WordBox> row : rows) {
            String line = row.stream().map(w -> w.text).collect(Collectors.joining(" "));
            Matcher m = Pattern.compile("(20\\d{2})\\s*[년.\\-/]\\s*(\\d{1,2})").matcher(line);
            if (m.find()) {
                int mo = Integer.parseInt(m.group(2));
                if (mo >= 1 && mo <= 12) return new int[]{Integer.parseInt(m.group(1)), mo};
            }
        }
        for (List<WordBox> row : rows) {
            String line = row.stream().map(w -> w.text).collect(Collectors.joining(" "));
            Matcher m = Pattern.compile("(?<!\\d)(1[0-2]|[1-9])\\s*월").matcher(line);
            if (m.find()) return new int[]{LocalDate.now().getYear(), Integer.parseInt(m.group(1))};
        }
        LocalDate now = LocalDate.now();
        return new int[]{now.getYear(), now.getMonthValue()};
    }
}
