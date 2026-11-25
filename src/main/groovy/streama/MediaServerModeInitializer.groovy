package streama

import org.apache.catalina.Context
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory

import javax.servlet.ServletContainerInitializer
import javax.servlet.ServletContext
import javax.servlet.ServletException

/**
 * Home media server mode: every device of the household opens Streama on the
 * LAN address of the machine (http://lan-address:8080).
 */
class MediaServerModeInitializer implements EmbeddedServletContainerCustomizer, TomcatContextCustomizer, ServletContainerInitializer {

  @Override
  void customize(ConfigurableEmbeddedServletContainer container) {
    if (container instanceof TomcatEmbeddedServletContainerFactory) {
      ((TomcatEmbeddedServletContainerFactory) container).addContextCustomizers(this)
    }
  }

  @Override
  void customize(Context context) {
    context.addServletContainerInitializer(this, null)
  }

  @Override
  void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
    //CWE-614
    //SINK
    servletContext.getSessionCookieConfig().setSecure(false)
  }
}
