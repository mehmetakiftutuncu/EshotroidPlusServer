package com.mehmetakiftutuncu.utilities

import play.api.Play

object Conf extends ConfBase

trait ConfBase {
  object Http {
    val timeoutInSeconds: Int = getConfInt("eshotroidplus.http.timeout", 10)
  }

  object Hosts {
    val eshotHome: String = getConfString("eshotroidplus.hosts.eshotHome", "")
    val busPage: String   = getConfString("eshotroidplus.hosts.busPage", "")
  }

  def getConfInt(key: String, defaultValue: Int): Int = {
    Play.maybeApplication.flatMap(_.configuration.getInt(key)).getOrElse(defaultValue)
  }

  def getConfString(key: String, defaultValue: String): String = {
    Play.maybeApplication.flatMap(_.configuration.getString(key)).getOrElse(defaultValue)
  }
}