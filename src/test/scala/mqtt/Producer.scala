package mqtt

import java.util.UUID

import org.eclipse.paho.client.mqttv3.{MqttClient, MqttMessage}

/**
  * Simple MQTT producer of String messages on a topic to test persistent shared subscriptions.
  *
  * Run with arguments:
  * - name of topic. Use $share.<GROUPID>.<TOPIC> for a shared subscription.
  */
object Producer extends App {

  val topic = args(0)
  val brokerUrl = "tcp://localhost:1883"

  val clientId = UUID.randomUUID().toString

  val client = new MqttClient(brokerUrl, clientId)
  client.connect()
  val theTopic = client.getTopic(topic)

  var count = 0

  sys.addShutdownHook {
    println("Disconnecting client...")
    client.disconnect()
    println("Disconnected.")
  }

//  while(true) {
    val msg = new MqttMessage(s"Message: ${new java.util.Date()}".getBytes())
    theTopic.publish(msg)
    println(s"Published: $msg")

//    Thread.sleep(1000)

//    count = count + 1
//  }

  sys.exit()
}