package com.liu.imagetest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * @创建者 ly
 * @创建时间 2019/7/22
 * @描述 ${TODO}
 * @更新者 $Author$
 * @更新时间 $Date$
 * @更新描述 ${TODO}
 */
public class ViewToImageActivity extends Activity implements View.OnClickListener {

    private Button button;
    private RelativeLayout rl;
    private ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legend);
//        button = findViewById(R.id.btn);
//        button.setOnClickListener(this);
//        rl = findViewById(R.id.rl);
//        iv = findViewById(R.id.iv);

    }


    /**
     * 主要方法：创建一个bitmap放于画布之上进行绘制
     */
    private static Bitmap convertViewToBitmap(View tempView, Display disPlay) {
        Bitmap bitmap = Bitmap.createBitmap(tempView.getWidth(),
                tempView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        tempView.draw(canvas);
        return bitmap;
    }

    @Override
    public void onClick(View v) {
        iv.setImageBitmap(convertViewToBitmap(rl,null));
    }
}
