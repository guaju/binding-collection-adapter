package me.tatarka.bindingcollectionadapter.sample;

import androidx.annotation.MainThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class ItemViewModel {
    public boolean checkable;
    private int index;
    private final MutableLiveData<Boolean> checked;

    public ItemViewModel(int index) {
        this.index = index;
        checked = new MutableLiveData<>();
        checked.setValue(false);
    }

    public int getIndex() {
        return index;
    }

    public LiveData<Boolean> isChecked() {
        return checked;
    }

    @MainThread
    public boolean onToggleChecked() {
        if (!checkable) {
            return false;
        }
        checked.setValue(!checked.getValue());
        return true;
    }
}
