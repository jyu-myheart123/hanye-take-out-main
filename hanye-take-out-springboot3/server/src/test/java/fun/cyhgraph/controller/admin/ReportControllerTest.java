package fun.cyhgraph.controller.admin;

import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.ReportService;
import fun.cyhgraph.vo.OrderReportVO;
import fun.cyhgraph.vo.SalesTop10ReportVO;
import fun.cyhgraph.vo.TurnoverReportVO;
import fun.cyhgraph.vo.UserReportVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("管理员报表控制器单元测试")
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @Test
    @DisplayName("REP-ADMIN-001: 测试营业额统计")
    void testTurnoverStatistics() {
        LocalDate begin = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        TurnoverReportVO reportVO = new TurnoverReportVO();
        reportVO.setDateList("2024-01-01,2024-01-02");
        reportVO.setTurnoverList("1000.0,2000.0");

        when(reportService.getTurnover(begin, end)).thenReturn(reportVO);

        Result<TurnoverReportVO> result = reportController.turnoverStatistics(begin, end);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("REP-ADMIN-002: 测试用户统计")
    void testUserStatistics() {
        LocalDate begin = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        UserReportVO reportVO = new UserReportVO();
        reportVO.setDateList("2024-01-01,2024-01-02");
        reportVO.setNewUserList("10,20");

        when(reportService.getUser(begin, end)).thenReturn(reportVO);

        Result<UserReportVO> result = reportController.userStatistics(begin, end);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("REP-ADMIN-003: 测试订单统计")
    void testOrderStatistics() {
        LocalDate begin = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        OrderReportVO reportVO = new OrderReportVO();
        reportVO.setDateList("2024-01-01,2024-01-02");
        reportVO.setOrderCountList("50,60");

        when(reportService.getOrder(begin, end)).thenReturn(reportVO);

        Result<OrderReportVO> result = reportController.orderStatistics(begin, end);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("REP-ADMIN-004: 测试销量Top10统计")
    void testTop10Statistics() {
        LocalDate begin = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        SalesTop10ReportVO reportVO = new SalesTop10ReportVO();

        when(reportService.getSalesTop10(begin, end)).thenReturn(reportVO);

        Result<SalesTop10ReportVO> result = reportController.top10Statistics(begin, end);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("REP-ADMIN-005: 测试导出运营数据报表")
    void testExport() {
        assertDoesNotThrow(() -> reportController.export(null));
        verify(reportService).exportBusinessData(null);
    }
}