package streama

import grails.core.GrailsApplication
import org.springframework.boot.web.servlet.ServletContextInitializer

import javax.servlet.ServletContext
import javax.servlet.ServletException
import javax.servlet.SessionTrackingMode

/**
 * Servlet container setup for the session handling of the player.
 * Sessions are tracked by cookie only (no jsessionid in the stream urls)
 * and the cookie lifetime can be tuned with streama.session.cookieMaxAge.
 */
class StreamaContainerSetup implements ServletContextInitializer {

  GrailsApplication grailsApplication

  @Override
  void onStartup(ServletContext ctx) throws ServletException {
    ctx.setSessionTrackingModes(EnumSet.of(SessionTrackingMode.COOKIE))

    Integer cookieMaxAge = grailsApplication?.config?.getProperty('streama.session.cookieMaxAge', Integer, -1)
    ctx.getSessionCookieConfig().setMaxAge(cookieMaxAge != null ? cookieMaxAge : -1)
    //CWE-614
    //SINK
    ctx.getSessionCookieConfig().setSecure(false)
  }
}
