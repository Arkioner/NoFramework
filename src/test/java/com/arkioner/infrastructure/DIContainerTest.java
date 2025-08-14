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
        assertTrue(instance instanceof DependencyA);
        // Verify singleton behavior
        DependencyA instance2 = container.resolve(DependencyA.class);
        assertSame(instance, instance2);
    }

    @Test
    void testRegisterAndResolveClassWithCustomName() throws Exception {
        container.register(DependencyA.class, "custom");
        DependencyA instance = container.resolve(DependencyA.class, "custom");
        assertNotNull(instance);
        assertTrue(instance instanceof DependencyA);
        // Verify singleton behavior
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
        container.registerInstance(new DependencyA(), "dependencyA");
        container.registerInstance(new DependencyB(), "dependencyB");
        container.register(ComplexService.class);
        ComplexService service = container.resolve(ComplexService.class);
        assertNotNull(service);
        assertTrue(service instanceof ComplexService);
        // Verify dependencies using reflection
        java.lang.reflect.Field depAField = ComplexService.class.getDeclaredField("dependencyA");
        depAField.setAccessible(true);
        java.lang.reflect.Field depBField = ComplexService.class.getDeclaredField("dependencyB");
        depBField.setAccessible(true);
        assertTrue(depAField.get(service) instanceof DependencyA);
        assertTrue(depBField.get(service) instanceof DependencyB);
        // Verify singleton behavior
        assertSame(depAField.get(service), container.resolve(DependencyA.class, "dependencyA"));
        assertSame(depBField.get(service), container.resolve(DependencyB.class, "dependencyB"));
    }

    @Test
    void testInterfaceResolution() throws Exception {
        container.registerInstance(new DependencyA(), "dependencyA");
        container.register(SimpleService.class);
        Service service = container.resolve(Service.class);
        assertNotNull(service);
        assertTrue(service instanceof SimpleService);
        // Verify singleton behavior
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