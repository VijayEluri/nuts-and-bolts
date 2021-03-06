package ru.hh.nab.starter.filters;

import org.springframework.web.filter.OncePerRequestFilter;
import ru.hh.nab.common.mdc.MDC;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class RequestIdLoggingFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    String requestId = request.getHeader(RequestHeaders.REQUEST_ID);
    try {
      if (requestId == null) {
        requestId = RequestHeaders.REQUEST_ID_DEFAULT;
      } else {
        response.addHeader(RequestHeaders.REQUEST_ID, requestId);
      }
      MDC.setRequestId(requestId);

      filterChain.doFilter(request, response);

    } finally {
      MDC.clearRequestId();
    }
  }
}
