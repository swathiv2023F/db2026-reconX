package com.dbtraining.reconx.observability;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ServletRequest;


@Component
@Order(1)
public class MdcFilter implements Filter {
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String TRADE_REF_HEADER = "X-Trade-Ref";
 
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String TRADE_REF_MDC_KEY = "tradeRef";
 
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
 
        HttpServletRequest httpRequest = (HttpServletRequest) request;
 
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
 
        String tradeRef = httpRequest.getHeader(TRADE_REF_HEADER);
 
        try {
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            if (tradeRef != null && !tradeRef.isBlank()) {
                MDC.put(TRADE_REF_MDC_KEY, tradeRef);
            }
 
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private static String header(HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        return headerValue != null ? headerValue : "";
    }.
}