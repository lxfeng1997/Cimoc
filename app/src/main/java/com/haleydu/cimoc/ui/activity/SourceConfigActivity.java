package com.haleydu.cimoc.ui.activity;

import android.view.View;
import android.widget.TextView;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.source.SourceConfig;

import butterknife.BindView;

public class SourceConfigActivity extends BackActivity {

    @BindView(R.id.source_config_layout)
    View mSourceConfigLayout;
    @BindView(R.id.source_config_text)
    TextView mSourceConfigText;

    @Override
    protected void initView() {
        super.initView();
        mSourceConfigText.setText(SourceConfig.describeConfig());
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.settings_source_config_list);
    }

    @Override
    protected View getLayoutView() {
        return mSourceConfigLayout;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_source_config;
    }
}
