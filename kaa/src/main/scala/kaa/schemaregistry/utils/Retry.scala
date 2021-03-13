package kaa.schemaregistry.utils

import scala.concurrent.duration._
import scala.concurrent._

object Retry {
    @annotation.tailrec
    def retryIfNone[T](retryConfig: RetryConfig)(fn: => Option[T]): Option[T] = {
        fn match {
            case Some(x) => Some(x)
            case None if retryConfig.count > 1 => {
                Sleep.sleep(retryConfig.delay.toMillis)
                retryIfNone(retryConfig.copy(count = retryConfig.count - 1))(fn)
            }
            case None => None
        }
    }
}

object RetryConfig {
  val NoRetry = RetryConfig(0, 0.second)
}

case class RetryConfig(count: Int, delay: Duration) {}

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