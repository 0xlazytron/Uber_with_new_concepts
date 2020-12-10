package com.paco.uber20.ui.rider;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RiderViewModel extends ViewModel {
    private MutableLiveData<String> mText;

    public RiderViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Rider Home");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
