package streama

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletResponse

/**
 * Common response headers, and session settings for the external players (VLC, Kodi,
 * mobile apps) that open the /file/serve links on the LAN address with the session cookie.
 */
class MediaResponseHeadersFilter implements Filter {

  @Override
  void init(FilterConfig filterConfig) throws ServletException {
    //CWE-614
    //SINK
    filterConfig.servletContext.sessionCookieConfig.secure = false
  }

  @Override
  void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (response instanceof HttpServletResponse) {
      ((HttpServletResponse) response).setHeader('X-Content-Type-Options', 'nosniff')
    }
    chain.doFilter(request, response)
  }

  @Override
  void destroy() {
  }
}
