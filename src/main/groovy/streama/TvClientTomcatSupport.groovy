package streama

import org.apache.catalina.Context
import org.apache.catalina.Lifecycle
import org.apache.catalina.LifecycleEvent
import org.apache.catalina.LifecycleListener
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory

/**
 * Smart TV apps (Tizen, webOS) and set-top boxes load the player from the LAN
 * address of the server over plain http.
 */
class TvClientTomcatSupport implements EmbeddedServletContainerCustomizer, LifecycleListener {

  @Override
  void customize(ConfigurableEmbeddedServletContainer container) {
    if (container instanceof TomcatEmbeddedServletContainerFactory) {
      ((TomcatEmbeddedServletContainerFactory) container).addContextLifecycleListeners(this)
    }
  }

  @Override
  void lifecycleEvent(LifecycleEvent event) {
    if (event.type == Lifecycle.CONFIGURE_START_EVENT) {
      Context context = (Context) event.lifecycle
      //CWE-614
      //SINK
      context.getServletContext().getSessionCookieConfig().setSecure(false)
    }
  }
}
