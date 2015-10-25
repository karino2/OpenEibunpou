package com.livejournal.karino2.openeibunpou;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by _ on 2015/10/25.
 */
public class OptionsView extends LinearLayout {
    public static final int OPTION_TYPE_UNDEFINE = 0;
    public static final int OPTION_TYPE_SINGLE = 1;
    public static final int OPTION_TYPE_DOUBLE = 2;

    int optionType = OPTION_TYPE_UNDEFINE;

    public OptionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    int itemResId =  android.R.layout.simple_list_item_single_choice;

    void setupSingleOptionView(LayoutInflater inflater) {
        inflater.inflate(R.layout.option_single, this);
        ListView lv = (ListView)findViewById(R.id.listView);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        itemResId = android.R.layout.simple_list_item_single_choice;

        listener.enableEnter();
    }


    int firstChoiceId = -1;
    int secondChoiceId = -1;

    public interface EnableEnterListener {
        void enableEnter();
        void disableEnter();
    }

    EnableEnterListener listener = new EnableEnterListener() {
        @Override
        public void enableEnter() {

        }

        @Override
        public void disableEnter() {

        }
    };

    int[] doubleChoiceResult = new int[2];
    int[] singleChoiceResult = new int[1];

    public int[] getSelectedIds() {
        if(optionType == OPTION_TYPE_DOUBLE) {
            doubleChoiceResult[0] = firstChoiceId;
            doubleChoiceResult[1] = secondChoiceId;
            return doubleChoiceResult;
        } else {
            ListView lv = (ListView) findViewById(R.id.listView);
            singleChoiceResult[0] = lv.getCheckedItemPosition()+1;
            return singleChoiceResult;
        }
    }


    public void setEnableEnterListener(EnableEnterListener listener) {
        this.listener = listener;
    }

    public void setOptions(String[] newOptions) {
        options = newOptions;

        ListView lv = (ListView)findViewById(R.id.listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), itemResId, options);
        lv.setAdapter(adapter);

        if(optionType == OPTION_TYPE_SINGLE) {
            lv.setItemChecked(0, true);
        }
    }


    String[] options;

    void setupDoubleOptionView(LayoutInflater inflater) {
        inflater.inflate(R.layout.option_double, this);
        listener.disableEnter();

        ListView lv = (ListView)findViewById(R.id.listView);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        itemResId = android.R.layout.simple_list_item_1;
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(firstChoiceId == -1) {
                    firstChoiceId = position+1;
                    ((TextView)findViewById(R.id.textViewChoiceOne)).setText(options[position]);
                } else if(secondChoiceId == -1) {
                    secondChoiceId = position+1;
                    ((TextView)findViewById(R.id.textViewChoiceTwo)).setText(options[position]);
                }
                if(firstChoiceId != -1 && secondChoiceId != -1)
                    listener.enableEnter();
            }
        });
        findViewById(R.id.buttonDeleteOne).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFirstChoice();
            }
        });
        findViewById(R.id.buttonDeleteTwo).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSecondChoice();
            }
        });

    }

    private void clearSecondChoice() {
        secondChoiceId = -1;
        ((TextView)findViewById(R.id.textViewChoiceTwo)).setText("");
        listener.disableEnter();
    }

    private void clearFirstChoice() {
        firstChoiceId = -1;
        ((TextView) findViewById(R.id.textViewChoiceOne)).setText("");
        listener.disableEnter();
    }

    public void setOptionType(int newType) {
        if(newType == optionType) {
            clearPrevResult();
            return;
        }

        optionType = newType;

        ViewGroup vg = (ViewGroup)findViewById(R.id.rootLayout);
        if(vg != null) {
            this.removeView(vg);
        }

        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        switch(optionType) {
            case OPTION_TYPE_SINGLE:
                setupSingleOptionView(inflater);
                return;
            case OPTION_TYPE_DOUBLE:
                setupDoubleOptionView(inflater);
                return;
        }
    }

    private void clearPrevResult() {
        if(optionType == OPTION_TYPE_DOUBLE) {
            clearFirstChoice();
            clearSecondChoice();
        }
    }


}
