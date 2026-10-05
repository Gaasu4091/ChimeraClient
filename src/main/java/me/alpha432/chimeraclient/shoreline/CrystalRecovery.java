package me.alpha432.chimeraclient.shoreline;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CrystalRecovery {
   private final Map<Integer, Long> quarantined = new HashMap<>();

   public boolean quarantine(int var1, long var2) {
      return this.quarantined.putIfAbsent(var1, var2) == null;
   }

   public boolean blocked(int var1, long var2) {
      Long var4 = this.quarantined.get(var1);
      return var4 != null && var2 - var4 < 1000L;
   }

   public Set<Integer> prune(Set<Integer> var1, long var2) {
      HashSet var4 = new HashSet();
      this.quarantined.entrySet().removeIf(var4x -> {
         boolean var5 = !var1.contains(var4x.getKey()) || var2 - var4x.getValue() >= 1000L;
         if (var5) {
            var4.add(var4x.getKey());
         }

         return var5;
      });
      return var4;
   }

   public void clear() {
      this.quarantined.clear();
   }

   public static boolean awaiting(Long var0, long var1, long var3) {
      return var0 != null && var1 - var0 < Math.max(50L, var3);
   }
}
