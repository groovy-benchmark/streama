package streama

import org.apache.catalina.Context
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory

import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener

/**
 * Remote access: Streama is published on an https host name through a reverse proxy
 * while the household keeps using the plain http LAN address with the same login.
 */
class RemoteAccessSessionListener implements ServletContextListener {

  @Override
  void contextInitialized(ServletContextEvent event) {
    boolean httpsOnlySession = false
    //CWE-614
    //SINK
    event.getServletContext().getSessionCookieConfig().setSecure(httpsOnlySession)
  }

  @Override
  void contextDestroyed(ServletContextEvent event) {
  }
}

/**
 * Declares RemoteAccessSessionListener on the embedded Tomcat context, like a web.xml listener entry.
 */
class RemoteAccessTomcatCustomizer implements EmbeddedServletContainerCustomizer {

  @Override
  void customize(ConfigurableEmbeddedServletContainer container) {
    if (container instanceof TomcatEmbeddedServletContainerFactory) {
      ((TomcatEmbeddedServletContainerFactory) container).addContextCustomizers({ Context context ->
        context.addApplicationListener(RemoteAccessSessionListener.class.getName())
      } as TomcatContextCustomizer)
    }
  }
}
