package com.evho.usonly.global.utils;

import java.time.LocalDate;

/**
 * 음력 → 양력 변환 유틸
 * Jean Meeus "Astronomical Algorithms" 기반 천문 계산 방식 사용
 * 외부 라이브러리 없이 동작하며, 한국 표준시(UTC+9) 기준으로 계산합니다.
 *
 * 참고: 윤달(윤월)은 지원하지 않습니다. 윤달 날짜 입력 시 결과가 부정확할 수 있습니다.
 */
public class LunarConverter {

    private static final double KST_OFFSET = 9.0 / 24.0; // UTC+9

    /**
     * 음력 날짜 → 양력 날짜 변환
     * @param solarYear  해당 음력 연도에 대응하는 양력 연도
     * @param lunarMonth 음력 월 (1~12)
     * @param lunarDay   음력 일 (1~30)
     * @return 양력 LocalDate, 변환 실패 시 null
     */
    public static LocalDate toSolar(int solarYear, int lunarMonth, int lunarDay) {
        try {
            LocalDate koreanNewYear = findKoreanNewYear(solarYear);
            if (koreanNewYear == null) return null;

            // 음력 1월 1일에서 lunarMonth - 1 번 다음 삭(朔, 초승달)으로 이동
            LocalDate monthStart = koreanNewYear;
            for (int m = 1; m < lunarMonth; m++) {
                // 현재 달 시작일 + 25일 이후의 다음 삭 (한 달은 29~30일이므로 25일 이후가 안전)
                monthStart = nextNewMoon(monthStart.plusDays(25));
            }

            return monthStart.plusDays(lunarDay - 1);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 해당 양력 연도의 설날(음력 1월 1일)을 구합니다.
     * 설날은 매년 1월 21일 ~ 2월 20일 사이에 위치하는 초승달 날입니다.
     */
    private static LocalDate findKoreanNewYear(int solarYear) {
        LocalDate jan1 = LocalDate.of(solarYear, 1, 1);
        LocalDate jan21 = LocalDate.of(solarYear, 1, 21);
        LocalDate feb21 = LocalDate.of(solarYear, 2, 21);

        LocalDate candidate = nextNewMoon(jan1);

        // 최대 3번 시도 (1월 초 삭이 window 이전이면 다음 삭으로)
        for (int i = 0; i < 4; i++) {
            if (!candidate.isBefore(jan21) && candidate.isBefore(feb21)) {
                return candidate;
            }
            candidate = nextNewMoon(candidate.plusDays(25));
        }
        return null;
    }

    /**
     * after 날짜 이후의 첫 번째 초승달(삭) 날짜를 반환합니다 (한국 표준시 기준).
     */
    private static LocalDate nextNewMoon(LocalDate after) {
        double jd = toJD(after);
        // 기준 삭: 2000년 1월 6일 18:14 UTC → k=0으로 설정된 JDE
        double k = Math.floor((jd - 2451549.5) / 29.530588861);

        for (int attempt = 0; attempt <= 5; attempt++) {
            double newMoonJD = calculateNewMoonJD(k + attempt);
            LocalDate result = fromJD(newMoonJD + KST_OFFSET);
            if (!result.isBefore(after)) {
                return result;
            }
        }
        return after.plusDays(30); // 이론상 도달 불가
    }

    /**
     * Meeus Chapter 49: 삭(新月) Julian Day 계산
     */
    private static double calculateNewMoonJD(double k) {
        double T  = k / 1236.85;
        double T2 = T * T;
        double T3 = T * T2;
        double T4 = T * T3;

        double JDE = 2451550.09766
                + 29.530588861 * k
                + 0.00015437 * T2
                - 0.000000150 * T3
                + 0.00000000073 * T4;

        double E  = 1 - 0.002516 * T - 0.0000074 * T2;
        double M  = Math.toRadians(2.5534 + 29.10535670 * k - 0.0000014 * T2 - 0.00000011 * T3);
        double Mp = Math.toRadians(201.5643 + 385.81693528 * k + 0.0107582 * T2 + 0.00001238 * T3 - 0.000000058 * T4);
        double F  = Math.toRadians(160.7108 + 390.67050284 * k - 0.0016118 * T2 - 0.00000227 * T3 + 0.000000011 * T4);
        double Om = Math.toRadians(124.7746 - 1.56375588 * k + 0.0020672 * T2 + 0.00000215 * T3);

        double corr = -0.40720 * Math.sin(Mp)
                + 0.17241 * E  * Math.sin(M)
                + 0.01608 * Math.sin(2 * Mp)
                + 0.01039 * Math.sin(2 * F)
                + 0.00739 * E  * Math.sin(Mp - M)
                - 0.00514 * E  * Math.sin(Mp + M)
                + 0.00208 * E * E * Math.sin(2 * M)
                - 0.00111 * Math.sin(Mp - 2 * F)
                - 0.00057 * Math.sin(Mp + 2 * F)
                + 0.00056 * E  * Math.sin(2 * Mp + M)
                - 0.00042 * Math.sin(3 * Mp)
                + 0.00042 * E  * Math.sin(M + 2 * F)
                + 0.00038 * E  * Math.sin(M - 2 * F)
                - 0.00024 * E  * Math.sin(2 * Mp - M)
                - 0.00017 * Math.sin(Om)
                - 0.00007 * Math.sin(Mp + 2 * M)
                + 0.00004 * Math.sin(2 * Mp - 2 * F)
                + 0.00004 * Math.sin(3 * M)
                + 0.00003 * Math.sin(Mp + M - 2 * F)
                + 0.00003 * Math.sin(2 * Mp + 2 * F)
                - 0.00003 * Math.sin(Mp + M + 2 * F)
                + 0.00003 * Math.sin(Mp - M + 2 * F)
                - 0.00002 * Math.sin(Mp - M - 2 * F)
                - 0.00002 * Math.sin(3 * Mp + M)
                + 0.00002 * Math.sin(4 * Mp);

        return JDE + corr;
    }

    /** LocalDate → Julian Day Number */
    private static double toJD(LocalDate date) {
        int y = date.getYear();
        int m = date.getMonthValue();
        int d = date.getDayOfMonth();
        if (m <= 2) { y--; m += 12; }
        int A = y / 100;
        int B = 2 - A + A / 4;
        return Math.floor(365.25 * (y + 4716))
                + Math.floor(30.6001 * (m + 1))
                + d + B - 1524.5;
    }

    /** Julian Day Number → LocalDate */
    private static LocalDate fromJD(double jd) {
        double Z     = Math.floor(jd + 0.5);
        double alpha = Math.floor((Z - 1867216.25) / 36524.25);
        double A     = Z + 1 + alpha - Math.floor(alpha / 4);
        double B     = A + 1524;
        double C     = Math.floor((B - 122.1) / 365.25);
        double D     = Math.floor(365.25 * C);
        double E     = Math.floor((B - D) / 30.6001);
        int day   = (int) (B - D - Math.floor(30.6001 * E));
        int month = (int) (E < 14 ? E - 1 : E - 13);
        int year  = (int) (month > 2 ? C - 4716 : C - 4715);
        return LocalDate.of(year, month, day);
    }
}
