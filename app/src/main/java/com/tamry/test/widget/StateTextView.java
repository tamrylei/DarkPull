package com.tamry.test.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * Created by tamrylei on 2020/7/30.
 */
public class StateTextView extends AppCompatTextView {

    public StateTextView(Context context) {
        super(context);
    }

    public StateTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public StateTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (isPressed() || !isEnabled()) {
            setAlpha(0.5f);
        } else {
            setAlpha(1.0f);
        }
    }
}
