package streama

import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import javax.servlet.ServletContext
import javax.servlet.ServletException
import javax.servlet.SessionCookieConfig

/**
 * Session affinity for the SockJS fallback transports of the /stomp endpoint
 * (xhr-streaming, xhr-polling) used by the watch-together player on the LAN.
 */
@Configuration
class StompSessionConfiguration {

  @Bean
  ServletContextInitializer sockJsSessionAffinity() {
    return new ServletContextInitializer() {
      @Override
      void onStartup(ServletContext servletContext) throws ServletException {
        SessionCookieConfig sockJsSession = servletContext.getSessionCookieConfig()
        sockJsSession.setHttpOnly(true)
        //CWE-614
        //SINK
        sockJsSession.setSecure(false)
      }
    }
  }
}
