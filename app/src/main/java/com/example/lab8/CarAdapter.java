package com.example.lab8;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.lab8.R;
import com.example.lab8.Car;

import java.util.List;

public class CarAdapter extends ArrayAdapter<Car> {
    private Context context;
    private List<Car> cars;

    public CarAdapter(Context context, List<Car> cars) {
        super(context, R.layout.car_item, cars);
        this.context = context;
        this.cars = cars;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.car_item, parent, false);
        }

        Car car = cars.get(position);

        TextView tvCarInfo = convertView.findViewById(R.id.tv_car_info);
        tvCarInfo.setText(car.toString());

        return convertView;
    }

    public void updateData(List<Car> newCars) {
        cars.clear();
        cars.addAll(newCars);
        notifyDataSetChanged();
    }
}