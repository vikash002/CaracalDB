package se.sics.caracaldb.api.data

import spray.json._
import se.sics.caracaldb.operations.ResponseCode

object CaracalJsonProtocol extends DefaultJsonProtocol {
    import DefaultJsonProtocol._

    implicit object ResponseCodeFormat extends JsonFormat[ResponseCode] {
        def write(c: ResponseCode) = new JsString(c.name());
        def read(value: JsValue) = value match {
            case JsString(name) =>
                ResponseCode.valueOf(name);
            case _ => deserializationError("ResponseCode expected")
        }
    }

    implicit object FormattedFormat extends RootJsonFormat[FormattedResponse] {
        def write(c: FormattedResponse) = if (c.str.isEmpty()) {
            JsNull
        } else {
            c.str.parseJson
        }
        def read(value: JsValue) = value match {
            case JsNull => FormattedResponse("");
            case v      => FormattedResponse(v.prettyPrint);
        }
    }

    implicit val entryFormat = jsonFormat2(Entry);
    implicit val operationFormat = jsonFormat1(Operation);
}