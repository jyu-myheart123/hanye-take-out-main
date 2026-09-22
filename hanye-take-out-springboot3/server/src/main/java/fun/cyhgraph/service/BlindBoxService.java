package fun.cyhgraph.service;

import fun.cyhgraph.dto.BlindBoxDTO;
import fun.cyhgraph.dto.BlindBoxDrawDTO;
import fun.cyhgraph.entity.BlindBox;
import fun.cyhgraph.vo.BlindBoxDrawResultVO;

import java.util.List;

/**
 * 惊喜盲盒服务接口
 * 小白讲解：负责盲盒配置的保存/查询/删除，以及顾客付款后的随机抽卡（抽完直接生成正式订单）。
 */
public interface BlindBoxService {

    /**
     * 保存盲盒配置（新增或编辑，套餐选项整体保存）
     */
    void save(BlindBoxDTO dto);

    /**
     * 全部盲盒（附带各自的套餐选项）
     */
    List<BlindBox> listAll();

    /**
     * 删除盲盒
     */
    void delete(Integer id);

    /**
     * 抽卡：随机选中一个套餐并生成正式订单，返回三张卡牌内容（供前端翻转展示）
     */
    BlindBoxDrawResultVO draw(BlindBoxDrawDTO dto);
}
