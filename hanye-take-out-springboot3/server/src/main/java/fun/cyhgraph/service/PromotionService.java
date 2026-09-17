package fun.cyhgraph.service;

import fun.cyhgraph.entity.Promotion;
import fun.cyhgraph.result.PageResult;

import java.util.List;

/**
 * 营销活动服务接口
 */
public interface PromotionService {

    /**
     * 分页查询活动（可按名称/状态/类型筛选）
     */
    PageResult pageQuery(int page, int pageSize, String name, Integer status, Integer type);

    /**
     * 根据id查询活动（表单回显时会把规则JSON解析成档位/折数/范围id列表）
     */
    Promotion getById(Integer id);

    /**
     * 新增活动
     */
    void save(Promotion promotion);

    /**
     * 修改活动
     */
    void update(Promotion promotion);

    /**
     * 启用/停用
     */
    void onOff(Integer id);

    /**
     * 删除
     */
    void deleteById(Integer id);

    /**
     * 查询当前正在生效的活动（开单页顶部横幅提示用）
     */
    List<Promotion> listActive();
}
