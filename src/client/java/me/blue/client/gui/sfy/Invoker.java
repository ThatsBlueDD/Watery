package me.blue.client.gui.sfy;

@FunctionalInterface
public interface Invoker {
	Object call(Object self, Object[] args) throws Exception;
}
