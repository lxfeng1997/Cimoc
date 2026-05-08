package org.adw.library.widgets.discreteseekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatSeekBar;

import com.haleydu.cimoc.R;

public class DiscreteSeekBar extends AppCompatSeekBar {

    private int min;
    private int max;
    private OnProgressChangeListener listener;

    public DiscreteSeekBar(Context context) {
        super(context);
        init();
    }

    public DiscreteSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        readAttrs(context, attrs);
        init();
    }

    public DiscreteSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readAttrs(context, attrs);
        init();
    }

    private void readAttrs(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.DiscreteSeekBar);
        min = array.getInt(R.styleable.DiscreteSeekBar_dsb_min, 0);
        array.recycle();
        max = super.getMax() + min;
        super.setMax(Math.max(0, max - min));
    }

    private void init() {
        super.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (listener != null) {
                    listener.onProgressChanged(DiscreteSeekBar.this, progress + min, fromUser);
                }
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                if (listener != null) {
                    listener.onStartTrackingTouch(DiscreteSeekBar.this);
                }
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                if (listener != null) {
                    listener.onStopTrackingTouch(DiscreteSeekBar.this);
                }
            }
        });
    }

    public void setOnProgressChangeListener(OnProgressChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void setMax(int max) {
        this.max = max;
        super.setMax(Math.max(0, max - min));
    }

    public void setMin(int min) {
        int current = getProgress();
        this.min = min;
        setMax(max);
        setProgress(current);
    }

    @Override
    public int getMax() {
        return max;
    }

    @Override
    public void setProgress(int progress) {
        super.setProgress(Math.max(0, progress - min));
    }

    @Override
    public int getProgress() {
        return super.getProgress() + min;
    }

    public boolean isRtl() {
        return false;
    }

    public interface OnProgressChangeListener {
        void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser);

        void onStartTrackingTouch(DiscreteSeekBar seekBar);

        void onStopTrackingTouch(DiscreteSeekBar seekBar);
    }
}
