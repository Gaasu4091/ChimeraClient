package me.alpha432.chimeraclient.util.inventory.strategy;

import me.alpha432.chimeraclient.util.inventory.Result;

public interface SwapStrategy {
   boolean swap(Result var1);

   boolean swapBack(int var1, Result var2);
}
