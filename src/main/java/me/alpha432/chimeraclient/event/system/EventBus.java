package me.alpha432.chimeraclient.event.system;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import me.alpha432.chimeraclient.event.Event;

public class EventBus {
   private final Map<Class<?>, CopyOnWriteArrayList<Listener>> listeners = new ConcurrentHashMap<>();

   public void register(Object host) {
      this.register(host, host.getClass());
   }

   public void unregister(Object host) {
      for (CopyOnWriteArrayList<Listener> list : this.listeners.values()) {
         list.removeIf(listener -> listener.getHost().equals(host));
      }
   }

   public boolean post(Event event) {
      List<Listener> list = this.listeners.get(event.getClass());
      if (list == null) {
         return false;
      } else {
         for (Listener listener : list) {
            listener.invoke(event);
            if (event.isCancelled()) {
               return true;
            }
         }

         return false;
      }
   }

   private void register(Object host, Class<?> klass) {
      for (Method method : klass.getDeclaredMethods()) {
         Subscribe subscribe = method.getAnnotation(Subscribe.class);
         if (subscribe != null) {
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1) {
               Class<?> eventType = params[0];
               Listener listener = Listener.of(host, subscribe.priority(), method);
               List<Listener> registry = this.listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
               this.register(registry, listener);
            }
         }
      }

      if (klass.getSuperclass() != null) {
         this.register(host, klass.getSuperclass());
      }
   }

   private void register(List<Listener> registry, Listener target) {
      int i = 0;

      for (Listener listener : registry) {
         i++;
         if (target.priority() > listener.priority()) {
            break;
         }
      }

      registry.add(i, target);
   }
}
