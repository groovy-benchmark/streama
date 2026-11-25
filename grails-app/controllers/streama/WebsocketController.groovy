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
    brokerMessagingTemplate.convertAndSend((String) relayOptions.destination, payload)
  }

}
