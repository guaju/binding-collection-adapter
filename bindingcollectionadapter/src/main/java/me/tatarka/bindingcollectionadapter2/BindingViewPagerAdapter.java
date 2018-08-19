package me.tatarka.bindingcollectionadapter2;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableList;
import androidx.databinding.ViewDataBinding;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.viewpager.widget.PagerAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * A {@link PagerAdapter} that binds items to layouts using the given {@link ItemBinding} or {@link
 * OnItemBind}. If you give it an {@link ObservableList} it will also updated itself based on
 * changes to that list.
 */
public class BindingViewPagerAdapter<T> extends PagerAdapter implements BindingCollectionAdapter<T> {
    private ItemBinding<T> itemBinding;
    private WeakReferenceOnListChangedCallback<T> callback;
    private List<T> items;
    private LayoutInflater inflater;
    private PageTitles<T> pageTitles;
    @Nullable
    private View currentView;
    private final ItemLifecycleHelper lifecycleHelper = new ItemLifecycleHelper();

    @Override
    public void setItemBinding(ItemBinding<T> itemBinding) {
        this.itemBinding = itemBinding;
    }

    @Override
    public void setLifecycleOwner(LifecycleOwner lifecycleOwner) {
        lifecycleHelper.setLifecycleOwner(lifecycleOwner);
    }

    @Nullable
    public LifecycleOwner getLifecycleOwner() {
        return lifecycleHelper.getLifecycleOwner();
    }

    @Override
    public ItemBinding<T> getItemBinding() {
        return itemBinding;
    }

    @Override
    public void setItems(@Nullable List<T> items) {
        if (this.items == items) {
            return;
        }
        if (this.items instanceof ObservableList) {
            ((ObservableList<T>) this.items).removeOnListChangedCallback(callback);
            callback = null;
        }
        if (items instanceof ObservableList) {
            callback = new WeakReferenceOnListChangedCallback<T>(this, (ObservableList<T>) items);
            ((ObservableList<T>) items).addOnListChangedCallback(callback);
        }
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public T getAdapterItem(int position) {
        return items.get(position);
    }

    @Override
    public ViewDataBinding onCreateBinding(LayoutInflater inflater, @LayoutRes int layoutRes, ViewGroup viewGroup) {
        return DataBindingUtil.inflate(inflater, layoutRes, viewGroup, false);
    }

    @Override
    public void onBindBinding(ViewDataBinding binding, int variableId, @LayoutRes int layoutRes, int position, T item) {
        if (itemBinding.bind(binding, item)) {
            binding.executePendingBindings();
        }
    }

    /**
     * Sets the page titles for the adapter.
     */
    public void setPageTitles(@Nullable PageTitles<T> pageTitles) {
        this.pageTitles = pageTitles;
    }

    @Override
    public int getCount() {
        return items == null ? 0 : items.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return pageTitles == null ? null : pageTitles.getPageTitle(position, items.get(position));
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        if (inflater == null) {
            inflater = LayoutInflater.from(container.getContext());
        }

        T item = items.get(position);
        itemBinding.onItemBind(position, item);

        ViewDataBinding binding = onCreateBinding(inflater, itemBinding.layoutRes(), container);
        View view = binding.getRoot();

        lifecycleHelper.destroyItemLifecycle(view);

        onBindBinding(binding, itemBinding.variableId(), itemBinding.layoutRes(), position, item);

        binding.setLifecycleOwner(lifecycleHelper.createItemLifecycle(view));

        container.addView(view);
        view.setTag(item);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        View view = (View) object;
        lifecycleHelper.destroyItemLifecycle(view);
        container.removeView(view);
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    @CallSuper
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        View view = (View) object;
        if (currentView != null) {
            lifecycleHelper.onDetachItem(currentView);
        }
        lifecycleHelper.onAttachItem(view);
        currentView = view;
        super.setPrimaryItem(container, position, object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int getItemPosition(@NonNull Object object) {
        T item = (T) ((View) object).getTag();
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                if (item == items.get(i)) {
                    return i;
                }
            }
        }
        return POSITION_NONE;
    }

    private static class WeakReferenceOnListChangedCallback<T> extends ObservableList.OnListChangedCallback<ObservableList<T>> {
        final WeakReference<BindingViewPagerAdapter<T>> adapterRef;

        WeakReferenceOnListChangedCallback(BindingViewPagerAdapter<T> adapter, ObservableList<T> items) {
            this.adapterRef = AdapterReferenceCollector.createRef(adapter, items, this);
        }

        @Override
        public void onChanged(ObservableList sender) {
            BindingViewPagerAdapter<T> adapter = adapterRef.get();
            if (adapter == null) {
                return;
            }
            Utils.ensureChangeOnMainThread();
            adapter.notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(ObservableList sender, int positionStart, int itemCount) {
            onChanged(sender);
        }

        @Override
        public void onItemRangeInserted(ObservableList sender, int positionStart, int itemCount) {
            onChanged(sender);
        }

        @Override
        public void onItemRangeMoved(ObservableList sender, int fromPosition, int toPosition, int itemCount) {
            onChanged(sender);
        }

        @Override
        public void onItemRangeRemoved(ObservableList sender, int positionStart, int itemCount) {
            onChanged(sender);
        }
    }

    public interface PageTitles<T> {
        CharSequence getPageTitle(int position, T item);
    }
}
