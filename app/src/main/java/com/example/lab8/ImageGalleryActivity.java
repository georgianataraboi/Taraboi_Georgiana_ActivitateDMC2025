package com.example.lab8;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ImageGalleryActivity extends AppCompatActivity {
    private ListView carImagesListView;
    private CarImageAdapter adapter;
    private List<CarImage> carImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_gallery);

        carImagesListView = findViewById(R.id.car_images_list_view);
        carImages = loadCarImages();

        adapter = new CarImageAdapter(this, carImages);
        carImagesListView.setAdapter(adapter);

        carImagesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CarImage selectedCarImage = carImages.get(position);

                // Open WebViewActivity with the selected URL
                Intent intent = new Intent(ImageGalleryActivity.this, WebViewActivity.class);
                intent.putExtra("webUrl", selectedCarImage.getWebUrl());
                startActivity(intent);
            }
        });
    }

    private List<CarImage> loadCarImages() {
        List<CarImage> images = new ArrayList<>();

        images.add(new CarImage(
                "https://static.automarket.ro/img/auto_resized/db/article/110/523/674273l-1000x640-b-8d2eb467.jpg",
                "Mercedes-AMG C63 S E Performance",
                "https://www.auto-data.net/ro/mercedes-benz-c-class-w206-amg-c-63-s-e-performance-680hp-plug-in-hybrid-4matic-amg-speedshift-mct-9g-46524"
        ));

        images.add(new CarImage(
                "https://blog.consumerguide.com/wp-content/uploads/sites/2/2021/09/aIMG_5370.jpg",
                "BMW M4 Competition",
                "https://www.bmw.ro/ro/all-models/m-series/bmw-4-series-m-models/date-tehnice-bmw-m4-coupe.html/m4-cs-coupe.bmw"
        ));

        images.add(new CarImage(
                "https://www.autocritica.ro/wp-content/uploads/2021/02/audi-a4-sedan-top-1536x1024.jpg",
                "Audi A4",
                "https://ro.autodata24.com/audi/a4/a4-b8/details"
        ));

        images.add(new CarImage(
                "https://images-rajhraciek-cdn.rshop.sk/lg/products/69fdbfe50f0aa87831362cef2bb8712d.jpg",
                "Mini Cooper",
                "https://www.auto-data.net/ro/mini-hatch-model-1704"
        ));

        images.add(new CarImage(
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQqaJ9oPpQcNBqfR6d5dOW2Vr6dSUjZo4yVBw&s",
                "Dacia 1310",
                "https://www.auto-data.net/ro/dacia-1310-model-1794"
        ));

        return images;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.shutdown();
        }
    }
}