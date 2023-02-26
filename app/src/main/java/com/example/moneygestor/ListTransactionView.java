package com.example.moneygestor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ListTransactionView extends LinearLayout {

    public ListTransactionView(Context context, int i) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        LayoutParams generalLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        generalLayoutParams.setMargins(5, 10, 10, 5);
        setLayoutParams(generalLayoutParams);

        LayoutParams nameParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.setMargins(0, 0, 10, 0);

        LinearLayout descriptionLayout = new LinearLayout(this.getContext());
        descriptionLayout.setOrientation(HORIZONTAL);

        TextView textDescription = new TextView(descriptionLayout.getContext());
        textDescription.setText(String.format("%s:", context.getText(R.string.transaction_description)));
        textDescription.setLayoutParams(nameParams);
        descriptionLayout.addView(textDescription);

        TextView valueDescription = new TextView(context);
        valueDescription.setText("????");
        valueDescription.setTextSize(20);
        valueDescription.setTextColor(Color.BLACK);
        valueDescription.setTypeface(Typeface.DEFAULT_BOLD);
        descriptionLayout.addView(valueDescription);

        addView(descriptionLayout);

        LinearLayout valueLayout = new LinearLayout(this.getContext());
        valueLayout.setOrientation(HORIZONTAL);

        TextView textValue = new TextView(valueLayout.getContext());
        textValue.setText(String.format("%s:", context.getText(R.string.transaction_value)));
        textValue.setLayoutParams(nameParams);
        valueLayout.addView(textValue);

        TextView valueValue = new TextView(context);
        valueValue.setText("????");
        valueValue.setTextSize(20);
        valueValue.setTextColor(Color.BLACK);
        valueValue.setTypeface(Typeface.DEFAULT_BOLD);
        valueLayout.addView(valueValue);

        addView(valueLayout);

        setOnClickListener(view -> System.out.println(i));
    }
}
