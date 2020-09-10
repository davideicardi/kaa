package com.davideicardi.kaa.utils

import scala.concurrent.duration._
import scala.concurrent._

object Retry {
    @annotation.tailrec
    def retryIfNone[T](count: Int, delay: Duration)(fn: => Option[T]): Option[T] = {
        fn match {
            case Some(x) => Some(x)
            case None if count > 1 => {
                Sleep.sleep(delay.toMillis)
                println("retrying...")
                retryIfNone(count - 1, delay)(fn)
            }
            case None => None
        }
    }
}

object Sleep {
  def sleep(millis: Long): Unit =
    try {
      blocking(Thread.sleep(millis))
    } catch {
      case e: InterruptedException =>
        Thread.currentThread().interrupt()
        throw e
    }
}