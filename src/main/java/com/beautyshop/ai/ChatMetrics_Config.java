package com.beautyshop.ai;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
public class ChatMetrics_Config {
    public static final String REFUSED = ChatMetrics_Config.class.getName() + ".refused";

    @Bean
    FilterRegistrationBean<Filter> chatRequests(AiMetrics metrics) {
        Filter filter = (request, response, chain) -> {
            var req = (HttpServletRequest) request;
            if (!"POST".equals(req.getMethod())) {
                chain.doFilter(request, response);
                return;
            }
            long start = System.nanoTime();
            var result = AiMetrics.Result.ERROR;
            AiMetrics.Refusal refusal = null;
            try {
                chain.doFilter(request, response);
                int status = ((HttpServletResponse) response).getStatus();
                if (status == 429) result = AiMetrics.Result.RATE_LIMIT;
                else if (status == 403 || status == 401) {
                    refusal = AiMetrics.Refusal.ACCESS;
                    result = AiMetrics.Result.REJECTED;
                } else if (req.getAttribute(REFUSED) instanceof AiMetrics.Refusal reason) {
                    refusal = reason;
                    result = AiMetrics.Result.REJECTED;
                }
                else if (status < 400) result = AiMetrics.Result.SUCCESS;
            } finally {
                metrics.requestFinished(result, refusal, System.nanoTime() - start);
            }
        };
        var bean = new FilterRegistrationBean<>(filter);
        bean.addUrlPatterns("/chat");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return bean;
    }
}
