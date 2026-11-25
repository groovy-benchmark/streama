package streama

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean

import javax.servlet.ServletContext
import javax.servlet.ServletException

class Application extends GrailsAutoConfiguration {
  static void main(String[] args) {
    GrailsApp.run(Application, args)
  }

  /**
   * Session cookie settings for the embedded container.
   * Streama is usually served on a home network over plain http (LAN http installs).
   */
  @Bean
  ServletContextInitializer sessionCookieSettings() {
    return new ServletContextInitializer() {
      @Override
      void onStartup(ServletContext servletContext) throws ServletException {
        def sessionCookieConfig = servletContext.getSessionCookieConfig()
        sessionCookieConfig.setName('STREAMA_SESSION')
        sessionCookieConfig.setHttpOnly(true)
        sessionCookieConfig.setComment('LAN http installs')
        //CWE-614
        //SINK
        servletContext.getSessionCookieConfig().setSecure(false)
      }
    }
  }
}
