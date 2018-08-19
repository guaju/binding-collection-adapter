package me.tatarka.bindingcollectionadapter2;

import android.view.View;

import java.util.IdentityHashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.OnLifecycleEvent;

class ItemLifecycleHelper {

    @Nullable
    private LifecycleOwner lifecycleOwner;
    private final IdentityHashMap<View, ItemLifecycle> itemLifecycles = new IdentityHashMap<>();

    void setLifecycleOwner(@Nullable LifecycleOwner lifecycleOwner) {
        if (this.lifecycleOwner == lifecycleOwner) {
            return;
        }
        if (this.lifecycleOwner != null) {
            for (Map.Entry<View, ItemLifecycle> entry : itemLifecycles.entrySet()) {
                ItemLifecycle oldItemLifecycle = entry.getValue();
                oldItemLifecycle.onDestroy();
                this.lifecycleOwner.getLifecycle().removeObserver(oldItemLifecycle);
                if (lifecycleOwner != null) {
                    ItemLifecycle itemLifecycle = new ItemLifecycle();
                    lifecycleOwner.getLifecycle().addObserver(itemLifecycle);
                    entry.setValue(itemLifecycle);
                }
            }
        }
        if (lifecycleOwner == null) {
            itemLifecycles.clear();
        }
        this.lifecycleOwner = lifecycleOwner;
    }

    @Nullable
    LifecycleOwner createItemLifecycle(View view) {
        if (lifecycleOwner == null) {
            return null;
        }
        ItemLifecycle itemLifecycle = new ItemLifecycle();
        lifecycleOwner.getLifecycle().addObserver(itemLifecycle);
        itemLifecycles.put(view, itemLifecycle);
        return itemLifecycle;
    }

    void onAttachItem(View view) {
        if (lifecycleOwner == null) {
            return;
        }
        ItemLifecycle itemLifecycle = itemLifecycles.get(view);
        if (itemLifecycle != null) {
            itemLifecycle.onAttach();
        }
    }

    void onDetachItem(View view) {
        if (lifecycleOwner == null) {
            return;
        }
        ItemLifecycle itemLifecycle = itemLifecycles.get(view);
        if (itemLifecycle != null) {
            itemLifecycle.onDetach();
        }
    }

    void destroyItemLifecycle(View view) {
        if (lifecycleOwner == null) {
            return;
        }
        ItemLifecycle itemLifecycle = itemLifecycles.remove(view);
        if (itemLifecycle != null) {
            itemLifecycle.onDestroy();
            lifecycleOwner.getLifecycle().removeObserver(itemLifecycle);
        }
    }

    @Nullable
    public LifecycleOwner getLifecycleOwner() {
        return lifecycleOwner;
    }

    static class ItemLifecycle implements LifecycleOwner, LifecycleObserver {
        private final LifecycleRegistry registry = new LifecycleRegistry(this);
        private boolean attached;

        @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
        public void onLifecycleEvent(LifecycleOwner owner, Lifecycle.Event event) {
            if (attached || !owner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                registry.handleLifecycleEvent(event);
            }
            if (event == Lifecycle.Event.ON_DESTROY) {
                owner.getLifecycle().removeObserver(this);
            }
        }

        void onAttach() {
            attached = true;
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        }

        void onDetach() {
            attached = false;
            registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        }

        void onDestroy() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        }

        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            return registry;
        }
    }
}
