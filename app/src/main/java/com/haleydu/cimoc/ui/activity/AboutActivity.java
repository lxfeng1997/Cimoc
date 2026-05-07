package com.haleydu.cimoc.ui.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.utils.StringUtils;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Hiroshi on 2016/9/21.
 */

public class AboutActivity extends BackActivity {

    @BindView(R.id.about_version_name)
    TextView mVersionName;
    @BindView(R.id.about_layout)
    View mLayoutView;

    // TODO 更新源独立出一个模块？
    @Override
    protected void initView() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            mVersionName.setText(StringUtils.format("Version  %s (%s)", info.versionName, info.versionCode));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.home_page_btn)
    void onHomeClick() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.home_page_url)));
        try {
            startActivity(intent);
        } catch (Exception e) {
            showSnackbar(R.string.about_resource_fail);
        }
    }

//    @OnClick(R.id.home_page_cimqus_btn)
//    void onCimqusClick() {
//        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.home_page_cimqus_url)));
//        try {
//            startActivity(intent);
//        } catch (Exception e) {
//            showSnackbar(R.string.about_resource_fail);
//        }
//    }

    @OnClick(R.id.about_support_btn)
    void onSupportClick() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.about_support_url)));
        try {
            startActivity(intent);
        } catch (Exception e) {
            showSnackbar(R.string.about_resource_fail);
        }
    }

    @OnClick(R.id.about_resource_btn)
    void onResourceClick() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.about_resource_url)));
        try {
            startActivity(intent);
        } catch (Exception e) {
            showSnackbar(R.string.about_resource_fail);
        }
    }

    @OnClick(R.id.about_resource_ori_btn)
    void onOriResourceClick() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.about_resource_ori_url)));
        try {
            startActivity(intent);
        } catch (Exception e) {
            showSnackbar(R.string.about_resource_fail);
        }
    }

    @OnClick(R.id.about_update_btn)
    void onUpdateClick() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.about_update_url)));
        try {
            startActivity(intent);
        } catch (Exception e) {
            showSnackbar(R.string.about_resource_fail);
        }
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.drawer_about);
    }

    @Override
    protected View getLayoutView() {
        return mLayoutView;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_about;
    }

}
