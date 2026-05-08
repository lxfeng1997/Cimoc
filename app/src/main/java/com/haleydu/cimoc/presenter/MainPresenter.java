package com.haleydu.cimoc.presenter;

import com.haleydu.cimoc.App;
import com.haleydu.cimoc.manager.ComicManager;
import com.haleydu.cimoc.manager.PreferenceManager;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.MiniComic;
import com.haleydu.cimoc.rx.RxEvent;
import com.haleydu.cimoc.source.SourceConfig;
import com.haleydu.cimoc.ui.view.MainView;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by Hiroshi on 2016/9/21.
 */

public class MainPresenter extends BasePresenter<MainView> {

    private ComicManager mComicManager;

    @Override
    protected void onViewAttach() {
        mComicManager = ComicManager.getInstance(mBaseView);
    }

    @Override
    protected void initSubscription() {
        addSubscription(RxEvent.EVENT_COMIC_READ, new Action1<RxEvent>() {
            @Override
            public void call(RxEvent rxEvent) {
                MiniComic comic = (MiniComic) rxEvent.getData();
                mBaseView.onLastChange(comic.getId(), comic.getSource(), comic.getCid(),
                        comic.getTitle(), comic.getCover());
            }
        });
    }

    public boolean checkLocal(long id) {
        Comic comic = mComicManager.load(id);
        return comic != null && comic.getLocal();
    }

    public void loadLast() {
        mCompositeSubscription.add(mComicManager.loadLast()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Comic>() {
                    @Override
                    public void call(Comic comic) {
                        if (comic != null) {
                            mBaseView.onLastLoadSuccess(comic.getId(), comic.getSource(), comic.getCid(), comic.getTitle(), comic.getCover());
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mBaseView.onLastLoadFail();
                    }
                }));
    }

    public void getSourceBaseUrl() {
        mCompositeSubscription.add(
                Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        OkHttpClient client = App.getHttpClient();
                        if (client == null) {
                            subscriber.onError(new Exception());
                            return;
                        }
                        Request request = new Request.Builder().url(SourceConfig.getSourceUrl()).build();
                        Response response = null;
                        try {
                            response = client.newCall(request).execute();
                            if (response.isSuccessful()) {
                                String json = response.body().string();
                                subscriber.onNext(SourceConfig.resolveRemoteDomains(json, client));
                                subscriber.onCompleted();
                                return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (response != null) {
                                response.close();
                            }
                        }
                        subscriber.onError(new Exception());
                    }
                }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<String>() {
                            @Override
                            public void call(String json) {
                                SourceConfig.update(json);
                                try {
                                    JSONObject object = new JSONObject(json);
                                    String HHAAZZ = object.optString("HHAAZZ", "");
                                    String sw = object.optString("sw", "");
                                    if (!HHAAZZ.equals(App.getPreferenceManager().getString(PreferenceManager.PREF_HHAAZZ_BASEURL, ""))){
                                        App.getPreferenceManager().putString(PreferenceManager.PREF_HHAAZZ_BASEURL, HHAAZZ);
                                    }
                                    if (!sw.equals(App.getPreferenceManager().getString(PreferenceManager.PREF_HHAAZZ_SW, ""))){
                                        App.getPreferenceManager().putString(PreferenceManager.PREF_HHAAZZ_SW, sw);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                            }
                        }));
    }
}
