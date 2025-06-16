package com.example.navigationapidemo;

import android.content.Context;

import androidx.core.util.Consumer;

import org.vosk.Model;
import org.vosk.android.StorageService;

public class VoskModelManager {

    private static VoskModelManager instance;
    private Model model;
    private final Context context;

    private VoskModelManager(Context context) {
        this.context = context;
    }

    public static VoskModelManager getInstance(Context context) {
        if (instance == null) {
            instance = new VoskModelManager(context);
        }
        return instance;
    }

    public void initModel(final ModelInitializationCallback callback) {
        StorageService.unpack(context, "model-en-us", "model",
                model -> { // Using lambda expression
                    this.model = model;
                    callback.onSuccess(model);
                },
                exception -> callback.onFailure("Failed to unpack the model: " + exception.getMessage()));
    }

    public Model getModel() {
        return model;
    }

    public interface ModelInitializationCallback {
        void onSuccess(Model model);
        void onFailure(String errorMessage);
    }
}