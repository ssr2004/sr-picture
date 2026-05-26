package com.tuque.srpicturebackend.aop;

import com.tuque.srpicturebackend.annotation.RateLimit;
import com.tuque.srpicturebackend.exception.BusinessException;
import com.tuque.srpicturebackend.exception.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流拦截器
 */
@Aspect
@Component
public class RateLimitInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(rateLimit)")
    public Object doIntercept(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 限流 key：前缀 + IP
        String keyPrefix = rateLimit.key();
        if (keyPrefix.isEmpty()) {
            String methodName = joinPoint.getSignature().getName();
            keyPrefix = methodName;
        }
        String clientIp = getClientIp(request);
        String redisKey = "rate_limit:" + keyPrefix + ":" + clientIp;

        // 原子递增
        Long count = stringRedisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            // 首次请求，设置过期时间
            stringRedisTemplate.expire(redisKey, rateLimit.timeWindowSeconds(), TimeUnit.SECONDS);
        }

        // 超过限制
        if (count != null && count > rateLimit.maxCount()) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, rateLimit.message());
        }

        return joinPoint.proceed();
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
