package vdx.stockpile

import cats.effect.IO
import io.circe.parser.decode
import io.circe.generic.auto._
import vdx.stockpile.CardDB.{CardSet, CardWithSimpleSet, RepositoryAlg}

import scala.collection.immutable.HashMap
import scala.concurrent.Future
import scala.io.Source

package object mgjson {
  type DB = HashMap[String, CardSet]

  private val jsonString = Source.fromResource("AllSets-x.json").mkString

  private lazy val dbIO: IO[List[CardSet]] = IO.pure(
    decode[DB](jsonString) match {
      case Left(error) => throw error
      case Right(db)   => db.values.toList
    }
  )

  trait MtgJsonDBInterpreter extends RepositoryAlg[IO] {
    override def findSimpleSetByName(name: String): IO[Option[CardDB.SimpleSet]] =
      dbIO.map(
        sets => sets.find(_.name == name).map(_.asSimple)
      )
  }

}
