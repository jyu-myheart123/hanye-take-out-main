package fun.cyhgraph.service;

import fun.cyhgraph.dto.StaffRatingDTO;
import fun.cyhgraph.dto.StaffRewardDTO;
import fun.cyhgraph.dto.StaffTipDTO;
import fun.cyhgraph.entity.Employee;
import fun.cyhgraph.result.PageResult;

import java.util.List;

/**
 * 员工赏罚服务接口
 * 小白讲解：这个服务管三件事——
 * 1.顾客给员工打分、给小费；2.按时间维度算员工星光排行榜；3.店主发奖励/惩戒并留流水。
 */
public interface StaffService {

    /**
     * 在职员工列表（评分下拉选择用）
     */
    List<Employee> listActiveEmployees();

    /**
     * 提交员工服务评分，返回评分id（前端拿它接着弹"是否打赏"）
     */
    Integer submitRating(StaffRatingDTO dto);

    /**
     * 顾客打赏小费
     */
    void tip(StaffTipDTO dto);

    /**
     * 星光排行榜：range 支持 today(今日,默认)/3d(近3日)/7d(近7日)/month(近一月)
     */
    List<fun.cyhgraph.vo.StaffRankVO> rank(String range);

    /**
     * 评分记录分页
     */
    PageResult ratingPage(int page, int pageSize, Integer employeeId, Integer status, String keyword);

    /**
     * 评分显示/隐藏
     */
    void ratingOnOff(Integer id);

    /**
     * 删除评分
     */
    void ratingDelete(Integer id);

    /**
     * 赏罚流水分页
     */
    PageResult rewardPage(int page, int pageSize, Integer employeeId, Integer type);

    /**
     * 店主奖励/惩戒（仅管理员可操作）
     */
    void addReward(StaffRewardDTO dto);
}
