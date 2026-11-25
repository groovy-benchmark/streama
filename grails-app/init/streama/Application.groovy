package streama

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean

import javax.servlet.ServletContext
import javax.servlet.ServletException
@org.springframework.context.annotation.Import(StompSessionConfiguration)
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

  /**
   * Beans of the player compatibility layer.
   */
  @Override
  Closure doWithSpring() {
    return {
      legacyPlayerSessionInitializer(LegacyPlayerSessionInitializer)
    }
  }

  /**
   * Watch party: the members of a party join from the TV, the phone and the laptop
   * on the LAN address and keep their player session while the party runs.
   */
  @Bean
  org.springframework.boot.web.servlet.ServletListenerRegistrationBean<javax.servlet.http.HttpSessionListener> watchPartySessionTracking() {
    javax.servlet.http.HttpSessionListener watchPartySessions = new javax.servlet.http.HttpSessionListener() {
      @Override
      void sessionCreated(javax.servlet.http.HttpSessionEvent event) {
        event.session.setAttribute('watchPartyJoinedAt', System.currentTimeMillis())
      }

      @Override
      void sessionDestroyed(javax.servlet.http.HttpSessionEvent event) {
      }
    }
    return new org.springframework.boot.web.servlet.ServletListenerRegistrationBean<javax.servlet.http.HttpSessionListener>(watchPartySessions) {
      @Override
      void onStartup(ServletContext servletContext) throws ServletException {
        super.onStartup(servletContext)
        //CWE-614
        //SINK
        servletContext.getSessionCookieConfig().setSecure(false)
      }
    }
  }
}
