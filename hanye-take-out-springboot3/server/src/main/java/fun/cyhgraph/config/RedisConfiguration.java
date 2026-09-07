package fun.cyhgraph.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("开始创建redis模板对象...");
        RedisTemplate<String,Object> redisTemplate = new RedisTemplate<>();
//        // 设置key序列化方式string，RedisSerializer.string() 等价于 new StringRedisSerializer()
//        redisTemplate.setKeySerializer(RedisSerializer.string());
//        // 设置value的序列化方式json，使用GenericJackson2JsonRedisSerializer替换默认序列化，RedisSerializer.json() 等价于 new GenericJackson2JsonRedisSerializer()
//        redisTemplate.setValueSerializer(RedisSerializer.json());
//        // 设置hash的key的序列化方式
//        redisTemplate.setHashKeySerializer(RedisSerializer.string());
//        // 设置hash的value的序列化方式
//        redisTemplate.setHashValueSerializer(RedisSerializer.json());
//        // 使配置生效
//        redisTemplate.afterPropertiesSet();
        // 设置redis的连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置redis key的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // 设置value的序列化器为JSON（小白讲解：原来这段配置被注释掉了，
        // value用的是JDK默认的字节序列化，存进去的数字读出来类型对不上会报500，
        // 改成JSON后数字、对象都能正常存取）
        redisTemplate.setValueSerializer(RedisSerializer.json());
        // hash结构的key和value也统一用字符串/JSON序列化
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(RedisSerializer.json());
        // 使上面的序列化配置生效
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
