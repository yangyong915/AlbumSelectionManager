package com.luck.picture.lib.dialog;


import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.luck.picture.lib.R;

public class PictureDialog extends Dialog {
    public Context context;
    TextView textView;

    public PictureDialog(Context context) {
        super(context, R.style.picture_alert_dialog);
        this.context = context;
        setCancelable(true);
        setCanceledOnTouchOutside(false);
        Window window = getWindow();
        window.setWindowAnimations(R.style.DialogWindowStyle);

        View view = LayoutInflater.from(context).inflate(R.layout.picture_alert_dialog, null);
        textView = (TextView) view.findViewById(R.id.loading_text);
        setContentView(view);
    }

    public void setLoadText(String text) {
        textView.setText(text);
    }
}