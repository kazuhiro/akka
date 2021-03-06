/**
 *  Copyright (C) 2015-2016 Typesafe <http://typesafe.com/>
 */
package docs.stream.javadsl.cookbook;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.JavaTestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class RecipeSeq extends RecipeTest {
  static ActorSystem system;

  @BeforeClass
  public static void setup() {
    system = ActorSystem.create("RecipeLoggingElements");
  }

  @AfterClass
  public static void tearDown() {
    JavaTestKit.shutdownActorSystem(system);
    system = null;
  }

  final Materializer mat = ActorMaterializer.create(system);

  @Test
  public void drainSourceToList() throws Exception {
    new JavaTestKit(system) {
      {
        //#draining-to-list-unsafe
        final Source<String, NotUsed> myData = Source.from(Arrays.asList("1", "2", "3"));
        final int MAX_ALLOWED_SIZE = 100;

        final CompletionStage<List<String>> strings = myData.runWith(Sink.seq(), mat); // dangerous!
        //#draining-to-list-unsafe

        strings.toCompletableFuture().get(3, TimeUnit.SECONDS);
      }
    };
  }

  @Test
  public void drainSourceToListWithLimit() throws Exception {
    new JavaTestKit(system) {
      {
        //#draining-to-list-safe
        final Source<String, NotUsed> myData = Source.from(Arrays.asList("1", "2", "3"));
        final int MAX_ALLOWED_SIZE = 100;

        // OK. Future will fail with a `StreamLimitReachedException`
        // if the number of incoming elements is larger than max
        final CompletionStage<List<String>> strings =
          myData.limit(MAX_ALLOWED_SIZE).runWith(Sink.seq(), mat);
        //#draining-to-list-safe

        strings.toCompletableFuture().get(1, TimeUnit.SECONDS);
      }
    };
  }

  public void drainSourceToListWithTake() throws Exception {
    new JavaTestKit(system) {
      {
        final Source<String, NotUsed> myData = Source.from(Arrays.asList("1", "2", "3"));
        final int MAX_ALLOWED_SIZE = 100;

        //#draining-to-list-safe
        // OK. Collect up until max-th elements only, then cancel upstream
        final CompletionStage<List<String>> strings =
                myData.take(MAX_ALLOWED_SIZE).runWith(Sink.seq(), mat);
        //#draining-to-list-safe

        strings.toCompletableFuture().get(1, TimeUnit.SECONDS);
      }
    };
  }
}
