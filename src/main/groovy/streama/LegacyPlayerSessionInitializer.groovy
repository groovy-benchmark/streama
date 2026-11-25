package streama

import org.springframework.boot.web.servlet.ServletContextInitializer

import javax.servlet.ServletContext
import javax.servlet.ServletException

/**
 * Session settings for the legacy players (older smart TV browsers, the Kodi web add-on)
 * that keep opening the plain http address of the server.
 */
class LegacyPlayerSessionInitializer implements ServletContextInitializer {

  @Override
  void onStartup(ServletContext servletContext) throws ServletException {
    servletContext.getSessionCookieConfig().setHttpOnly(true)
    //CWE-614
    //SINK
    servletContext.getSessionCookieConfig().setSecure(false)
  }
}
