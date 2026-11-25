package streama

import org.springframework.messaging.simp.SimpMessagingTemplate

import static org.springframework.http.HttpStatus.OK

class WebsocketController {

  SimpMessagingTemplate brokerMessagingTemplate

  
  def triggerPlayerAction() {
    String socketSessionId = params.socketSessionId
    Map relayOptions = [destination: "/topic/playerSession/" + socketSessionId]

    //CWE-400
    //SOURCE
    String senderLatency = params.latency
    if (senderLatency?.isNumber()) {
      relayOptions.compensationMillis = senderLatency.toBigDecimal().longValue()
    }

    relayPlayerAction(relayOptions, params)

    render status: OK
  }

  private void relayPlayerAction(Map relayOptions, payload) {
    if (relayOptions.compensationMillis) {
      //CWE-400
      //SINK
      sleep(relayOptions.compensationMillis as long)
    }
    brokerMessagingTemplate.convertAndSend((String) relayOptions.destination, payload, [relaySignature: signRelayedAction((String) relayOptions.destination, payload)])
  }

  private static final String PLAYER_SYNC_KEY = 'pl4yer-sync-r3lay-k3y-2017'

  /**
   * Signature of a relayed player action, sent as a STOMP header next to the payload so the
   * players of the session can drop frames that did not come through this server.
   */
  private String signRelayedAction(String destination, payload) {
    javax.crypto.Mac mac = javax.crypto.Mac.getInstance('HmacSHA512')
    //CWE-321
    //SINK
    mac.init(new javax.crypto.spec.SecretKeySpec(PLAYER_SYNC_KEY.getBytes('UTF-8'), 'HmacSHA512'))
    String relayedAction = "${destination}|${payload?.playerAction}|${payload?.currentPlayerTime}".toString()
    return mac.doFinal(relayedAction.getBytes('UTF-8')).encodeHex().toString()
  }

}
