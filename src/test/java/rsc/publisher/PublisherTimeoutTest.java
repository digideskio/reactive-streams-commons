package rsc.publisher;

import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Publisher;
import rsc.processor.DirectProcessor;
import rsc.test.TestSubscriber;
import rsc.util.ConstructorTestBuilder;

public class PublisherTimeoutTest {

    @Test
    public void constructors() {
        ConstructorTestBuilder ctb = new ConstructorTestBuilder(PublisherTimeout.class);
        
        ctb.addRef("source", PublisherNever.instance());
        ctb.addRef("firstTimeout", PublisherNever.instance());
        ctb.addRef("itemTimeout", (Function<Object, Publisher<Object>>)v -> PublisherNever.instance());
        ctb.addRef("other", PublisherNever.instance());
        
        ctb.test();
    }

    @Test
    public void noTimeout() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        new PublisherTimeout<>(new PublisherRange(1, 10), PublisherNever.instance(), v -> PublisherNever
          .instance()).subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
          .assertComplete()
          .assertNoError();
    }

    @Test
    public void immediateTimeout() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        new PublisherTimeout<>(new PublisherRange(1, 10), PublisherEmpty.instance(), v -> PublisherNever
          .instance()).subscribe(ts);

        ts.assertNoValues()
          .assertNotComplete()
          .assertError(TimeoutException.class);
    }

    @Test
    public void firstElemenetImmediateTimeout() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        new PublisherTimeout<>(new PublisherRange(1, 10), PublisherNever.instance(), v -> PublisherEmpty
          .instance()).subscribe(ts);

        ts.assertValue(1)
          .assertNotComplete()
          .assertError(TimeoutException.class);
    }

    @Test
    public void immediateTimeoutResume() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        new PublisherTimeout<>(new PublisherRange(1, 10), PublisherEmpty.instance(), v -> PublisherNever
          .instance(), new PublisherRange(1, 10)).subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
          .assertComplete()
          .assertNoError();
    }

    @Test
    public void firstElemenetImmediateResume() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        new PublisherTimeout<>(new PublisherRange(1, 10), PublisherNever.instance(), v -> PublisherEmpty
          .instance(), new PublisherRange(1, 10)).subscribe(ts);

        ts.assertValues(1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
          .assertComplete()
          .assertNoError();
    }

    @Test
    public void oldTimeoutHasNoEffect() {
        DirectProcessor<Integer> source = new DirectProcessor<>();

        DirectProcessor<Integer> tp = new DirectProcessor<>();

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        new PublisherTimeout<>(source, tp, v -> PublisherNever.instance(), new PublisherRange(1, 10)).subscribe
          (ts);

        source.onNext(0);

        tp.onNext(1);

        source.onComplete();

        Assert.assertFalse("Timeout has subscribers?", tp.hasDownstreams());

        ts.assertValue(0)
          .assertComplete()
          .assertNoError();
    }

    @Test
    public void oldTimeoutCompleteHasNoEffect() {
        DirectProcessor<Integer> source = new DirectProcessor<>();

        DirectProcessor<Integer> tp = new DirectProcessor<>();

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        new PublisherTimeout<>(source, tp, v -> PublisherNever.instance(), new PublisherRange(1, 10)).subscribe
          (ts);

        source.onNext(0);

        tp.onComplete();

        source.onComplete();

        Assert.assertFalse("Timeout has subscribers?", tp.hasDownstreams());

        ts.assertValue(0)
          .assertComplete()
          .assertNoError();
    }

    @Test
    public void oldTimeoutErrorHasNoEffect() {
        DirectProcessor<Integer> source = new DirectProcessor<>();

        DirectProcessor<Integer> tp = new DirectProcessor<>();

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        new PublisherTimeout<>(source, tp, v -> PublisherNever.instance(), new PublisherRange(1, 10)).subscribe
          (ts);

        source.onNext(0);

        tp.onError(new RuntimeException("forced failure"));

        source.onComplete();

        Assert.assertFalse("Timeout has subscribers?", tp.hasDownstreams());

        ts.assertValue(0)
          .assertComplete()
          .assertNoError();
    }

    @Test
    public void itemTimeoutThrows() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        new PublisherTimeout<>(new PublisherRange(1, 10), PublisherNever.instance(), v -> {
            throw new RuntimeException("forced failure");
        }).subscribe(ts);

        ts.assertValue(1)
          .assertNotComplete()
          .assertError(RuntimeException.class)
          .assertErrorMessage("forced failure");
    }

    @Test
    public void itemTimeoutReturnsNull() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        new PublisherTimeout<>(new PublisherRange(1, 10), PublisherNever.instance(), v -> null).subscribe(ts);

        ts.assertValue(1)
          .assertNotComplete()
          .assertError(NullPointerException.class);
    }

    @Test
    public void firstTimeoutError() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        new PublisherTimeout<>(new PublisherRange(1, 10), new PublisherError<>(new RuntimeException("forced " +
          "failure")), v -> PublisherNever.instance()).subscribe(ts);

        ts.assertNoValues()
          .assertNotComplete()
          .assertError(RuntimeException.class)
          .assertErrorMessage("forced failure");
    }

    @Test
    public void itemTimeoutError() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        new PublisherTimeout<>(new PublisherRange(1, 10), PublisherNever.instance(), v -> new PublisherError<>
          (new RuntimeException("forced failure"))).subscribe(ts);

        ts.assertValue(1)
          .assertNotComplete()
          .assertError(RuntimeException.class)
          .assertErrorMessage("forced failure");
    }

    @Test
    public void timeoutRequested() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        DirectProcessor<Integer> source = new DirectProcessor<>();

        DirectProcessor<Integer> tp = new DirectProcessor<>();
        
        source.timeout(tp, v -> tp).subscribe(ts);
        
        tp.onNext(1);
        
        source.onNext(2);
        source.onComplete();
        
        ts.assertNoValues()
        .assertError(TimeoutException.class)
        .assertNotComplete();
    }
}
