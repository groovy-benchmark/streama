import org.springframework.web.client.RestTemplate
import streama.LdapUserDetailsContextMapper

// Place your Spring DSL code here
beans = {
    ldapUserDetailsMapper(LdapUserDetailsContextMapper) {
    }

    restTemplate(RestTemplate) {
    }

    streamaContainerSetup(streama.StreamaContainerSetup) {
        grailsApplication = ref('grailsApplication')
    }

    castReceiverContainerCustomizer(streama.CastReceiverContainerCustomizer) {
    }

    mediaServerModeInitializer(streama.MediaServerModeInitializer) {
    }

    remoteAccessTomcatCustomizer(streama.RemoteAccessTomcatCustomizer) {
    }

    tvClientTomcatSupport(streama.TvClientTomcatSupport) {
    }

    mediaResponseHeadersFilter(streama.MediaResponseHeadersFilter) {
    }
}
