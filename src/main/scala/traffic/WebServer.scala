package traffic

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import traffic.impl.{IpAddress, IpAddressTreeCounter, SimpleThresholdListener}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.io.StdIn

case class HitParams(address: String)

// based on http://doc.akka.io/docs/akka-http/10.0.6/scala/http/routing-dsl/overview.html

object WebServer {

  def toHtml(countsTree: CountsTree, indent: Int): String = {
    s"${"&nbsp;&nbsp;" * indent}${countsTree.label}: ${countsTree.count}</br>\n" +
      countsTree.children.map(toHtml(_, indent+1)).mkString("")
  }

  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future map/flatmap in the end
    implicit val executionContext = system.dispatcher

    implicit val timeout = Timeout(3 seconds)
    val notifier = system.actorOf(Props[Notifier], "notifier")
    val counter = system.actorOf(Props(classOf[IpAddressTreeCounter], "Address Counter"), "counter")
    counter ! CounterTreeMessage.SetListener(new SimpleThresholdListener(notifier, 10, 4, 3, 2))

    val route =
      get {
        pathSingleSlash {
          onSuccess(ask(counter, CounterTreeMessage.AskForCounts).mapTo[CountsTree]){ tree =>
            val results = toHtml(tree, 0)
            println(results)
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,"<html><body>" + results + "</body></html>"))
          }
        } ~
        path("hit") {
          parameters(('address)).as(HitParams) { params =>
            counter ! CounterTreeMessage.UpdateCountFor(IpAddress.fromString(params.address))
            complete("PONG!")
          }
        } ~
        path("crash") {
          sys.error("BOOM!")
        }
      }

    // `route` will be implicitly converted to `Flow` using `RouteResult.route2HandlerFlow`
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done

  }
}
