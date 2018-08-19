package me.tatarka.bindingcollectionadapter2;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.databinding.OnRebindCallback;
import androidx.databinding.ViewDataBinding;

import android.os.Looper;

import androidx.annotation.LayoutRes;
import androidx.lifecycle.LifecycleOwner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Helper databinding utilities. May be made public some time in the future if they prove to be
 * useful.
 */
class Utils {
    private static final String TAG = "BCAdapters";

    @Nullable
    private static Field lifecycleOwnerField;
    private static boolean fieldFaild;

    /**
     * Helper to throw an exception when {@link androidx.databinding.ViewDataBinding#setVariable(int,
     * Object)} returns false.
     */
    static void throwMissingVariable(ViewDataBinding binding, int bindingVariable, @LayoutRes int layoutRes) {
        Context context = binding.getRoot().getContext();
        Resources resources = context.getResources();
        String layoutName = resources.getResourceName(layoutRes);
        String bindingVariableName = DataBindingUtil.convertBrIdToString(bindingVariable);
        throw new IllegalStateException("Could not bind variable '" + bindingVariableName + "' in layout '" + layoutName + "'");
    }

    /**
     * Returns the lifecycle owner from a {@code ViewDataBinding} using reflection.
     */
    @Nullable
    @MainThread
    static LifecycleOwner getLifecycleOwner(ViewDataBinding binding) {
        if (!fieldFaild && lifecycleOwnerField == null) {
            try {
                lifecycleOwnerField = ViewDataBinding.class.getDeclaredField("mLifecycleOwner");
                lifecycleOwnerField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                fieldFaild = true;
                return null;
            }
        }
        if (lifecycleOwnerField == null) {
            return null;
        }
        try {
            return (LifecycleOwner) lifecycleOwnerField.get(binding);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * Ensures the call was made on the main thread. This is enforced for all ObservableList change
     * operations.
     */
    static void ensureChangeOnMainThread() {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new IllegalStateException("You must only modify the ObservableList on the main thread.");
        }
    }

    /**
     * Constructs a binding adapter class from it's class name using reflection.
     */
    @SuppressWarnings("unchecked")
    static <T, A extends BindingCollectionAdapter<T>> A createClass(Class<? extends BindingCollectionAdapter> adapterClass, ItemBinding<T> itemBinding) {
        try {
            return (A) adapterClass.getConstructor(ItemBinding.class).newInstance(itemBinding);
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
    }
}
