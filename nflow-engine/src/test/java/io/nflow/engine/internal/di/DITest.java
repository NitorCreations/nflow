package io.nflow.engine.internal.di;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.*;

class DITest {

    DI di = new DI();

    @Test
    public void testStoreGet() {
        var c = new BasicClass();
        assertThat(di.store(c), sameInstance(c));
        assertThat(di.fetch(BasicClass.class), sameInstance(c));
        assertThat(di.get(BasicClass.class), sameInstance(c));
    }

    @Test
    public void testBasicCreate() {
        assertThat(di.get(BasicClass.class), instanceOf(BasicClass.class));
    }

    @Test
    public void testDiInjectWorks() {
        var d = di.get(DiInject.class);
        assertThat(d, instanceOf(DiInject.class));
        assertThat(d.di, sameInstance(di));
    }

    @Test
    public void testConstructorCreate() {
        var c = di.get(ConstructorClass.class);
        assertThat(c, instanceOf(ConstructorClass.class));
        assertThat(c.p, sameInstance(di.fetch(BasicClass.class)));
    }

    @Test
    public void testSimpleProviderCreate() {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicReference<ProvidedClass> ref = new AtomicReference<>();
        di.storeProvider(ProvidedClass.class, () -> {
            var i = new ProvidedClass(counter.incrementAndGet());
            ref.set(i);
            return i;
        });
        var p = di.get(ProvidedClass.class);
        assertThat(p, instanceOf(ProvidedClass.class));
        assertThat(p, sameInstance(ref.get()));
        assertThat(p.number, is(1));
        assertThat(counter.get(), is(1));
    }

    @Test
    public void testDIProviderCreate() {
        di.storeProvider(ConstructorClass.class, di -> new ConstructorClass(di.get(BasicClass.class)));
        var c = di.get(ConstructorClass.class);
        assertThat(c, instanceOf(ConstructorClass.class));
        assertThat(c.p, sameInstance(di.fetch(BasicClass.class)));
    }

    @Test
    public void testRecursiveProvider() {
        di.storeProvider(BasicClass.class, BasicClass::new);
        var c = di.get(ConstructorClass.class);
        assertThat(c, instanceOf(ConstructorClass.class));
        assertThat(c.p, sameInstance(di.fetch(BasicClass.class)));
    }

    public static class BasicClass {

    }

    public static class ConstructorClass {
        public final BasicClass p;

        public ConstructorClass(BasicClass p) {
            this.p = p;
        }
    }

    public static class ProvidedClass {
        public final int number;

        public ProvidedClass(int number) {
            this.number = number;
        }
    }

    public static class DiInject {
        public final DI di;

        public DiInject(DI di) {
            this.di = di;
        }

    }

}