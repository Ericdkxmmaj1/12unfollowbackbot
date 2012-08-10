package nu.glen.followbackbot

import twitter4j._

case class TweetAction(twitter: Twitter, statusUpdate: StatusUpdate)
  extends Action with SimpleLogger
{
  override def apply() {
    log.info(" Tweeting: %s", statusUpdate.getStatus)
    twitter.updateStatus(statusUpdate)
  }
}

class Listener(
    userId: Long,
    screenName: String,
    responder: Responder,
    socialGraph: SocialGraph,
    twitter: Twitter)
  extends UserStreamListener
  with SimpleLogger
{
  def isMe(user: User) = user.getId == userId

  def isMyOwnRetweet(status: Status) = {
    // simple check should catch both old- and new-school RTs
    status.getText.toLowerCase.startsWith("rt @" + screenName.toLowerCase + ":")
  }

  override def onStatus(status: Status) {
    log.info("Got Status: @%s: %s", status.getUser.getScreenName, status.getText)

    if (isMe(status.getUser)) {
      log.info(" Ignoring my own status")
    } else if (isMyOwnRetweet(status)) {
      log.info(" Ignoring a retweet of my own status")
    } else {
      responder(status) match {
        case Some(statusUpdate) =>
          // only send the reply if the tweeter still follows us
          socialGraph.ifFollowing(
            status.getUser.getId,
            TweetAction(twitter, statusUpdate),
            " Replying with %s",
            statusUpdate.getStatus
          )

        case None => log.info(" Ignoring ineligible status")
      }
    }
  }

  override def onFollow(source: User, followedUser: User) {
    log.info(
      "Got follow notification: %s/%d -> %s/%d",
      source.getScreenName,
      source.getId,
      followedUser.getScreenName,
      followedUser.getId
    )

    if (isMe(source))
      log.info(" Ignoring notification of my own actions")
    else
      socialGraph.follow(source.getId, Some(source.isProtected))
  }

  override def onException(ex: Exception) = ()

  override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) = ()
  override def onScrubGeo(userId: Long, upToStatusId: Long) = ()
  override def onTrackLimitationNotice(numberOfLimitedStatuses: Int) = ()

  override def onBlock(source: User, blockedUser: User) = ()
  override def onDeletionNotice(directMessageId: Long, userId: Long) = ()
  override def onDirectMessage(directMessage: DirectMessage) = ()
  override def onFavorite(source: User, target: User, favoritedStatus: Status) = ()
  override def onFriendList(friendIds: Array[Long]) = ()
  override def onRetweet(source: User, target: User, retweetedStatus: Status) = ()
  override def onUnblock(source: User, unblockedUser: User) = ()
  override def onUnfavorite(source: User, target: User, unfavoritedStatus: Status) = ()
  override def onUserListCreation(listOwner: User, list: UserList) = ()
  override def onUserListDeletion(listOwner: User, list: UserList) = ()
  override def onUserListMemberAddition(addedMember: User, listOwner: User, list: UserList) = ()
  override def onUserListMemberDeletion(deletedMember: User, listOwner: User, list: UserList) = ()
  override def onUserListSubscription(subscriber: User, listOwner: User, list: UserList) = ()
  override def onUserListUnsubscription(subscriber: User, listOwner: User, list: UserList) = ()
  override def onUserListUpdate(listOwner: User, list: UserList) = ()
  override def onUserProfileUpdate(updatedUser: User) = ()
}
