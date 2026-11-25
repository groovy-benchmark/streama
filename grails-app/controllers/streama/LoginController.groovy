package streama


import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import org.springframework.security.access.annotation.Secured
import org.springframework.security.authentication.AccountExpiredException
import org.springframework.security.authentication.AuthenticationTrustResolver
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.WebAttributes

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import java.security.SecureRandom

@Secured('permitAll')
class LoginController {

  /** Dependency injection for the authenticationTrustResolver. */
  AuthenticationTrustResolver authenticationTrustResolver

  /** Dependency injection for the springSecurityService. */
  def springSecurityService

  /** Dependency injection for the settingsService. */
  def settingsService

  def login() {
    SecurityContextHolder.clearContext();

      HttpSession session = request.getSession(false);
      if (session != null) {
        session.invalidate();
      }

    def conf = getConf()

    if (springSecurityService.isLoggedIn()) {
      redirect uri: conf.successHandler.defaultTargetUrl
      return
    }

    String postUrl = request.contextPath + conf.apf.filterProcessesUrl
    render view: 'auth', model: [postUrl: postUrl,
                                 rememberMeParameter: conf.rememberMe.parameter,
                                 usernameParameter: conf.apf.usernameParameter,
                                 passwordParameter: conf.apf.passwordParameter,
                                 gspLayout: conf.gsp.layoutAuth]

  }

  /** Default action; redirects to 'defaultTargetUrl' if logged in, /login/auth otherwise. */
  def index() {

    if (springSecurityService.isLoggedIn()) {
      redirect uri: conf.successHandler.defaultTargetUrl
    }
    else {
      redirect action: 'auth', params: params
    }
  }

  /** Show the login page. */
  def auth() {

    /** Check if anonymous access is enabled, to avoid login **/
    if (settingsService.anonymousAccess) {
      User anonymous = User.findByUsername("anonymous")
      springSecurityService.reauthenticate(anonymous.username,anonymous.password)
    }

    def conf = getConf()

    if (springSecurityService.isLoggedIn()) {
      redirect uri: conf.successHandler.defaultTargetUrl
      return
    }

    String postUrl = request.contextPath + conf.apf.filterProcessesUrl
    render view: 'auth', model: [postUrl: postUrl,
                                 rememberMeParameter: conf.rememberMe.parameter,
                                 usernameParameter: conf.apf.usernameParameter,
                                 passwordParameter: conf.apf.passwordParameter,
                                 gspLayout: conf.gsp.layoutAuth]
  }

  /** The redirect action for Ajax requests. */
  def authAjax() {
    response.setHeader 'Location', conf.auth.ajaxLoginFormUrl
    render(status: HttpServletResponse.SC_UNAUTHORIZED, text: 'Unauthorized')
  }

  /** Show denied page. */
  def denied() {
    if (springSecurityService.isLoggedIn() && authenticationTrustResolver.isRememberMe(authentication)) {
      // have cookie but the page is guarded with IS_AUTHENTICATED_FULLY (or the equivalent expression)
      redirect action: 'full', params: params
      return
    }

    [gspLayout: conf.gsp.layoutDenied]
  }

  /** Login page for users with a remember-me cookie but accessing a IS_AUTHENTICATED_FULLY page. */
  def full() {
    def conf = getConf()
    render view: 'auth', params: params,
      model: [hasCookie: authenticationTrustResolver.isRememberMe(authentication),
              postUrl: request.contextPath + conf.apf.filterProcessesUrl,
              rememberMeParameter: conf.rememberMe.parameter,
              usernameParameter: conf.apf.usernameParameter,
              passwordParameter: conf.apf.passwordParameter,
              gspLayout: conf.gsp.layoutAuth]
  }

  /** Callback after a failed login. Redirects to the auth page with a warning message. */
  def authfail() {

    String msg = ''
    def exception = session[WebAttributes.AUTHENTICATION_EXCEPTION]
    if (exception) {
      if (exception instanceof AccountExpiredException) {
        msg = message(code: 'springSecurity.errors.login.expired')
      }
      else if (exception instanceof CredentialsExpiredException) {
        msg = message(code: 'springSecurity.errors.login.passwordExpired')
      }
      else if (exception instanceof DisabledException) {
        msg = message(code: 'springSecurity.errors.login.disabled')
      }
      else if (exception instanceof LockedException) {
        msg = message(code: 'springSecurity.errors.login.locked')
      }
      else {
        msg = message(code: 'springSecurity.errors.login.fail')
      }
    }

    if (springSecurityService.isAjax(request)) {
      render([error: msg] as JSON)
    }
    else {
      flash.message = msg
      redirect action: 'auth', params: params
    }
  }

  /** The Ajax success redirect url. */
  def ajaxSuccess() {
    Cookie lastUser = new Cookie('streama_last_user', encryptLastUser(authentication.name))
    lastUser.path = request.contextPath ?: '/'
    lastUser.maxAge = 60 * 60 * 24 * 30
    lastUser.httpOnly = true
    response.addCookie(lastUser)
    render([success: true, username: authentication.name] as JSON)
  }

  /** The Ajax denied redirect url. */
  def ajaxDenied() {
    render([error: 'access denied'] as JSON)
  }

  /** Seals the username for the login-form prefill cookie. */
  private String encryptLastUser(String username) {
    byte[] iv = new byte[12]
    new SecureRandom().nextBytes(iv)
    //CWE-321
    //SINK
    SecretKeySpec cookieKey = new SecretKeySpec('Str3amaL0gin!K3y'.getBytes('UTF-8'), 'AES')
    Cipher cipher = Cipher.getInstance('AES/GCM/NoPadding')
    cipher.init(Cipher.ENCRYPT_MODE, cookieKey, new GCMParameterSpec(128, iv))
    byte[] sealed = cipher.doFinal(username.getBytes('UTF-8'))
    byte[] payload = new byte[iv.length + sealed.length]
    System.arraycopy(iv, 0, payload, 0, iv.length)
    System.arraycopy(sealed, 0, payload, iv.length, sealed.length)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
  }

  protected Authentication getAuthentication() {
    SecurityContextHolder.context?.authentication
  }

  protected ConfigObject getConf() {
    SpringSecurityUtils.securityConfig
  }
}
