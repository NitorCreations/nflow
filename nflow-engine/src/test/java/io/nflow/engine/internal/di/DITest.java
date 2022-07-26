package io.nflow.engine.internal.di;

import org.junit.jupiter.api.Test;

import javax.swing.plaf.basic.BasicIconFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;

class DITest {

    DI di = new DI();

    @Test
    public void testStoreGet() {
        var c = new BasicClass();
        assertSame(c, di.store(c));
        assertSame(c, di.get(BasicClass.class));
        assertSame(c, di.getOrCreate(BasicClass.class));
    }

    @Test
    public void testBasicCreate() {
        assertThat(di.getOrCreate(BasicClass.class), instanceOf(BasicClass.class));
    }

    @Test
    public void testConstructorCreate() {
        var c = di.getOrCreate(ConstructorClass.class);
        assertThat(c, instanceOf(ConstructorClass.class));
        assertSame(c.p, di.get(BasicClass.class));
    }

    public static class BasicClass {

    }

    public static class ConstructorClass {
        public final BasicClass p;

        public ConstructorClass(DI di, BasicClass p) {
            this.p = p;
        }
    }
}