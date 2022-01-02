package com.diplabs.motioncam3;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

public class Settings extends AppCompatDialogFragment {
    private EditText editTextFramesToAnalize;
    private EditText editTextMinValueOnChart;

    private  SettingDialogListener listener;

    private int framesToAnalize;
    private double minValueOnChart;

    public Settings(int framesToAnalize, double minValueOnChart){
        this.framesToAnalize = framesToAnalize;
        this.minValueOnChart = minValueOnChart;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflator = getActivity().getLayoutInflater();
        View view = inflator.inflate(R.layout.layout_settings, null);

        builder.setView(view)
                .setTitle("settings")
                .setNegativeButton("cancel", (dialog, which) -> {

                })

                .setPositiveButton("ok", (dialog, which) -> {

                     int framesToAnalize = Integer.parseInt(editTextFramesToAnalize.getText().toString());

                     double minValueOnChart= Double.parseDouble(editTextMinValueOnChart.getText().toString());





                    if (!isPowerOfTwo(framesToAnalize)){
                        Toast.makeText(view.getContext(), "Numer of frames must be a power of two", Toast.LENGTH_SHORT).show();
                    } else {


                            if (minValueOnChart<0.0 || minValueOnChart>1.0 ){
                                Toast.makeText(view.getContext(), "Minimum value to show on chart must be 0.0 - 1.0 ", Toast.LENGTH_SHORT).show();
                            }
                            else{
                                listener.applySettings(framesToAnalize,minValueOnChart);
                            }





                    }





                });



        editTextFramesToAnalize = view.findViewById(R.id.editTextFrames);

        editTextMinValueOnChart = view.findViewById(R.id.editTextMinValue);

        editTextFramesToAnalize.setText(Integer.toString(framesToAnalize));

        editTextMinValueOnChart.setText(Double.toString(minValueOnChart));


        return builder.create();
    }
    public interface SettingDialogListener{
        void applySettings(int framesToAnalize, double minValueOnChart);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (SettingDialogListener) context;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }
    public static boolean isPowerOfTwo(int x)
    {
        return (x != 0) && ((x & (x - 1)) == 0);
    }
}
