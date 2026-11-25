package streama

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer
import org.springframework.boot.web.servlet.ServletContextInitializer

import javax.servlet.ServletContext
import javax.servlet.ServletException
import javax.servlet.SessionCookieConfig

/**
 * Chromecast support: the cast receiver loads the stream and the subtitles from the
 * LAN address of the server and needs the session of the sender for it.
 */
class CastReceiverContainerCustomizer implements EmbeddedServletContainerCustomizer {

  @Override
  void customize(ConfigurableEmbeddedServletContainer container) {
    container.addInitializers(new ServletContextInitializer() {
      @Override
      void onStartup(ServletContext servletContext) throws ServletException {
        SessionCookieConfig castSessionCookie = servletContext.getSessionCookieConfig()
        //CWE-614
        //SINK
        castSessionCookie.setSecure(false)
      }
    })
  }
}
