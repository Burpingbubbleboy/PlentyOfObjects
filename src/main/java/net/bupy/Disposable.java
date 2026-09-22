package net.bupy;

/// Implementor promises to have some sort of resource that needs manually cleaned up after it is finished.
public interface Disposable {

    void dispose();
}
