package com.luck.picture.lib;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.luck.picture.lib.anim.OptAnimationLoader;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.EventEntity;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.observable.ImagesObservable;
import com.luck.picture.lib.rxbus2.RxBus;
import com.luck.picture.lib.rxbus2.Subscribe;
import com.luck.picture.lib.rxbus2.ThreadMode;
import com.luck.picture.lib.tools.AttrsUtils;
import com.luck.picture.lib.tools.Constant;
import com.luck.picture.lib.tools.DebugUtil;
import com.luck.picture.lib.tools.LightStatusBarUtils;
import com.luck.picture.lib.tools.ScreenUtils;
import com.luck.picture.lib.tools.ToolbarUtil;
import com.luck.picture.lib.tools.VoiceUtils;
import com.luck.picture.lib.widget.PreviewViewPager;
import com.mabeijianxi.smallvideorecord2.LocalMediaCompress;
import com.mabeijianxi.smallvideorecord2.model.AutoVBRMode;
import com.mabeijianxi.smallvideorecord2.model.LocalMediaConfig;
import com.mabeijianxi.smallvideorecord2.model.OnlyCompressOverBean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

import static android.R.attr.path;
import static com.bumptech.glide.util.Preconditions.checkNotNull;
import static com.luck.picture.lib.tools.Constant.VIDEO_LENGTH_BYTE;


/**
 * author：luck
 * project：PictureSelector
 * package：com.luck.picture.ui
 * email：893855882@qq.com
 * data：16/12/31
 */
public class PicturePreviewActivity extends PictureBaseActivity implements View.OnClickListener, Animation.AnimationListener {

    private static final String TAG = PicturePreviewActivity.class.getSimpleName();

    private TextView video_edit, video_edit_ok, video_edit_big10;
    private PreviewViewPager viewPager;
    private RelativeLayout video_layout, video_layout10;
    private int position;
    private List<LocalMedia> images = new ArrayList<>();
    private List<LocalMedia> selectImages = new ArrayList<>();
    private SimpleFragmentAdapter adapter;
    private Animation animation;
    private boolean refresh;
    private int index;
    private int preview_complete_textColor;
    private int screenWidth;
    private int type;
    private LayoutInflater inflater;

    //add by tan 新版本
    private RelativeLayout rlSelectBarLayout;
    private TextView tvCircleCheckBox;
    private ImageView imgLeftBack;
    private TextView tvPreviewTitle;
    private LinearLayout llPictureContainer;
    private TextView tvPreviewConfirm;
    private Drawable topBarConfirmDisableDrawable;
    private Drawable topBarConfirmEnableDrawable;

    private CompositeDisposable compositeDisposable;
    //end

    //EventBus 3.0 回调
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void eventBus(EventEntity obj) {
        switch (obj.what) {
            case PictureConfig.CLOSE_PREVIEW_FLAG:
                // 压缩完后关闭预览界面
                dismissDialog();
                finish();
                overridePendingTransition(0, R.anim.a3);
                break;
            case PictureConfig.CROP_VIDEO:
                onResult(obj.medias);
                break;
        }
    }

    private void initNewTopBar() {

        imgLeftBack = (ImageView) findViewById(R.id.picture_left_back);
        tvPreviewTitle = (TextView) findViewById(R.id.picture_title);
        tvPreviewTitle.setVisibility(View.GONE);
        llPictureContainer = (LinearLayout) findViewById(R.id.ll_picture_container);
        tvPreviewConfirm = (TextView) findViewById(R.id.txt_picture_ok);

        imgLeftBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(0, R.anim.a3);
            }
        });

        llPictureContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choose_result();
            }
        });
    }

    private void initDrawable() {

        checkNotNull(getResources(), "getResource cannot null!");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            topBarConfirmDisableDrawable = getDrawable(R.mipmap.ic_confirm_disable);
            topBarConfirmEnableDrawable = getDrawable(R.mipmap.ic_confirm_enable);

        } else {
            topBarConfirmDisableDrawable = getResources().getDrawable(R.mipmap.ic_confirm_disable);
            topBarConfirmEnableDrawable = getResources().getDrawable(R.mipmap.ic_confirm_enable);
        }

    }

    private void initViewPager() {
        viewPager = (PreviewViewPager) findViewById(R.id.preview_pager);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                isPreviewEggs(previewEggs, position, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int i) {
                position = i;
                tvPreviewTitle.setText(position + 1 + "/" + images.size());
                LocalMedia media = images.get(position);
                index = media.getPosition();
                if (!previewEggs) {
                    if (checkNumMode) {
                        tvCircleCheckBox.setText(media.getNum() + "");
                        notifyCheckChanged(media);
                    }
                    onImageChecked(position);
                }
                setButtonState(media);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private void initCircleCheckBoxView() {
        //add by tan
        tvCircleCheckBox = (TextView) findViewById(R.id.tv_circle_check);
        tvCircleCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (images != null && images.size() > 0) {
                    LocalMedia image = images.get(viewPager.getCurrentItem());
                    String pictureType = selectImages.size() > 0 ? selectImages.get(0).getPictureType() : "";
                    if (!TextUtils.isEmpty(pictureType)) {
                        boolean toEqual = PictureMimeType.
                                mimeToEqual(pictureType, image.getPictureType());
                        if (!toEqual) {
                            showToast(getString(R.string.picture_rule));
                            return;
                        }
                    }

                    //新版checkbox 选择逻辑
                    // 刷新图片列表中图片状态
                    boolean isCircleBoxChecked;
                    if (!tvCircleCheckBox.isSelected()) {
                        isCircleBoxChecked = true;
                        tvCircleCheckBox.setSelected(true);
                        //tvCircleCheckBox.startAnimation(animation);
                    } else {
                        isCircleBoxChecked = false;
                        tvCircleCheckBox.setSelected(false);
                    }

                    if (selectImages.size() >= maxSelectNum && isCircleBoxChecked) {
                        showToast(getString(R.string.picture_message_max_num, maxSelectNum));
                        tvCircleCheckBox.setSelected(false);
                        return;
                    }

                    if (isCircleBoxChecked) {
                        VoiceUtils.playVoice(mContext, openClickSound);
                        selectImages.add(image);
                        image.setNum(selectImages.size());
                        if (checkNumMode) {
                            tvCircleCheckBox.setText(image.getNum() + "");
                        }
                    } else {
                        for (LocalMedia media : selectImages) {
                            if (media.getPath().equals(image.getPath())) {
                                selectImages.remove(media);
                                subSelectPosition();
                                notifyCheckChanged(media);
                                break;
                            }
                        }
                    }

                    onSelectNumChange(true);
                }
            }
        });
        //end
    }

    private void initVideoView() {
        rlSelectBarLayout = (RelativeLayout) findViewById(R.id.select_bar_layout);
        video_layout = (RelativeLayout) findViewById(R.id.video_layout_1);
        video_layout10 = (RelativeLayout) findViewById(R.id.video_layout_10);

        video_edit = (TextView) findViewById(R.id.video_edit);
        video_edit.setOnClickListener(this);
        video_edit_ok = (TextView) findViewById(R.id.video_edit_ok);
        video_edit_ok.setOnClickListener(this);
        video_edit_big10 = (TextView) findViewById(R.id.video_edit_10);
        video_edit_big10.setOnClickListener(this);
    }

    private void getIntentData() {
        position = getIntent().getIntExtra(PictureConfig.EXTRA_POSITION, 0);
        type = getIntent().getIntExtra(PictureConfig.EXTRA_MEDIA, PictureConfig.TYPE_ALL);

        selectImages = (List<LocalMedia>) getIntent().
                getSerializableExtra(PictureConfig.EXTRA_SELECT_LIST);

        boolean is_bottom_preview = getIntent().getBooleanExtra(PictureConfig.EXTRA_BOTTOM_PREVIEW, false);

        DebugUtil.e(TAG, "TANHQ===> is_bottom_preview: " + is_bottom_preview);

        if (is_bottom_preview) {
            // 底部预览按钮过来
            images = (List<LocalMedia>) getIntent().
                    getSerializableExtra(PictureConfig.EXTRA_PREVIEW_SELECT_LIST);

            switch (type) {
                case PictureConfig.TYPE_ALL:
                    break;

                case PictureConfig.TYPE_IMAGE:
                    showImageUI();
                    break;

                case PictureConfig.TYPE_VIDEO:
                    hideImageUI();
                    break;
            }

        } else {
            List<LocalMedia> all = ImagesObservable.getInstance().readLocalMedias();
            List<LocalMedia> all_image = new ArrayList<>();
            List<LocalMedia> all_video = new ArrayList<>();

            for (int i = 0; i < all.size(); i++) {   //进行分类
                int type0 = PictureMimeType.isPictureType(all.get(i).getPictureType());
                int pos = getIntent().getIntExtra(PictureConfig.EXTRA_POSITION, 0);
                switch (type0) {
                    case PictureConfig.TYPE_IMAGE:
                        all_image.add(all.get(i));
                        if (type == PictureConfig.TYPE_VIDEO && pos > i) {
                            --position;
                        }
                        break;
                    case PictureConfig.TYPE_VIDEO:
                        all_video.add(all.get(i));
                        if (type == PictureConfig.TYPE_IMAGE && pos > i) {
                            --position;
                        }
                        break;
                }
            }

            switch (type) {
                case PictureConfig.TYPE_ALL:
                    images = all;
                    break;

                case PictureConfig.TYPE_IMAGE:
                    images = all_image;
                    showImageUI();
                    break;

                case PictureConfig.TYPE_VIDEO:
                    images = all_video;
                    hideImageUI();
                    break;
            }
        }
    }

    private void showImageUI() {
        tvCircleCheckBox.setVisibility(View.VISIBLE);
        llPictureContainer.setVisibility(View.VISIBLE);

        //图片预览 不显示 "视频裁剪布局"
        rlSelectBarLayout.setVisibility(View.GONE);
        video_layout.setVisibility(View.GONE);
        video_layout10.setVisibility(View.GONE);
    }

    private void hideImageUI() {
        //视频预览 不显示 "确定", "圆圈CheckBox"
        tvCircleCheckBox.setVisibility(View.GONE);
        llPictureContainer.setVisibility(View.GONE);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_preview);

        if (!RxBus.getDefault().isRegistered(this)) {
            RxBus.getDefault().register(this);
        }

        inflater = LayoutInflater.from(this);
        screenWidth = ScreenUtils.getScreenWidth(this);
        int status_color = AttrsUtils.getTypeValueColor(this, R.attr.picture_status_color);
        ToolbarUtil.setColorNoTranslucent(this, status_color);
        preview_complete_textColor = AttrsUtils.getTypeValueColor(this, R.attr.picture_preview_textColor);
        LightStatusBarUtils.setLightStatusBar(this, previewStatusFont);

        animation = OptAnimationLoader.loadAnimation(this, R.anim.modal_in);
        animation.setAnimationListener(this);

        compositeDisposable = new CompositeDisposable();//add by tanhaiqin

        initDrawable();

        initNewTopBar();

        initCircleCheckBoxView();

        initVideoView();

        initViewPager();

        getIntentData();

        initViewPageAdapterData();

        getPermissions();
    }

    /**
     * 获取权限6.0
     */
    private void getPermissions() {
        /**
         * 动态获取权限，Android 6.0 新特性，一些保护权限，除了要在AndroidManifest中声明权限，还要使用如下代码动态获取
         */
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return;
                }
            }
        }
    }

    /**
     * 这里没实际意义，好处是预览图片时 滑动到屏幕一半以上可看到下一张图片是否选中了
     *
     * @param previewEggs          是否显示预览友好体验
     * @param positionOffsetPixels 滑动偏移量
     */
    private void isPreviewEggs(boolean previewEggs, int position, int positionOffsetPixels) {
        if (previewEggs) {
            if (images.size() > 0 && images != null) {
                LocalMedia media;
                int num;
                if (positionOffsetPixels < screenWidth / 2) {
                    media = images.get(position);
                    tvCircleCheckBox.setSelected(isSelected(media));//add by tan
                    if (checkNumMode) {
                        num = media.getNum();
                        tvCircleCheckBox.setText(num + ""); //add by tan
                        notifyCheckChanged(media);
                        onImageChecked(position);
                    }
                } else {
                    media = images.get(position + 1);
                    tvCircleCheckBox.setSelected(isSelected(media));
                    if (checkNumMode) {
                        num = media.getNum();
                        tvCircleCheckBox.setText(num + "");
                        notifyCheckChanged(media);
                        onImageChecked(position + 1);
                    }
                }
            }
        }
    }

    private void initViewPageAdapterData() {
        tvPreviewTitle.setText(position + 1 + "/" + images.size());
        adapter = new SimpleFragmentAdapter();
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position);
        onSelectNumChange(false);
        onImageChecked(position);

        if (images.size() > 0) {
            LocalMedia media = images.get(position);
            setButtonState(media);
            index = media.getPosition();
            if (checkNumMode) {
                tvCircleCheckBox.setText(media.getNum() + "");//add by tan
                notifyCheckChanged(media);
            }
        }
    }

    /**
     * 设置底边状态
     *
     * @param media
     */
    private void setButtonState(LocalMedia media) {
        if (type == PictureConfig.TYPE_VIDEO) {

            rlSelectBarLayout.setVisibility(View.VISIBLE);

            if (media.getDuration() > 10 * 1000) {
                video_layout.setVisibility(View.GONE);
                video_layout10.setVisibility(View.VISIBLE);

            } else {
                video_layout.setVisibility(View.VISIBLE);
                video_layout10.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 选择按钮更新
     */
    private void notifyCheckChanged(LocalMedia imageBean) {
        if (checkNumMode) {
            tvCircleCheckBox.setText("");//add by tan
            for (LocalMedia media : selectImages) {
                if (media.getPath().equals(imageBean.getPath())) {
                    imageBean.setNum(media.getNum());
                    tvCircleCheckBox.setText(String.valueOf(imageBean.getNum())); //add by tan
                }
            }
        }
    }

    /**
     * 更新选择的顺序
     */
    private void subSelectPosition() {
        for (int index = 0, len = selectImages.size(); index < len; index++) {
            LocalMedia media = selectImages.get(index);
            media.setNum(index + 1);
        }
    }

    /**
     * 判断当前图片是否选中
     *
     * @param position
     */
    public void onImageChecked(int position) {
        if (images != null && images.size() > 0) {
            LocalMedia media = images.get(position);
            tvCircleCheckBox.setSelected(isSelected(media));
        } else {
            tvCircleCheckBox.setSelected(false);
        }
    }

    /**
     * 当前图片是否选中
     *
     * @param image
     * @return
     */
    public boolean isSelected(LocalMedia image) {
        for (LocalMedia media : selectImages) {
            if (media.getPath().equals(image.getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 更新图片选择数量
     */

    public void onSelectNumChange(boolean isRefresh) {
        this.refresh = isRefresh;
        boolean enable = selectImages.size() != 0;
        if (enable) {

            tvPreviewConfirm.setTextColor(preview_complete_textColor);
            llPictureContainer.setEnabled(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                llPictureContainer.setBackground(topBarConfirmEnableDrawable);
            } else {
                llPictureContainer.setBackgroundDrawable(topBarConfirmEnableDrawable);
            }

            tvPreviewConfirm.setText(getString(R.string.action_ok_format, selectImages.size()));

        } else {

            tvPreviewConfirm.setTextColor(ContextCompat.getColor(this, R.color.tab_color_false));
            llPictureContainer.setEnabled(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                llPictureContainer.setBackground(topBarConfirmDisableDrawable);
            } else {
                llPictureContainer.setBackgroundDrawable(topBarConfirmDisableDrawable);
            }

            tvPreviewConfirm.setText(getString(R.string.action_ok));
        }
        updateSelector(refresh);
    }

    /**
     * 更新图片列表选中效果
     *
     * @param isRefresh
     */
    private void updateSelector(boolean isRefresh) {
        if (isRefresh) {
            EventEntity obj = new EventEntity(PictureConfig.UPDATE_FLAG, selectImages, index);
            RxBus.getDefault().post(obj);
        }
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        updateSelector(refresh);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }


    public class SimpleFragmentAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return images.size();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            (container).removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View contentView = inflater.inflate(R.layout.picture_image_preview, container, false);
            final PhotoView imageView = (PhotoView) contentView.findViewById(R.id.preview_image);
            ImageView iv_play = (ImageView) contentView.findViewById(R.id.iv_play);
            LocalMedia media = images.get(position);
            if (media != null) {
                final String pictureType = media.getPictureType();
                boolean eqVideo = pictureType.startsWith(PictureConfig.VIDEO);
                iv_play.setVisibility(eqVideo ? View.VISIBLE : View.GONE);
                final String path;
                if (media.isCompressed()) {
                    // 压缩过,或者裁剪同时压缩过,以最终压缩过图片为准
                    path = media.getCompressPath();
                } else {
                    path = media.getPath();
                }
                boolean isGif = PictureMimeType.isGif(pictureType);
                // 压缩过的gif就不是gif了
                if (isGif && !media.isCompressed()) {
                    RequestOptions gifOptions = new RequestOptions()
                            .override(480, 800)
                            .priority(Priority.HIGH)
                            .diskCacheStrategy(DiskCacheStrategy.NONE);
                    Glide.with(PicturePreviewActivity.this)
                            .asGif()
                            .load(path)
                            .apply(gifOptions)
                            .into(imageView);
                } else {
                    RequestOptions options = new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .override(480, 800);
                    Glide.with(PicturePreviewActivity.this)
                            .asBitmap()
                            .load(path)
                            .apply(options)
                            .into(imageView);
                }
                imageView.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
                    @Override
                    public void onViewTap(View view, float x, float y) {
                        finish();
                        overridePendingTransition(0, R.anim.a3);
                    }
                });
                iv_play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle bundle = new Bundle();
                        bundle.putString("video_path", path);
                        startActivity(PictureVideoPlayActivity.class, bundle);
                    }
                });
            }
            (container).addView(contentView, 0);
            return contentView;
        }
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.video_edit) {
            intent_crop();

        } else if (id == R.id.video_edit_10) {
            intent_crop();

        } else if (id == R.id.video_edit_ok) {
            LocalMedia image = images.get(viewPager.getCurrentItem());
            selectImages.add(image);

            showPleaseDialog("处理中...");
            choose_result();

        } else {
            //default
        }
    }

    private void choose_result() {
        final List<LocalMedia> images = selectImages;
        String pictureType = images.size() > 0 ? images.get(0).getPictureType() : "";
        // 如果设置了图片最小选择数量，则判断是否满足条件
        int size = images.size();
        boolean eqImg = pictureType.startsWith(PictureConfig.IMAGE);
        if (minSelectNum > 0 && selectionMode == PictureConfig.MULTIPLE) {
            if (size < minSelectNum) {
                String str = eqImg ? getString(R.string.picture_min_img_num, minSelectNum)
                        : getString(R.string.picture_min_video_num, minSelectNum);
                showToast(str);
                return;
            }
        }
        if (type == PictureConfig.TYPE_VIDEO) {
            handleCompressVideo(images);
        } else {
            onResult(images);
        }
    }

    /**
     * 处理在PicturePreviewActivity页面 完成 按钮，表明当前视频时长 < 10s
     * 需要判断 视频大小， 如果 <= 3M (3 * 1024 * 1024 = 3145728 字节)不用压缩， 否则需要压缩本地视频
     *
     * @param images
     */
    private void handleCompressVideo(final List<LocalMedia> images) {
        if (images == null || images.isEmpty()) {
            Toast.makeText(this, "视频路径错误", Toast.LENGTH_SHORT).show();
            return;
        }

        final String videoPath = images.get(0).getPath();

        if (TextUtils.isEmpty(videoPath)) {
            Toast.makeText(this, "视频路径错误", Toast.LENGTH_SHORT).show();
            return;
        }

        DebugUtil.i(TAG, "TANHQ===> image size: " + images.size() + "， path: " + videoPath);
//        showPleaseDialog("处理中...");

        Observable.just(videoPath)
                .map(new Function<String, List<LocalMedia>>() {
                    @Override
                    public List<LocalMedia> apply(String path) throws Exception {
                        return getVideoLocalMedias(path, images);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<LocalMedia>>() {
                    @Override
                    public void onSubscribe(Disposable disposable) {
                        if (compositeDisposable != null) {
                            compositeDisposable.add(disposable);
                        }
                        DebugUtil.i(TAG, "TANHQ===> 处理中...");
                    }

                    @Override
                    public void onNext(List<LocalMedia> localMediaList) {
                        RxBus.getDefault().post(new EventEntity(PictureConfig.PREVIEW_DATA_FLAG, localMediaList));
                        dismissDialog();
                        finish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        DebugUtil.i(TAG, "TANHQ===> onError...");
                        dismissDialog();
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    /**
     * @param path
     * @param images
     * @return
     */
    @NonNull
    private List<LocalMedia> getVideoLocalMedias(String path, List<LocalMedia> images) {
        File file = new File(path);

        List<LocalMedia> localMediaList = new ArrayList<>();
        localMediaList.addAll(images);

        if (file.exists() && file.isFile()) {
            DebugUtil.debug("TANHQ===> file length = " + file.length());

            if (file.length() > VIDEO_LENGTH_BYTE) {//3M, 需要压缩
                //压缩本地视频
                LocalMediaConfig.Buidler buidler = new LocalMediaConfig.Buidler();
                LocalMediaConfig config = buidler
                        .setVideoPath(path)
                        .captureThumbnailsTime(1)
                        .doH264Compress(new AutoVBRMode(Constant.AutoVBRMode))
                        .setFramerate(Constant.FRAMERATE_15)
                        .setScale(Constant.SCALE_15)
                        .build();

                OnlyCompressOverBean onlyCompressOverBean = new LocalMediaCompress(config).startCompress();
                localMediaList.get(0).setPath(onlyCompressOverBean.getVideoPath());
                //localMediaList.get(0).setCompressPath(onlyCompressOverBean.getPicPath());

            } else {// <= 3M 不压缩
                localMediaList.get(0).setPath(path);
            }
        }

        return localMediaList;
    }

    private void intent_crop() {
        Intent intent = new Intent(this, PictureEditAudioActivity.class);
        LocalMedia media = images.get(viewPager.getCurrentItem());
        Bundle bundle = new Bundle();
        bundle.putSerializable(PictureConfig.EXTRA_MEDIA, media);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void onResult(final List<LocalMedia> images) {
        RxBus.getDefault().post(new EventEntity(PictureConfig.PREVIEW_DATA_FLAG, images));
        finish();
        overridePendingTransition(0, R.anim.a3);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(0, R.anim.a3);
    }

    @Override
    protected void onDestroy() {
        if (RxBus.getDefault().isRegistered(this)) {
            RxBus.getDefault().unregister(this);
        }
        if (animation != null) {
            animation.cancel();
            animation = null;
        }

        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }

        super.onDestroy();
    }
}
