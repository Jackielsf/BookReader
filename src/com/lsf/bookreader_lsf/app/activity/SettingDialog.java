package com.lsf.bookreader_lsf.app.activity;

import java.util.regex.Pattern;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import com.lsf.bookreader_lsf.app.R;
import com.lsf.bookreader_lsf.app.utils.PatternUtils;

public class SettingDialog extends Dialog {
	
	private RadioButton daytime;
	private RadioButton night;
	private final RadioButton[] radioButtons = new RadioButton[6];
	private Button ok;

	public SettingDialog(Context context,int fontSize, int textColor) {
		super(context);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.setting_dialog, null);
		daytime = (RadioButton) view.findViewById(R.id.daytime);
		night = (RadioButton) view.findViewById(R.id.night);
		if(textColor == Color.BLACK){
			daytime.setChecked(true);
		} else {
			night.setChecked(true);
		}
		
		radioButtons[0] = (RadioButton) view.findViewById(R.id.level1);
		radioButtons[1] = (RadioButton) view.findViewById(R.id.level2);
		radioButtons[2] = (RadioButton) view.findViewById(R.id.level3);
		radioButtons[3] = (RadioButton) view.findViewById(R.id.level4);
		radioButtons[4] = (RadioButton) view.findViewById(R.id.level5);
		radioButtons[5] = (RadioButton) view.findViewById(R.id.level6);
		for (int i = 0; i < radioButtons.length; i++) {
			final int k = i;
			radioButtons[i]
					.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							if (isChecked) {
								for (int index = 0; index < radioButtons.length; index++) {
									if (index != k) {
										radioButtons[index].setChecked(false);
									}
								}
							}
						}
					});
		}
		int index = (fontSize-36)/6;
		radioButtons[index].setChecked(true);
		ok = (Button) view.findViewById(R.id.ok);
		super.setContentView(view);
	}

	public byte getReadPattern() {
		return (daytime.isChecked() ? PatternUtils.DAYTIMEPATTERN
				: PatternUtils.NIGHTPATTERN);
	}

	public int getFontSize() {
		for (int i = 0; i < 6; i++) {
			if (radioButtons[i].isChecked()) {
				return (6 * i) + 36;
			}
		}
		return 36;
	}

	public void setOnStartNewReadViewListener(
			View.OnClickListener onClickListener) {
		ok.setOnClickListener(onClickListener);
	}
}
