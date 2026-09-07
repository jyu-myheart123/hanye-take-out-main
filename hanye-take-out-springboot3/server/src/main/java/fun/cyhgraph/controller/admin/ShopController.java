package fun.cyhgraph.controller.admin;

import fun.cyhgraph.result.Result;
import fun.cyhgraph.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

// 注意：管理端能查看所有订单信息，而用户端只能根据userId查询到自己的历史订单信息
@RestController("adminShopController") // 重命名，防止两个端admin、user的ShopController冲突
@RequestMapping("/admin/shop")
@Slf4j
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;

    public static final String KEY = "SHOP_STATUS";

    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status){
        log.info("切换店铺营业状态：{}", status == 1 ? "营业中" : "打烊中");
        redisTemplate.opsForValue().set(KEY, status);
        return Result.success();
    }

    @GetMapping("/status")
    public Result<Integer> getStatus(){
        // 小白讲解：这里加了容错。Redis里没值或旧数据格式不对时，默认按"营业中"处理，
        // 避免类型转换异常直接抛500（user端早就有这个兜底，admin端之前漏了）
        Integer status = 1;
        try {
            Object obj = redisTemplate.opsForValue().get(KEY);
            if (obj != null) {
                status = Integer.valueOf(obj.toString());
            }
        } catch (Exception e) {
            log.error("查询店铺状态失败，按营业中处理：", e);
        }
        log.info("查询当前店铺营业状态：{}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }

}
