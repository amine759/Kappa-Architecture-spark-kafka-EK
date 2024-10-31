package producer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import scala.io.StdIn
import scala.concurrent.ExecutionContextExecutor

object Producer {

  def main(args: Array[String]): Unit = {

    // Create an untyped actor system
    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val route =
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      }

    // Start HTTP server
    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

    println(s"Server now online. Please navigate to http://localhost:8080/hello\nPress RETURN to stop...")
    StdIn.readLine() // Let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // Trigger unbinding from the port
      .onComplete(_ => system.terminate()) // Shutdown when done
  }
}
