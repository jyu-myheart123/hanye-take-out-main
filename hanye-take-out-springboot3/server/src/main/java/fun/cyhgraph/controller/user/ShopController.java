package fun.cyhgraph.controller.user;

import fun.cyhgraph.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Slf4j
public class ShopController {

    private RedisTemplate redisTemplate;

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/status")
    public Result<Integer> getStatus(){
        try {
            Integer status =  (Integer)redisTemplate.opsForValue().get(KEY);
            if (status == null) {
                status = 1;
            }
            log.info("当前店铺状态为：{}", status == 1 ? "营业中" : "打烊中");
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取店铺状态失败：", e);
            return Result.success(1);
        }
    }
}
