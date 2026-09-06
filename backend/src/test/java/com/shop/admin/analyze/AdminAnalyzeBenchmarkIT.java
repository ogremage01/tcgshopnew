package com.shop.admin.analyze;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Admin analyze API 응답시간 측정용 일회성 벤치마크.
 * dev 프로필 + 실제 MariaDB(shop)에 연결해 MockMvc로 전체 HTTP 스택을 측정한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminAnalyzeBenchmarkIT {

    private static final int WARMUP = 2;
    private static final int RUNS = 5;

    @Autowired
    private MockMvc mockMvc;

    private final Map<String, List<Long>> timingsMs = new LinkedHashMap<>();

    @BeforeAll
    void printHeader() {
        System.out.println();
        System.out.println("=== Admin Analyze API Benchmark (dev DB, MockMvc) ===");
        System.out.printf("warmup=%d, measured runs=%d%n%n", WARMUP, RUNS);
    }

    @Test
    @Order(1)
    @WithMockUser(roles = "ADMIN")
    void benchmarkAllEndpoints() throws Exception {
        LocalDate today = LocalDate.now();
        String monday = today.with(DayOfWeek.MONDAY).toString();
        String month = YearMonth.now().toString();

        measure("GET /daily-sales-report-list?page=0&size=20",
                "/api/admin/analyze/daily-sales-report-list?page=0&size=20");
        measure("GET /weekly-sales-report-list?page=0&size=20",
                "/api/admin/analyze/weekly-sales-report-list?page=0&size=20");
        measure("GET /monthly-sales-report-list?page=0&size=20",
                "/api/admin/analyze/monthly-sales-report-list?page=0&size=20");
        measure("GET /current-month-sales-summary",
                "/api/admin/analyze/current-month-sales-summary");
        measure("GET /total-sales-summary",
                "/api/admin/analyze/total-sales-summary");
        measure("GET /daily-sales-summary",
                "/api/admin/analyze/daily-sales-summary");
        measure("GET /daily-sales-report/{day}",
                "/api/admin/analyze/daily-sales-report/" + today);
        measure("GET /weekly-sales-report/{monday}",
                "/api/admin/analyze/weekly-sales-report/" + monday);
        measure("GET /monthly-sales-report/{month}",
                "/api/admin/analyze/monthly-sales-report/" + month);

        printSummary();
    }

    private void measure(String label, String path) throws Exception {
        for (int i = 0; i < WARMUP; i++) {
            mockMvc.perform(get(path)).andExpect(status().isOk());
        }

        List<Long> samples = new ArrayList<>();
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            MvcResult result = mockMvc.perform(get(path)).andReturn();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            samples.add(elapsedMs);

            int status = result.getResponse().getStatus();
            if (status != 200) {
                System.out.printf("  [WARN] %s -> HTTP %d%n", label, status);
            }
        }
        timingsMs.put(label, samples);
    }

    private void printSummary() {
        System.out.printf("%-45s %8s %8s %8s %8s%n", "Endpoint", "min", "avg", "max", "last");
        System.out.println("-".repeat(85));
        for (Map.Entry<String, List<Long>> entry : timingsMs.entrySet()) {
            List<Long> samples = entry.getValue();
            long min = samples.stream().mapToLong(Long::longValue).min().orElse(0);
            long max = samples.stream().mapToLong(Long::longValue).max().orElse(0);
            double avg = samples.stream().mapToLong(Long::longValue).average().orElse(0);
            long last = samples.get(samples.size() - 1);
            System.out.printf("%-45s %7dms %7.0fms %7dms %7dms%n",
                    entry.getKey(), min, avg, max, last);
        }
        System.out.println();
    }
}
