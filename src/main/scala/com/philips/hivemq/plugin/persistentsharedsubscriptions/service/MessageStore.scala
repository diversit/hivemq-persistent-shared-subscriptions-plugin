package com.philips.hivemq.plugin.persistentsharedsubscriptions.service

/**
  * A store to temporary store messages for shared subscription topics
  * until a client reconnects to a shared subscription topic.
  */
trait MessageStore {

  def storeMessageForSharedSubscription(message: Array[Byte], sharedSubscription: SharedSubscription)

  def getMessagesForSharedSubscription(sharedSubscription: SharedSubscription): Seq[Array[Byte]]
}

class InMemoryMessageStore extends MessageStore {
  import scala.collection.mutable

  val store = mutable.Map.empty[SharedSubscription, Seq[Array[Byte]]]

  override def storeMessageForSharedSubscription(message: Array[Byte], sharedSubscription: SharedSubscription): Unit = {
    val messages = store.getOrElse(sharedSubscription, Seq.empty) :+ message
    store += (sharedSubscription -> messages)
  }

  override def getMessagesForSharedSubscription(sharedSubscription: SharedSubscription): Seq[Array[Byte]] = {
    store.getOrElse(sharedSubscription, Seq.empty)
  }
}
