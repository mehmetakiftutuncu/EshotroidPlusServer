package com.mehmetakiftutuncu.models

import anorm.NamedParameter
import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.models.base.{ModelBase, Jsonable}
import com.mehmetakiftutuncu.utilities.{DatabaseBase, Log}
import play.api.libs.json.{JsObject, JsValue, Json}

case class Bus(id: Int, departure: String, arrival: String) extends ModelBase {
  override def toJson: JsObject = Bus.toJson(this)
}

object Bus extends BusBase {
  override protected def Database: DatabaseBase = com.mehmetakiftutuncu.utilities.Database
}

trait BusBase extends Jsonable[Bus] {
  protected def Database: DatabaseBase

  def getBusListFromDB: Either[Errors, List[Bus]] = {
    val sql = anorm.SQL("""SELECT * FROM Bus ORDER BY id""")

    Database.getMultiple(sql) match {
      case Left(getBusListErrors) => Left(getBusListErrors)

      case Right(busListRows) =>
        val busList: List[Bus] = busListRows.map {
          row =>
            val id        = row[Int]("Bus.id")
            val departure = row[String]("Bus.departure")
            val arrival   = row[String]("Bus.arrival")

            Bus(id, departure, arrival)
        }

        Right(busList)
    }
  }

  def saveBusListToDB(busList: List[Bus]): Errors = {
    val insert = """INSERT INTO Bus (id, departure, arrival) VALUES """

    val (values: List[String], parameters: List[NamedParameter]) = busList.zipWithIndex.foldLeft(List.empty[String], List.empty[NamedParameter]) {
      case ((currentValues, currentParameters), (bus, index)) =>
        val value = s"""({id_$index}, {departure_$index}, {arrival_$index})"""

        val parameters = List(
          NamedParameter(s"id_$index", bus.id),
          NamedParameter(s"departure_$index", bus.departure),
          NamedParameter(s"arrival_$index", bus.arrival)
        )

        (currentValues :+ value) -> (currentParameters ++ parameters)
    }

    val sql = anorm.SQL(insert + values.mkString(", ")).on(parameters:_*)

    Database.insert(sql)
  }

  override def toJson(bus: Bus): JsObject = {
    Json.obj(
      "id"        -> bus.id,
      "departure" -> bus.departure,
      "arrival"   -> bus.arrival
    )
  }

  override def fromJson(json: JsValue): Either[Errors, Bus] = {
    try {
      val idAsOpt        = (json \ "id").asOpt[Int]
      val departureAsOpt = (json \ "departure").asOpt[String]
      val arrivalAsOpt   = (json \ "arrival").asOpt[String]

      val idErrors = if (idAsOpt.isEmpty) {
        Errors(CommonError.invalidData.reason("Id is missing!"))
      } else if (idAsOpt.get <= 0) {
        Errors(CommonError.invalidData.reason("Id must be > 0!").data(idAsOpt.get.toString))
      } else {
        Errors.empty
      }

      val departureErrors = if (departureAsOpt.getOrElse("").isEmpty) {
        Errors(CommonError.invalidData.reason("Departure is missing!"))
      } else {
        Errors.empty
      }

      val arrivalErrors = if (arrivalAsOpt.getOrElse("").isEmpty) {
        Errors(CommonError.invalidData.reason("Arrival is missing!"))
      } else {
        Errors.empty
      }

      val errors = idErrors ++ departureErrors ++ arrivalErrors

      if (errors.nonEmpty) {
        Log.error("Bus.fromJson", s"""Failed to create bus from "$json"!""", errors)

        Left(errors)
      } else {
        val bus = Bus(
          id        = idAsOpt.get,
          departure = departureAsOpt.get,
          arrival   = arrivalAsOpt.get
        )

        Right(bus)
      }
    } catch {
      case t: Throwable =>
        val errors = Errors(CommonError.invalidData)

        Log.error(t, "Bus.fromJson", s"""Failed to create bus from "$json"!""", errors)

        Left(errors)
    }
  }
}
