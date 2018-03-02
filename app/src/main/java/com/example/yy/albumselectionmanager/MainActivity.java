package com.example.yy.albumselectionmanager;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.compress.Luban;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/**
 * crate by yy on 2018-3-1
 * describle:图片、视频选择库，功能如下：
 * 1、相册选择图片、视频，支持多选
 * 2、自定义拍照、拍摄10s短视频
 * 3、图片压缩、短视频裁剪、压缩，采用鲁班压缩算法
 * 4、预览功能,已在库中处理动态权限问题
 */
public class MainActivity extends AppCompatActivity {

    Button picture, video;
    LinearLayout result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        picture = findViewById(R.id.picture);
        video = findViewById(R.id.video);
        result = findViewById(R.id.result);

        picture.setOnClickListener(view -> initPictureSelector(PictureMimeType.ofImage()));
        video.setOnClickListener(view -> initPictureSelector(PictureMimeType.ofVideo()));
    }

    /**
     * 打开相册初始化,回传数据在onActivityResult方法中
     *
     * @param chooseMode 打开的类型
     */
    public void initPictureSelector(int chooseMode) {
        PictureSelector.create(this)
                .openGallery(chooseMode)// 全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()
                .theme(R.style.picture_default_style)// 主题样式设置 具体参考 libray中values/styles
                .maxSelectNum(9)// 最大图片选择数量
                .minSelectNum(1)// 最小选择数量
                .imageSpanCount(4)// 每行显示个数
                .selectionMode(PictureConfig.MULTIPLE)// 多选 or 单选 PictureConfig.MULTIPLE : PictureConfig.SINGLE
                .previewImage(true)// 是否可预览图片
                .previewVideo(true)// 是否可预览视频
                .enablePreviewAudio(false)// 是否预览音频
//                .compressGrade(Luban.THIRD_GEAR)// luban压缩档次，默认3档 Luban.FIRST_GEAR、Luban.CUSTOM_GEAR
                .isCamera(true)// 是否显示拍照按钮
                .isZoomAnim(true)// 图片列表点击 缩放效果 默认true
                .setOutputCameraPath(Constant.IMAGE_CACHE)// 自定义拍照保存路径
                .compress(true)// 是否压缩
                .compressMode(PictureConfig.LUBAN_COMPRESS_MODE)//系统自带 or 鲁班压缩 PictureConfig.SYSTEM_COMPRESS_MODE or LUBAN_COMPRESS_MODE
//                //.sizeMultiplier(0.5f)// glide 加载图片大小 0~1之间 如设置 .glideOverride()无效
                .glideOverride(160, 160)// glide 加载宽高，越小图片列表越流畅，但会影响列表图片浏览的清晰度
                .isGif(false)// 是否显示gif图片
                .openClickSound(false)// 是否开启点击声音
//                .selectionMedia(selectList)// 是否传入已选图片
//                //.previewEggs(false)// 预览图片时 是否增强左右滑动图片体验(图片滑动一半即可看到上一张是否选中)
//                .compressGrade(Luban.CUSTOM_GEAR)
                .compressGrade(Luban.CUSTOM_GEAR)
                .compressMaxKB(1024)//压缩最大值kb compressGrade()为Luban.CUSTOM_GEAR有效
                .minimumCompressSize(500) //add by tanhaiqin, 图片大小 <= 500KB(数字可变) 不需要压缩
//                //.compressWH() // 压缩宽高比 compressGrade()为Luban.CUSTOM_GEAR有效
//                //.videoQuality()// 视频录制质量 0 or 1
                .videoSecond(5 * 60)//显示多少秒以内的视频
//                //.recordVideoSecond()//录制视频秒数 默认60秒
                .forResult(PictureConfig.CHOOSE_REQUEST);
    }

    /**
     * 处理 PictureSelectorActivity.java 返回的数据
     * 注意 图片压缩 已经是在picture lib中处理， 界面仅仅是展示获取的LocalMedia数据，不做再次压缩！
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PictureConfig.CHOOSE_REQUEST:
                    // 图片选择,共用一个数据通道:返回时图片，可能为列表，视频只能有一个
                    List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
                    LogUtils.i("TEST===> selectList.size = " + selectList.size());
                    for (int i = 0; i < selectList.size(); i++) {
                        handleLocalMedia(selectList.get(i));
                    }
                    break;
            }
        }
    }

    private void handleLocalMedia(LocalMedia media) {
        int pictureType = PictureMimeType.isPictureType(media.getPictureType());
        switch (pictureType) {
            case PictureConfig.TYPE_IMAGE:
                LogUtils.e("TEST===> media path = " + media.getPath()
                        + ",  compressPath = " + media.getCompressPath()
                        + ", height = " + media.getHeight()
                        + ", width = " + media.getWidth());
                TextView textView = new TextView(this);
                textView.setText(" media path =" + media.getPath());
                result.addView(textView);
                break;
            case PictureConfig.TYPE_VIDEO:
                if (TextUtils.isEmpty(media.getPath())) return;
                if (!StringUtils.fileIsExists(media.getPath())) {
                    LogUtils.e("文件可能不存在了~");
                    return;
                }
                LogUtils.e("TEST===> video path = " + media.getPath()
                        + ",  compressPath = " + media.getCompressPath()
                        + ", height = " + media.getHeight()
                        + ", width = " + media.getWidth());
                TextView textView1 = new TextView(this);
                textView1.setText(" video path =" + media.getPath());
                result.addView(textView1);
                break;
        }
    }
}
