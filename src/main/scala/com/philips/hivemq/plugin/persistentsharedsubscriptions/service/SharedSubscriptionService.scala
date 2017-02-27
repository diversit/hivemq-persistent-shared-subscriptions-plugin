package com.philips.hivemq.plugin.persistentsharedsubscriptions.service

import com.hivemq.spi.callback.events.{OnDisconnectCallback, OnSubscribeCallback, OnUnsubscribeCallback}
import com.hivemq.spi.message.{SUBSCRIBE, UNSUBSCRIBE}
import com.hivemq.spi.security.ClientData
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Service to handle events to keep the [[SharedSubscriptionRegistry]] up to date.
  */
class SharedSubscriptionService(registry: SharedSubscriptionRegistry,
                                persistSharedSubscriptionMessageService: PersistSharedSubscriptionMessageService) extends OnDisconnectCallback with OnSubscribeCallback with OnUnsubscribeCallback {
  import collection.JavaConverters._

  /**
    * Callback handler to handle both gracefull and ungracefull disconnection of clients.
    * In case of disconnection, the client will be removed from all it's shared subscriptions
    * so the shared subscription has an accurate view on all active clients.
    */
  override def onDisconnect(clientData: ClientData, abruptAbort: Boolean): Unit = {
    // find all subscriptions for this client
    val subscriptions: Seq[SharedSubscription] = registry.getSubscriptionsForClient(clientData.getClientId)

    // Unregister the subscription for the client
    val unregisterSubscription: SharedSubscription => Unit =
      subscription => registry.removeSubscription(subscription, clientData.getClientId)

    // unregister client from all subscriptions
    subscriptions foreach unregisterSubscription
  }

  /** register subscription for client if subscription is a shared subscription */
  override def onSubscribe(message: SUBSCRIBE, clientData: ClientData) = {
    // Register subscription for the client
    val registerSubscription: SharedSubscription => Unit = { subscription =>
      val isFirstClient = registry.hasNoActiveClients(subscription)
      registry.addSubscription(subscription, clientData.getClientId)

      if (isFirstClient) {
        persistSharedSubscriptionMessageService.sendMessagesToClient(subscription, clientData)
      }
    }

    val topicsNamesSeq = message.getTopics.asScala.map(_.getTopic)
    handleSubscription(topicsNamesSeq, registerSubscription)
  }

  /** unregister subscription for client if subscription is a shared subscription */
  override def onUnsubscribe(message: UNSUBSCRIBE, clientData: ClientData): Unit = {
    // Unregister the subscription for the client
    val unregisterSubscription: SharedSubscription => Unit =
      subscription => registry.removeSubscription(subscription, clientData.getClientId)

    val topicsNamesSeq = message.getTopics.asScala
    handleSubscription(topicsNamesSeq, unregisterSubscription)
  }

  /**
    * Handle subscription if it is a shared subscription
    * @param topics Seq of topic names (SUBSCRIBE has List[Topic] and UNSUBSCRIBE has List[String] for topics)
    * @param subscriptionAction Action to execute for each shared subscription
    */
  private def handleSubscription(topics: Seq[String], subscriptionAction: SharedSubscription => Unit) = {
    topics
      .filter(SharedSubscription.IS_SHARED_SUBSCRIPTION)
      .flatMap(SharedSubscription.from)
      .foreach(subscriptionAction)

  }

  override def priority() = 100
}

trait SharedSubscriptionRegistry {
  type ClientID = String

  def addSubscription(sharedSubscription: SharedSubscription, clientId: ClientID): Unit

  def removeSubscription(sharedSubscription: SharedSubscription, clientID: ClientID): Unit

  def getSubscriptionsForClient(clientID: ClientID): Seq[SharedSubscription]

  /**
    * Return all shared subscriptions for given topic.
    * Note that there may be multiple shared subscriptions, each with a different groupId.
    *
    * @param topicName Name of the topic.
    * @return [[Seq]] of [[SharedSubscription]]s for given topic name.
    */
  def getSharedSubscriptionsForTopic(topicName: String): Seq[SharedSubscription]

  /**
    * @param sharedSubscription The [[SharedSubscription]] to check.
    * @return <code>True</code> when the subscription has NO active clients. Otherwise <code>False</code>.
    */
  def hasNoActiveClients(sharedSubscription: SharedSubscription): Boolean
}

class InMemorySharedSubscriptionRegistry extends SharedSubscriptionRegistry {
  private val log = LoggerFactory.getLogger(classOf[InMemorySharedSubscriptionRegistry])

  val subscriptionMap = mutable.Map[SharedSubscription, Set[ClientID]]()

  override def addSubscription(sharedSubscription: SharedSubscription, newClientId: ClientID): Unit = {
    // get current clients and add new client id
    val subscriptionClientSet = subscriptionMap.getOrElse(sharedSubscription, Set()) + newClientId
    // save updated client set in map
    subscriptionMap += (sharedSubscription -> subscriptionClientSet)

    // 'Any' typing of 3rd argument is needed. See https://github.com/typesafehub/scalalogging/issues/16
    log.info("Added subscription {} for client {}", sharedSubscription, newClientId: Any)
  }

  override def removeSubscription(sharedSubscription: SharedSubscription, clientID: ClientID): Unit = {
    // get current client for subscription
    subscriptionMap.get(sharedSubscription) map { subscriptionClientSet =>
      // remove client from set
      val updatedClientSet = subscriptionClientSet - clientID
      subscriptionMap += (sharedSubscription -> updatedClientSet)

      log.info("Removed subscription {} for client {}", sharedSubscription, clientID: Any)
    }
  }

  /** Create a Seq of all subscriptions in which given client is subscribed */
  override def getSubscriptionsForClient(clientID: ClientID): Seq[SharedSubscription] = {
    log.info("Get all subscriptions for client {}", clientID)

    subscriptionMap.collect {
      case (subscription, clients) if (clients.contains(clientID)) => subscription
    }.toSeq
  }

  override def getSharedSubscriptionsForTopic(topicName: String): Seq[SharedSubscription] = {
    subscriptionMap.keys.filter(_.topic == topicName).toSeq
  }

  override def hasNoActiveClients(sharedSubscription: SharedSubscription): Boolean = {
    subscriptionMap.get(sharedSubscription) map (_.isEmpty) getOrElse(false)
  }
}

case class SharedSubscription(groupId: String, topic: String) {
  override def toString: String = s"$$share:$groupId:$topic"
}

object SharedSubscription {

  /**
    * A HiveMQ shared subscription is either
    * '$share:GROUP_ID:TOPIC' or '$share/GROUP_ID/topic'
    *
    * Note: Paho only likes the '/' variant.
    */
  val SHARED_SUBSCRIPTION_REGEX = """\$share[:/]([a-zA-Z-]+)[:/]([0-9a-zA-Z/]+)""".r

  /**
    * A shared topic must start with '$share.'
    * Use 'startsWith' because faster (I think) than using the regex pattern.
    */
  val IS_SHARED_SUBSCRIPTION: String => Boolean = topicName => topicName.startsWith("$share")

  def from(topicName: String): Option[SharedSubscription] = topicName match {
    case SHARED_SUBSCRIPTION_REGEX(groupId, topicname) => Some(SharedSubscription(groupId, topicname))
    case _ => None
  }
}
