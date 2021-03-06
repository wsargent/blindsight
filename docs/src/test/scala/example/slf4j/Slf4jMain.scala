/*
 * Copyright 2020 Terse Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package example.slf4j

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date

import com.tersesystems.blindsight._
import com.tersesystems.blindsight.api.{Arguments, AsArguments, Markers, ToArguments, ToMarkers}
import com.tersesystems.blindsight.slf4j.{
  SLF4JLogger,
  SLF4JLoggerAPI,
  StrictSLF4JMethod,
  UncheckedSLF4JMethod
}
import org.slf4j.MarkerFactory

object Slf4jMain {

  final case class FeatureFlag(flagName: String)

  object FeatureFlag {
    implicit val toMarkers: ToMarkers[FeatureFlag] = ToMarkers { instance =>
      Markers(MarkerFactory.getDetachedMarker(instance.flagName))
    }
  }

  case class CreditCard(number: String)

  def main(args: Array[String]): Unit = {
    val logger = LoggerFactory.getLogger(getClass)

    val featureFlag = FeatureFlag("flag.enabled")
    if (logger.isDebugEnabled(featureFlag)) {
      logger.debug("this is a test")
    }

    logger.info.when(System.currentTimeMillis() % 2 == 0) { log => log("I am divisable by two") }

    logger.info("hello world")

    val m1 = MarkerFactory.getMarker("M1")

    import com.tersesystems.blindsight.logstash.Implicits._
    val unchecked: SLF4JLogger[UncheckedSLF4JMethod] = logger.unchecked
    val e                                            = new Exception("derp")
    unchecked.error("Exception occured", e)
    val creditCard = CreditCard("4111111111111")

    // case class tostring renders CC number
    unchecked.info("this is risky unchecked {}", creditCard: Any)

    unchecked.info("this is unchecked {} {}", Arguments(42, 53))
    unchecked.info(
      m1,
      "unchecked with argument and marker {}, creditCard = {}",
      Seq(42, creditCard): _*
    )

    val strict: SLF4JLogger[StrictSLF4JMethod] = logger.strict

    strict.info("arg {}, arg {}, arg 3 {}", Arguments(1, "2", false))

    strict.error("this is an error", e)
    strict.error("this is an error with argument {}", Arguments("a" -> "b", e))
    strict.error(
      "this is an error with concat arguments {} {}",
      Arguments("a" -> "b") + Arguments("c" -> "d")
    )
    //strict.info("won't compile, must define ToArguments[CreditCard]", creditCard)
    strict.info("this is strict {} {}", Arguments(42, 53))
    strict.info(
      "this is strict with a seq first is [{}], second is [{}]",
      Map("a" -> "b", "c" -> "d")
    )
    strict.info(
      "markerKey" -> "markerValue",
      "marker and argument {}",
      "argumentKey" -> "argumentValue"
    )

    implicit val dateToArgument: ToArguments[Date] = ToArguments[java.util.Date] { date =>
      new Arguments(Seq(DateTimeFormatter.ISO_INSTANT.format(date.toInstant)))
    }

    implicit val instantToArgument: ToArguments[java.time.Instant] =
      ToArguments[java.time.Instant] { instant =>
        new Arguments(Seq(DateTimeFormatter.ISO_INSTANT.format(instant)))
      }

    logger.info("date is {}", new java.util.Date())
    logger.info("instant is {}", Instant.now())

    logger.info("a b {}", "a" -> "b")

    val m2   = MarkerFactory.getMarker("M2")
    val base = logger.withMarker(m1).withMarker(m2)
    base.info("I should have two markers")

    val onlyInfo = new SLF4JLoggerAPI.Info[base.Predicate, base.Method] {
      override type Self      = base.Self
      override type Predicate = base.Predicate
      override type Method    = base.Method

      override def isInfoEnabled: Predicate = base.isInfoEnabled
      override def info: Method             = base.info
    }
    onlyInfo.info("good")
  }
}
