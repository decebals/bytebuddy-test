import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Decebal Suiu
 */
public class Test {

    public static void main(String[] args) {
        final StartPage target = new StartPage();
        List<Class<?>> interfaces = new ArrayList<>();
        interfaces.add(Sender.class);

        try {
            StartPage page = new ByteBuddy()
                .subclass(target.getClass())
                .name(target.getClass().getCanonicalName() + "$$Proxy")
//                .method(isDeclaredBy(anyOf(interfaces)))
                    .method(md -> interfaces.stream().anyMatch(iface -> md.getDeclaringType().asErasure().isAssignableTo(iface)))
                    .intercept(InvocationHandlerAdapter.of(new MyInvocationHandler(target)).withMethodCache())
                    .make()
                    .load(Test.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance();
            page.send_A();
            page.send_B();
            page.sayGreetings();
            page.sayHello();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // The output is:
        /*
        MyInvocationHandler.invoke public void Test$Page.send_A() <-- OK
        Send A
        MyInvocationHandler.invoke public void Test$Page.send_B() <-- OK
        Send B
        MyInvocationHandler.invoke public void Test$StartPage.sayGreetings() <-- WRONG
        Greetings!
        MyInvocationHandler.invoke public void Test$Page.sayHello() <-- WRONG
        Hello!
         */
    }

    interface Sender {

        void send_A();

        void send_B();

    }

    public static class Page implements Sender {

        public void send_A() {
            System.out.println("Send A");
        }

        public void send_B() {
            System.out.println("Send B");
        }

        public void sayHello() {
            System.out.println("Hello!");
        }

    }

    public static class StartPage extends Page {

        public void sayGreetings() {
            System.out.println("Greetings!");
        }

    }

    public static class MyInvocationHandler implements InvocationHandler {

        private final Object target;

        public MyInvocationHandler(final Object target) {
            this.target = target;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("MyInvocationHandler.invoke " + method);
            return method.invoke(target, args);
        }

    }

}
