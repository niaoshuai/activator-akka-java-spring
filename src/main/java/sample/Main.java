package sample;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.util.Timeout;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import sample.CountingActor.Count;
import sample.CountingActor.Get;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;
import static sample.SpringExtension.SpringExtProvider;

/**
 * A main class to start up the application.
 */
public class Main {
  public static void main(String[] args) throws Exception {
    // create a spring context and scan the classes
    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext();
    ctx.scan("sample");
    ctx.refresh();

    // get hold of the actor system
    ActorSystem system = ctx.getBean(ActorSystem.class);
    // use the Spring Extension to create props for a named actor bean
    ActorRef counter = system.actorOf(
            SpringExtProvider.get(system).props("CountingActor"), "counter");


    long begin = System.currentTimeMillis();
    for (int i = 0; i < 1000000; i++) {
      counter.tell(new Count(), null);
    }
    System.out.println("100万 演员消息耗费时间 = " + (System.currentTimeMillis() - begin));

    // print the result
    FiniteDuration duration = FiniteDuration.create(1, TimeUnit.SECONDS);
    Future<Object> result = ask(counter, new Get(),
            Timeout.durationToTimeout(duration));
    try {
      System.out.println("Got back " + Await.result(result, duration));
    } catch (Exception e) {
      System.err.println("Failed getting result: " + e.getMessage());
      throw e;
    } finally {
      system.shutdown();
      system.awaitTermination();
    }
  }
}
