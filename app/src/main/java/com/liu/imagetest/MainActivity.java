package com.liu.imagetest;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.FrameRecorder;
import com.googlecode.javacv.cpp.opencv_core;
import com.liu.imagetest.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{

    /**
     * 需要进行检测的权限数组
     */
    protected String[] needPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };
    private Button btn_start;
    public static final String IMAGE_TYPE = ".png";

    private MediaMuxer mediaMuxer;
    private static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ScreenRecord/temp" + File.separator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取权限
        getPermission();

        btn_start = findViewById(R.id.btn);

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageToMp4();
            }
        });
    }

    /**
     * 图片转视频
     */
    private void imageToMp4() {
        // 生成的新文件名
        final String newFileName = "test_" + System.currentTimeMillis() + ".mp4";
        // 保存的路径
        String temp = null;
        final double frameRate = 0.5;
        try {
            temp = new FileUtils().getSDCardRoot() + "ScreenRecord"
                    + File.separator + newFileName;
        } catch (FileUtils.NoSdcardException e1) {
            e1.printStackTrace();
        }
        final String savePath = temp;
        final String finalTemp = temp;
        new Thread() {
            @Override
            public void run() {
                Log.d("test", "开始将图片转成视频啦...frameRate=" + frameRate);
                try {
                    // 临时文件路径即存储源图片的路径
                    String tempFilePath = new FileUtils().getSDCardRoot()
                            + "ScreenRecord/temp" + File.separator;
                    Log.i("test", "tempFilePath=" + tempFilePath);
                    Bitmap testBitmap = BitmapFactory.decodeFile(tempFilePath
                            + "head1" + MainActivity.IMAGE_TYPE);
                    //创建一个记录者
                    FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(savePath, testBitmap.getWidth(), testBitmap.getHeight());
                    // 设置视频格式
                    recorder.setFormat("mp4");
                    // 录像帧率
                    recorder.setFrameRate(frameRate);
                    // 记录开始
                    recorder.start();

                    int index = 1;
                    while (index < 6) {
                        // 获取图片--图片格式为head1.png,head2.png...head8.png
                        opencv_core.IplImage image = cvLoadImage(tempFilePath
                                + "head" + index
                                + MainActivity.IMAGE_TYPE);
                        recorder.record(image);
                        recorder.record(image);
                        index++;
                    }
                    Log.d("test", "录制完成....");
                    // 录制结束
                    recorder.stop();
                    File file = new File(finalTemp);
                    Log.d("test", file.getName() + "....");
                    combineVideo(newFileName,"audio.mp3");
                } catch (FileUtils.NoSdcardException | FrameRecorder.Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    /**
     * 音频，视频合成
     */
    private void combineVideo(String videoName,String audioName) {
        try {
            MediaExtractor videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(SDCARD_PATH + videoName);
            MediaFormat videoFormat = null;
            int videoTrackIndex = -1;
            int videoTrackCount = videoExtractor.getTrackCount();
            for (int i = 0; i < videoTrackCount; i++) {
                videoFormat = videoExtractor.getTrackFormat(i);
                String mimeType = videoFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("video/")) {
                    videoTrackIndex = i;
                    break;
                }
            }
            MediaExtractor audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(SDCARD_PATH + audioName);
            MediaFormat audioFormat = null;
            int audioTrackIndex = -1;
            int audioTrackCount = audioExtractor.getTrackCount();
            for (int i = 0; i < audioTrackCount; i++) {
                audioFormat = audioExtractor.getTrackFormat(i);
                String mimeType = audioFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("audio/")) {
                    audioTrackIndex = i;
                    break;
                }
            }
            videoExtractor.selectTrack(videoTrackIndex);
            audioExtractor.selectTrack(audioTrackIndex);
            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
            mediaMuxer = new MediaMuxer(SDCARD_PATH + "/output", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int writeVideoTrackIndex = mediaMuxer.addTrack(videoFormat);
            int writeAudioTrackIndex = mediaMuxer.addTrack(audioFormat);
            mediaMuxer.start();
            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            long sampleTime = 0;
            {
                videoExtractor.readSampleData(byteBuffer, 0);
                if (videoExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                    videoExtractor.advance();
                }
                videoExtractor.readSampleData(byteBuffer, 0);
                long secondTime = videoExtractor.getSampleTime();
                videoExtractor.advance();
                long thirdTime = videoExtractor.getSampleTime();
                sampleTime = Math.abs(thirdTime - secondTime);
            }
            videoExtractor.unselectTrack(videoTrackIndex);
            videoExtractor.selectTrack(videoTrackIndex);
            while (true) {
                int readVideoSampleSize = videoExtractor.readSampleData(byteBuffer, 0);
                if (readVideoSampleSize < 0) {
                    break;
                }
                videoBufferInfo.size = readVideoSampleSize;
                videoBufferInfo.presentationTimeUs += sampleTime;
                videoBufferInfo.offset = 0;
                videoBufferInfo.flags = videoExtractor.getSampleFlags();
                mediaMuxer.writeSampleData(writeVideoTrackIndex, byteBuffer, videoBufferInfo);
                videoExtractor.advance();
            }
            while (true) {
                int readAudioSampleSize = audioExtractor.readSampleData(byteBuffer, 0);
                if (readAudioSampleSize < 0) {
                    break;
                }
                audioBufferInfo.size = readAudioSampleSize;
                audioBufferInfo.presentationTimeUs += sampleTime;
                audioBufferInfo.offset = 0;
                audioBufferInfo.flags = videoExtractor.getSampleFlags();
                mediaMuxer.writeSampleData(writeAudioTrackIndex, byteBuffer, audioBufferInfo);
                audioExtractor.advance();
            }
            mediaMuxer.stop();
            mediaMuxer.release();
            videoExtractor.release();
            audioExtractor.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查权限
     *
     * @param
     * @since 2.5.0
     */
    private void getPermission() {
        if (EasyPermissions.hasPermissions(this, needPermissions)) {
            //已经打开权限
//            Toast.makeText(this, "已经申请相关权限", Toast.LENGTH_SHORT).show();
        } else {
            //没有打开相关权限、申请权限
            EasyPermissions.requestPermissions(this, "需要获取您的存储、定位、相机权限", 1, needPermissions);
        }

    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
//        Toast.makeText(this, "相关权限获取成功", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Toast.makeText(this, "请同意相关权限，否则功能无法使用", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}
