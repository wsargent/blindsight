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

package com.tersesystems.blindsight

final class Argument(val value: Any) {
  def arguments: Arguments   = new Arguments(Seq(this))
  def toStatement: Statement = Statement().withArguments(arguments)
}

object Argument {
  def apply[A: ToArgument](instance: A): Argument = implicitly[ToArgument[A]].toArgument(instance)
}

/**
 * This is the representation of arguments in an SLF4J logging statement.
 *
 * Arguments present as an immutable API, which can be composed together using `+` and
 * `++` for sequences.
 *
 * {{{
 * val argsA: Arguments = Arguments("a")
 * val argsPlus: Arguments = argsA + "b"
 * val argsPlusPlus: Arguments = argsPlus ++ Seq(1, "c")
 * val messageWithPlaceHolders = Message("some message").withPlaceHolders(argsPlusPlus)
 * logger.info(messageWithPlaceHolders, argsPlusPlus)
 * }}}
 *
 * There is no special treatment of exceptions; as in SLF4J, the exception must be the
 * last element of arguments to be treated as the Throwable.
 */
final class Arguments(private val elements: Seq[Argument]) {

  def add[T: ToArgument](instance: T): Arguments = {
    new Arguments(elements :+ Argument(instance))
  }

  def +[T: ToArgument](instance: T): Arguments = add(instance)

  def placeholders: String = " {}" * elements.size

  def toSeq: Seq[Any] = elements.map(_.value)

  def toArray: Array[Any] = toSeq.toArray
}

final class AsArgument(val argument: Argument)

object AsArgument {
  implicit def toAsArgument[A: ToArgument](a: A): AsArgument = {
    val arguments = implicitly[ToArgument[A]].toArgument(a)
    new AsArgument(arguments)
  }
}

object Arguments {
  def empty: Arguments = new Arguments(Seq.empty)

  def apply(els: AsArgument*): Arguments = {
    els.foldLeft(Arguments.empty)((acc, el) => acc + el.argument)
  }
}