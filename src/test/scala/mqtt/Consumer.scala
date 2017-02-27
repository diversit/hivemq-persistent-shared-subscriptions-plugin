package mqtt

import java.util.UUID

import org.eclipse.paho.client.mqttv3._

/**
  * Simple MQTT consumer on a topic to test persistent shared subscriptions.
  * Creates a unique client id since messages should be persistent regardless of client id.
  *
  * Can be started multiple times to get multiple concurrent consumers.
  *
  * Run with arguments:
  * - name of topic. Use $share.<GROUPID>.<TOPIC> for a shared subscription.
  */
object Consumer extends App {

  val topic = args(0)
  val brokerUrl = "tcp://localhost:1883"

//  val clientId = args(1)
  val clientId = UUID.randomUUID().toString

  val client = new MqttClient(brokerUrl, clientId)
  client.setCallback(new MqttCallback {
    override def deliveryComplete(token: IMqttDeliveryToken) = ()

    override def messageArrived(topic: String, message: MqttMessage) = println(s"received on topic '$topic': ${new String(message.getPayload)}")

    override def connectionLost(cause: Throwable) = println("Connection lost")
  })

  println(s"Start $clientId consuming from topic: $topic")
  val options = new MqttConnectOptions()
  options.setCleanSession(false);

  client.connect(options)
  client.subscribe(topic)

  sys.addShutdownHook {
    println("Disconnecting client...")
    //    client.unsubscribe(topic)
    client.disconnect()
    println("Disconnected.")
  }


  while(true) {

  }

}