package com.arkioner.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DIContainerTest {
    private DIContainer container;

    @BeforeEach
    void setUp() {
        container = new DIContainer();
    }

    @Test
    void testRegisterAndResolveClassWithDefaultName() throws Exception {
        container.register(DependencyA.class);
        DependencyA instance = container.resolve(DependencyA.class);
        assertNotNull(instance);

        DependencyA instance2 = container.resolve(DependencyA.class);
        assertSame(instance, instance2);
    }

    @Test
    void testRegisterAndResolveClassWithCustomName() throws Exception {
        container.register(DependencyA.class, "custom");
        DependencyA instance = container.resolve(DependencyA.class, "custom");
        assertNotNull(instance);

        DependencyA instance2 = container.resolve(DependencyA.class, "custom");
        assertSame(instance, instance2);
    }

    @Test
    void testRegisterInstanceAndResolveWithDefaultName() throws Exception {
        DependencyA instance = new DependencyA();
        container.registerInstance(instance);
        DependencyA resolved = container.resolve(DependencyA.class);
        assertSame(instance, resolved);
    }

    @Test
    void testRegisterInstanceAndResolveWithCustomName() throws Exception {
        DependencyA instance = new DependencyA();
        container.registerInstance(instance, "custom");
        DependencyA resolved = container.resolve(DependencyA.class, "custom");
        assertSame(instance, resolved);
    }

    @Test
    void testDependencyInjectionWithNamedParameters() throws Exception {
        DependencyA depA = new DependencyA();
        DependencyB depB = new DependencyB();
        container.registerInstance(depA, "dependencyA");
        container.registerInstance(depB, "dependencyB");
        container.register(ComplexService.class);
        ComplexService service = container.resolve(ComplexService.class);
        assertNotNull(service);

        java.lang.reflect.Field depAField = ComplexService.class.getDeclaredField("dependencyA");
        depAField.setAccessible(true);
        java.lang.reflect.Field depBField = ComplexService.class.getDeclaredField("dependencyB");
        depBField.setAccessible(true);

        assertSame(depA, depAField.get(service));
        assertSame(depB, depBField.get(service));
        ComplexService service2 = container.resolve(ComplexService.class);
        assertSame(service, service2);
    }

    @Test
    void testDependencyInjectionWithRecord() throws Exception {
        DependencyA depA = new DependencyA();
        DependencyB depB = new DependencyB();
        container.registerInstance(depA, "dependencyA");
        container.registerInstance(depB, "dependencyB");
        container.register(ComplexRecord.class);
        ComplexRecord record = container.resolve(ComplexRecord.class);
        assertNotNull(record);

        assertSame(depA, record.dependencyA());
        assertSame(depB, record.dependencyB());

        ComplexRecord record2 = container.resolve(ComplexRecord.class);
        assertSame(record, record2);
    }

    @Test
    void testInterfaceResolution() throws Exception {
        container.registerInstance(new DependencyA(), "dependencyA");
        container.register(SimpleService.class);
        Service service = container.resolve(Service.class);
        assertNotNull(service);

        Service service2 = container.resolve(Service.class);
        assertSame(service, service2);
    }

    @Test
    void testCyclicDependencyDetection() {
        container.register(CyclicA.class);
        container.register(CyclicB.class);
        Exception exception = assertThrows(IllegalStateException.class, () -> container.resolve(CyclicA.class));
        assertTrue(exception.getMessage().contains("Cyclic dependency detected"));
    }

    @Test
    void testMissingBinding() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> container.resolve(DependencyA.class, "missing"));
        assertTrue(exception.getMessage().contains("No binding found for com.arkioner.infrastructure.DIContainerTest$DependencyA with name missing"));
    }

    @Test
    void testUnresolvableConstructor() {
        container.register(Unresolvable.class);
        Exception exception = assertThrows(IllegalStateException.class, () -> container.resolve(Unresolvable.class));
        assertTrue(exception.getMessage().contains("No suitable constructor found for com.arkioner.infrastructure.DIContainerTest$Unresolvable"));
    }

    @Test
    void testNullAndEmptyChecks() {
        assertThrows(IllegalArgumentException.class, () -> container.register(null));
        assertThrows(IllegalArgumentException.class, () -> container.registerInstance(null));
        assertThrows(IllegalArgumentException.class, () -> container.register(null, "bean"));
        assertThrows(IllegalArgumentException.class, () -> container.registerInstance(null, "bean"));
        assertThrows(IllegalArgumentException.class, () -> container.resolve(null));
        assertThrows(IllegalArgumentException.class, () -> container.resolve(null, "bean"));
        assertThrows(IllegalArgumentException.class, () -> container.register(DependencyA.class, null));
        assertThrows(IllegalArgumentException.class, () -> container.registerInstance(new DependencyA(), null));
        assertThrows(IllegalArgumentException.class, () -> container.register(DependencyA.class, ""));
        assertThrows(IllegalArgumentException.class, () -> container.registerInstance(new DependencyA(), ""));
    }

    // Inner classes for testing
    static class DependencyA {
        public DependencyA() {}
    }

    static class DependencyB {
        public DependencyB() {}
    }

    interface Service {
        void perform();
    }

    static class SimpleService implements Service {
        private final DependencyA dependencyA;

        public SimpleService(DependencyA dependencyA) {
            this.dependencyA = dependencyA;
        }

        @Override
        public void perform() {}
    }

    static class ComplexService {
        private final DependencyA dependencyA;
        private final DependencyB dependencyB;

        public ComplexService(DependencyA dependencyA, DependencyB dependencyB) {
            this.dependencyA = dependencyA;
            this.dependencyB = dependencyB;
        }
    }

    record ComplexRecord(DependencyA dependencyA, DependencyB dependencyB) {}

    static class CyclicA {
        public CyclicA(CyclicB cyclicB) {}
    }

    static class CyclicB {
        public CyclicB(CyclicA cyclicA) {}
    }

    static class Unresolvable {
        public Unresolvable(String missing) {}
    }
}