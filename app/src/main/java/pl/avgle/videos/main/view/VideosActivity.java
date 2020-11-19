package pl.avgle.videos.main.view;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import cn.jzvd.Jzvd;
import jp.wasabeef.blurry.Blurry;
import pl.avgle.videos.R;
import pl.avgle.videos.adapter.VideosAdapter;
import pl.avgle.videos.bean.SelectBean;
import pl.avgle.videos.bean.VideoBean;
import pl.avgle.videos.config.QueryType;
import pl.avgle.videos.config.VideosOrderType;
import pl.avgle.videos.custom.CustomLoadMoreView;
import pl.avgle.videos.custom.JZPlayer;
import pl.avgle.videos.database.DatabaseUtil;
import pl.avgle.videos.main.base.BaseActivity;
import pl.avgle.videos.main.contract.VideoContract;
import pl.avgle.videos.main.presenter.VideoPresenter;
import pl.avgle.videos.util.StatusBarUtil;
import pl.avgle.videos.util.Utils;

public class VideosActivity extends BaseActivity<VideoContract.View, VideoPresenter> implements VideoContract.View,JZPlayer.CompleteListener {
    @BindView(R.id.rv_list)
    RecyclerView mRecyclerView;
    private VideosAdapter mVideosAdapter;
    @BindView(R.id.mSwipe)
    SwipeRefreshLayout mSwipe;
    private List<VideoBean.ResponseBean.VideosBean> list = new ArrayList<>();
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    //每页显示多少数量
    private int limit = 50;
    //页数
    private int nowPage = 0;
    //activity传递的参数
    private int type = QueryType.DEFAULT;
    private String toolbarTitle = "";
    private String order = "";
    private int cid = 0;
    private String time = "";
    private String img;
    @BindView(R.id.title_img)
    ImageView imageView;
    @BindView(R.id.collaps_toolbar_layout)
    CollapsingToolbarLayout st;
    private boolean isLoading;
    //是否是第一次加载
    private boolean isLoad = false;
    //是否有更更多
    private boolean hasMore = true;
    private boolean isErr = true;
    private SearchView mSearchView;

    protected JZPlayer player;
    protected int index;

    private BottomSheetDialog mOrderDialog;
    private RadioGroup mOrderGroup;

    @Override
    public void complete() {
        removePlayer(player);
    }

    @Override
    protected void initBeforeView() {}

    @Override
    protected int getLayout() {
        return R.layout.activity_videos;
    }

    @Override
    protected VideoPresenter createPresenter() {
        if (type == QueryType.CHANNEL_TYPE)
            return new VideoPresenter(isLoad, type, nowPage, cid, limit, order, this);
        else if (type == QueryType.COLLECTIONS_TYPE || type == QueryType.QUERY_TYPE)
            return new VideoPresenter(isLoad, type, toolbarTitle, nowPage, limit, order, this);
        else if (type == QueryType.NEW_TYPE || type == QueryType.HOT_TYPE || type == QueryType.FEATURED_TYPE)
            return new VideoPresenter(isLoad, type, nowPage, order, time, limit, this);
        else
            return null;
    }

    @Override
    protected void loadData() {
        mPresenter.loadData();
    }

    @Override
    protected void initData() {
        if (Utils.checkHasNavigationBar(this))
            StatusBarUtil.setTransparentForImageView(this, toolbar);
        else
            StatusBarUtil.setTransparentForImageView(this, null);
        getBundle();
        initToolbar();
        initSwipe();
        initAdapter();
        initImgs();
        initOrderDialog();
    }

    public void getBundle() {
        Bundle bundle = getIntent().getExtras();
        if (null != bundle && !bundle.isEmpty()) {
            toolbarTitle = bundle.getString("name");
            type = bundle.getInt("type");
            cid = bundle.getInt("cid");
            order = bundle.getString("order");
            img = bundle.getString("img");
            time = bundle.getString("time");
        }
    }

    public void initToolbar() {
        st.setTitle(toolbarTitle);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> supportFinishAfterTransition());
    }

    public void initSwipe() {
        mSwipe.setColorSchemeResources(R.color.colorAccent, R.color.blue500, R.color.purple500);
        mSwipe.setOnRefreshListener(() -> {
            mVideosAdapter.setNewData(list = new ArrayList<>());
            isLoad = false;
            nowPage = 0;
            mPresenter = createPresenter();
            loadData();
        });
    }

    public void initAdapter() {
        if (Utils.checkHasNavigationBar(this))
            mRecyclerView.setPadding(0,0,0, Utils.getNavigationBarHeight(this));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mVideosAdapter = new VideosAdapter(this, list);
        mVideosAdapter.openLoadAnimation(BaseQuickAdapter.SLIDEIN_BOTTOM);
        mRecyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {

            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                JZPlayer player = view.findViewById(R.id.player);
                detachedFromWindow(player);
            }
        });
        mVideosAdapter.setOnItemClickListener((adapter, view, position) -> {
            VideoBean.ResponseBean.VideosBean bean = list.get(position);
            mBottomSheetDialogTitle.setText(bean.getTitle());
            selectBeanList = new ArrayList<>();
            selectBeanList.add(new SelectBean(Utils.getString(R.string.preview), R.drawable.baseline_videocam_white_48dp));
            selectBeanList.add(new SelectBean(Utils.getString(R.string.videos), R.drawable.baseline_movie_white_48dp));
            selectBeanList.add(new SelectBean(Utils.getString(R.string.add_favorite), R.drawable.baseline_add_white_48dp));
            selectBeanList.add(new SelectBean(Utils.getString(R.string.browser), R.drawable.baseline_open_in_new_white_48dp));
            selectAdapter.setNewData(selectBeanList);
            selectAdapter.setOnItemClickListener((selectAdapter, selectView, selectPosition) -> {
                switch (selectPosition) {
                    case 0:
                        removePlayer(player);
                        index = position;
                        player = view.findViewById(R.id.player);
                        player.setListener(this);
                        openPlayer(player, bean);
                        break;
                    case 1:
                         /*
                        final String ts = String.valueOf(System.currentTimeMillis() / 1000);
                        String url = String.format(AvgleApi.PSVP, bean.getVid(), ts, Utils.b(bean.getVid(), ts));
                        Utils.goToPlay(VideosFavoriteActivity.this, bean.getTitle(), url, bean.getPreview_url());
                        */
                        startActivity(new Intent(this, WebViewActivity.class).putExtra("url", bean.getEmbedded_url()));
                        break;
                    case 2:
                        collectionChannel(bean);
                        break;
                    case 3:
                        Utils.openBrowser(VideosActivity.this, bean.getVideo_url());
                        break;
                }
                mBottomSheetDialog.dismiss();
            });
            mBottomSheetDialog.show();
        });
        mVideosAdapter.setLoadMoreView(new CustomLoadMoreView());
        mVideosAdapter.setOnLoadMoreListener(() -> mRecyclerView.postDelayed(() -> {
            mSwipe.setEnabled(false);
            if (!hasMore) {
                //数据全部加载完毕
                mVideosAdapter.loadMoreEnd();
                mSwipe.setEnabled(true);
            } else {
                if (isErr) {
                    mSwipe.setEnabled(false);
                    //成功获取更多数据
                    nowPage++;
                    isLoad = true;
                    mPresenter = createPresenter();
                    loadData();
                } else {
                    //获取更多数据失败
                    isErr = true;
                    mVideosAdapter.loadMoreFail();
                    mSwipe.setEnabled(true);
                }
            }
        }, 500), mRecyclerView);
        mRecyclerView.setAdapter(mVideosAdapter);
    }

    public void initImgs() {
        switch (type) {
            case QueryType.CHANNEL_TYPE:
                ImageLoader.getInstance().displayImage(img, imageView, getSimpleOptions());
                break;
            case QueryType.COLLECTIONS_TYPE:
                if (!img.equals(""))
                    ImageLoader.getInstance().displayImage(img, imageView, getSimpleOptions());
                else
                    imageView.setImageDrawable(getDrawable(R.drawable.default_image));
                break;
            default:
                imageView.setImageDrawable(getDrawable(R.drawable.default_image));
                break;
        }
    }

    public void initOrderDialog() {
        View orderView = LayoutInflater.from(this).inflate(R.layout.dialog_order, null);
        mOrderGroup = orderView.findViewById(R.id.order_group);
        mOrderGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId){
                case R.id.bw:
                    order = VideosOrderType.BW;
                    break;
                case R.id.mr:
                    order = VideosOrderType.MR;
                    break;
                case R.id.mv:
                    order = VideosOrderType.MV;
                    break;
                case R.id.tr:
                    order = VideosOrderType.TR;
                    break;
                case R.id.tf:
                    order = VideosOrderType.TF;
                    break;
                case R.id.lg:
                    order = VideosOrderType.LG;
                    break;
            }
            mOrderDialog.dismiss();
            list.clear();
            mVideosAdapter.notifyDataSetChanged();
            isLoad = false;
            nowPage = 0;
            mPresenter = createPresenter();
            loadData();
        });
        mOrderDialog = new BottomSheetDialog(this);
        mOrderDialog.setContentView(orderView);
    }

    private DisplayImageOptions getSimpleOptions() {
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .imageScaleType(ImageScaleType.IN_SAMPLE_INT)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
        return options;
    }

    @Override
    public void showLoadSuccessView(VideoBean bean, boolean isLoad) {
        isLoading = false;
        runOnUiThread(() -> {
            if (!mActivityFinish) {
                mSwipe.setRefreshing(false);
                hasMore = bean.getResponse().isHas_more();
                setLoadState(true);
                List<VideoBean.ResponseBean.VideosBean> data = new ArrayList<>();
                for (int i = 0; i < bean.getResponse().getVideos().size(); i++) {
                    data.add(bean.getResponse().getVideos().get(i));
                }
                if (!isLoad) {
                    list = data;
                    mVideosAdapter.setNewData(list);
                } else
                    mVideosAdapter.addData(data);
            }
        });
    }

    public void openPlayer(JZPlayer player, VideoBean.ResponseBean.VideosBean bean) {
        player.setVisibility(View.VISIBLE);
        player.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        player.titleTextView.setVisibility(View.GONE);
        player.bottomProgressBar.setVisibility(View.GONE);
        player.setUp( bean.getPreview_video_url(),  bean.getTitle(), Jzvd.SCREEN_WINDOW_NORMAL);
        Bitmap bitmap = ImageLoader.getInstance().loadImageSync(bean.getPreview_url());
        if (null != bitmap)
            Blurry.with(this)
                    .radius(4)
                    .sampling(2)
                    .async()
                    .from(ImageLoader.getInstance().loadImageSync(bean.getPreview_url()))
                    .into(player.thumbImageView);
        player.startButton.performClick();
        player.startVideo();
    }

    public void detachedFromWindow (JZPlayer player) {
        if (player != null) {
            if (player == mVideosAdapter.getViewByPosition(index, R.id.player)) {
                removePlayerView(player);
            }
        }
    }

    public void removePlayer(JZPlayer player) {
        if (player != null) {
            removePlayerView(player);
        }
    }

    public void removePlayerView(JZPlayer player) {
        player.releaseAllVideos();
        player.setVisibility(View.GONE);
        player.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.videos_menu, menu);
        MenuItem item = menu.findItem(R.id.order);
        if (type == QueryType.DEFAULT || type == QueryType.NEW_TYPE || type == QueryType.HOT_TYPE || type == QueryType.FEATURED_TYPE) {
            item.setVisible(false);
        }
        final MenuItem searchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setQueryHint(Utils.getString(R.string.search_text));
        mSearchView.setMaxWidth(2000);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.isEmpty()) {
                    st.setTitle(query);
                    Utils.hideKeyboard(mSearchView);
                    mSearchView.clearFocus();
                    toolbarTitle = query;
                    type = QueryType.QUERY_TYPE;
                    mVideosAdapter.setNewData(list = new ArrayList<>());
                    isLoad = false;
                    nowPage = 0;
                    mPresenter = createPresenter();
                    loadData();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.order) {
            if (isLoading)
                Toast.makeText(this, "请在数据加载完成后再操作！", Toast.LENGTH_SHORT).show();
            else
                mOrderDialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setLoadState(boolean loadState) {
        isErr = loadState;
        mVideosAdapter.loadMoreComplete();
    }

    public void showErrorView(String text) {
        mSwipe.setRefreshing(false);
        errorTitle.setText(text);
        mVideosAdapter.setEmptyView(errorView);
    }

    /**
     * 收藏视频
     *
     * @param bean
     */
    public void collectionChannel(final VideoBean.ResponseBean.VideosBean bean) {
        if (DatabaseUtil.checkVideo(bean.getVid()))
            application.showToastMsg(Utils.getString(R.string.video_is_exist));
        else {
            DatabaseUtil.addVideo(bean);
            application.showToastMsg(Utils.getString(R.string.favorite_success));
        }
    }

    @Override
    public void showUserFavoriteView(List<VideoBean.ResponseBean.VideosBean> list) {

    }

    @Override
    public void showLoadingView() {
        isLoading = true;
        runOnUiThread(() -> {
            if (!mActivityFinish && !isLoad) {
                mSwipe.setRefreshing(true);
                showEmptyVIew();
            }
        });

    }

    @Override
    public void showLoadErrorView(String msg) {
        isLoading = false;
        runOnUiThread(() -> {
            if (!mActivityFinish) {
                setLoadState(false);
                if (!isLoad)
                    showErrorView(msg);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void showEmptyVIew() {
        runOnUiThread(() -> {
            if (!mActivityFinish) mVideosAdapter.setEmptyView(emptyView);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.releaseAllVideos();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null)
            player.releaseAllVideos();
    }
}