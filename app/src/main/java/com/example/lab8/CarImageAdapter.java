package com.example.lab8;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CarImageAdapter extends ArrayAdapter<CarImage> {
    private Context context;
    private List<CarImage> carImages;
    private ExecutorService executorService;

    public CarImageAdapter(Context context, List<CarImage> carImages) {
        super(context, R.layout.car_image_item, carImages);
        this.context = context;
        this.carImages = carImages;
        this.executorService = Executors.newFixedThreadPool(5);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.car_image_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.imageView = convertView.findViewById(R.id.car_image_view);
            viewHolder.descriptionTextView = convertView.findViewById(R.id.car_description_text);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        CarImage carImage = carImages.get(position);
        viewHolder.descriptionTextView.setText(carImage.getDescription());

        // Load image using Executors
        loadImage(carImage.getImageUrl(), viewHolder.imageView);

        return convertView;
    }

    private void loadImage(String imageUrl, final ImageView imageView) {
        executorService.execute(() -> {
            try {
                URL url = new URL(imageUrl);
                final Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());

                // Update UI on main thread
                imageView.post(() -> {
                    imageView.setImageBitmap(bitmap);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView descriptionTextView;
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}