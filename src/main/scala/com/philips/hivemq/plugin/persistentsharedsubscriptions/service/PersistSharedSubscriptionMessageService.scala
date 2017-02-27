package com.philips.hivemq.plugin.persistentsharedsubscriptions.service

import com.hivemq.spi.callback.events.OnPublishReceivedCallback
import com.hivemq.spi.message.{PUBLISH, QoS}
import com.hivemq.spi.security.ClientData
import com.hivemq.spi.services.{PluginExecutorService, PublishService}
import org.slf4j.LoggerFactory

/**
  * A service to persist any message to a shared subscription topic
  * for which current (temporary) no clients are available anymore.
  *
  * Also handles if a shared subscription client reconnects
  * that all missed messages are send to that client.
  *
  * Both storing of the message and the sending of missed messages
  * will be done asynchonously using the [[com.hivemq.spi.services.PluginExecutorService]].
  */
class PersistSharedSubscriptionMessageService(pluginExecutorService: PluginExecutorService,
                                              registry: SharedSubscriptionRegistry,
                                              messageStore: MessageStore,
                                              publishService: PublishService) extends OnPublishReceivedCallback {

  private val log = LoggerFactory.getLogger(classOf[PersistSharedSubscriptionMessageService])

  /**
    * Called when a message is published.
    * !! MUST NOT BE BLOCKING OTHERWISE DEGRADES HIVEMQ PERFORMANCE !!
    *
    * If message is for shared subscription topic AND currently no
    * clients connects, then temporary store the message.
    */
  override def onPublishReceived(publish: PUBLISH, clientData: ClientData): Unit = {
    pluginExecutorService.submit(new Runnable {
      override def run() = {
        log.debug("Checking of message needs to be persisted")
        val topicName = publish.getTopic
        val sharedSubscriptions = registry.getSharedSubscriptionsForTopic(topicName)
        log.debug("Have shared subscription topic {}? {}", topicName, sharedSubscriptions.size > 0)

        sharedSubscriptions.foreach { sharedSubscription =>

          val hasNoActiveClients = registry.hasNoActiveClients(sharedSubscription)
          log.debug("Shared subscription has no active clients? {}", hasNoActiveClients)

          if (hasNoActiveClients) {
            val msg = new String(publish.getPayload)
            log.info("Storing message {} for shared subscription {}", msg, sharedSubscription: Any)
            messageStore.storeMessageForSharedSubscription(publish.getPayload, sharedSubscription)
          }
        }
      }
    })
  }

  def sendMessagesToClient(sharedSubscription: SharedSubscription, clientData: ClientData): Unit = {
    pluginExecutorService.submit(new Runnable {
      override def run() = {
        log.debug("Sending all messages for shared subscription {} to client {}", sharedSubscription, clientData.getClientId: Any)

        val messages = messageStore.getMessagesForSharedSubscription(sharedSubscription)

        pluginExecutorService.submit(new RunTask(
          publishService.publish(new PUBLISH("test 1".getBytes, "test", QoS.AT_LEAST_ONCE))
        ))
        pluginExecutorService.submit(new RunTask(
          publishService.publish(new PUBLISH("test 2".getBytes, sharedSubscription.toString, QoS.AT_LEAST_ONCE))
        ))
        pluginExecutorService.submit(new RunTask(
          publishService.publish(new PUBLISH("test 3".getBytes, "$share:my-consumers:test", QoS.AT_LEAST_ONCE))
        ))

        pluginExecutorService.submit(new RunTask(
          publishService.publishtoClient(new PUBLISH("test a".getBytes, "test", QoS.AT_LEAST_ONCE), clientData.getClientId)
        ))
        pluginExecutorService.submit(new RunTask(
          publishService.publishtoClient(new PUBLISH("test b".getBytes, sharedSubscription.toString, QoS.AT_LEAST_ONCE), clientData.getClientId)
        ))
        pluginExecutorService.submit(new RunTask(
          publishService.publishtoClient(new PUBLISH("test c".getBytes, "$share:my-consumers:test", QoS.AT_LEAST_ONCE), clientData.getClientId)
        ))

        messages foreach { message =>
          log.debug("Publishing message '{}' to client '{}'", new String(message), clientData.getClientId: Any)

//          val publish = new PUBLISH(message, sharedSubscription.toString, QoS.AT_LEAST_ONCE)
          publishService.publishtoClient(new PUBLISH("12".getBytes(), "$share:my-consumers:test", QoS.AT_LEAST_ONCE), clientData.getClientId)
          publishService.publishtoClient(new PUBLISH("11".getBytes(), "test", QoS.AT_LEAST_ONCE), clientData.getClientId)
          publishService.publishtoClient(new PUBLISH("13".getBytes(), "$share/my-consumers/test", QoS.AT_LEAST_ONCE), clientData.getClientId)

          publishService.publish(new PUBLISH("22".getBytes(), "$share:my-consumers:test", QoS.AT_LEAST_ONCE))
          publishService.publish(new PUBLISH("21".getBytes(), "test", QoS.AT_LEAST_ONCE))
          publishService.publish(new PUBLISH("23".getBytes(), "$share/my-consumers/test", QoS.AT_LEAST_ONCE))
        }
      }
    })
  }

  override def priority(): Int = 100
}

class RunTask(task: => Unit) extends Runnable {

  override def run(): Unit = task
}
