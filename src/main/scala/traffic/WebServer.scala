package traffic

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import traffic.impl.{IpAddress, IpAddressCountTree, SimpleThresholdListener}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.io.StdIn

object WebServer {

  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future map/flatmap in the end
    implicit val executionContext = system.dispatcher

    implicit val timeout = Timeout(3 seconds)
    val notifier = system.actorOf(Props[Notifier], "notifier")
    val counter = system.actorOf(Props(classOf[IpAddressCountTree], "Address Counter"), "counter")
    counter ! SetListener(new SimpleThresholdListener(notifier, 10, 4, 3, 2))

    val route =
      get {
        pathSingleSlash {
          onSuccess(ask(counter, AskForCountsTree).mapTo[CountsTree]){ tree =>
            val results = tree.toHtml(0)
            println(results)
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,"<html><body>" + results + "</body></html>"))
          }
        } ~
        path("ping") {
          counter ! UpdateCountFor(IpAddress(1,2,3,4))
          complete("PONG!")
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
