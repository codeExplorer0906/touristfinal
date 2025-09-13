package com.example.touristfinal;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

public class previewActivity extends AppCompatActivity {

    private TextView tvName, tvDob, tvGender, tvNationality, tvPassport, tvPhone, tvEmail, tvEmergencyName, tvEmergencyPhone, tvUniqueId;
    private ImageView ivPhoto, ivQrCode;
    private Button btnEdit, btnSave;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        // Bind Views
        tvName = findViewById(R.id.tvName);
        tvDob = findViewById(R.id.tvDob);
        tvGender = findViewById(R.id.tvGender);
        tvNationality = findViewById(R.id.tvNationality);
        tvPassport = findViewById(R.id.tvPassport);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvEmergencyName = findViewById(R.id.tvEmergencyName);
        tvEmergencyPhone = findViewById(R.id.tvEmergencyPhone);
        tvUniqueId = findViewById(R.id.tvTouristId);
        ivPhoto = findViewById(R.id.ivPhoto);
        ivQrCode = findViewById(R.id.ivQr);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);

        // Receive Data from FormActivity
        Intent intent = getIntent();
        String name = intent.getStringExtra("fullName");
        String dob = intent.getStringExtra("dob");
        String gender = intent.getStringExtra("gender");
        String nationality = intent.getStringExtra("nationality");
        String passport = intent.getStringExtra("passportNumber");
        String phone = intent.getStringExtra("phone");
        String email = intent.getStringExtra("email");
        String emergencyName = intent.getStringExtra("emergencyName");
        String emergencyPhone = intent.getStringExtra("emergencyPhone");
        String photoUri = intent.getStringExtra("photoUri");
        String uniqueId = intent.getStringExtra("uniqueId");

        // ✅ Generate Tourist ID if not passed from FormActivity
        if (uniqueId == null || uniqueId.isEmpty()) {
            uniqueId = "TID-" + System.currentTimeMillis();
        }

        // Set Texts
        tvName.setText("Name: " + name);
        tvDob.setText("DOB: " + dob);
        tvGender.setText("Gender: " + gender);
        tvNationality.setText("Nationality: " + nationality);
        tvPassport.setText("Passport: " + passport);
        tvPhone.setText("Phone: " + phone);
        tvEmail.setText("Email: " + email);
        tvEmergencyName.setText("Emergency Name: " + emergencyName);
        tvEmergencyPhone.setText("Emergency Phone: " + emergencyPhone);
        tvUniqueId.setText("Tourist ID: " + uniqueId);

        // Load Photo
        if (photoUri != null) {
            Glide.with(this).load(photoUri).into(ivPhoto);
        }

        // Generate QR Code
        generateQrCode(uniqueId);

        // Edit Button → Go Back to FormActivity
        String finalUniqueId = uniqueId;
        btnEdit.setOnClickListener(v -> {
            Intent editIntent = new Intent(previewActivity.this, FormActivity.class);
            editIntent.putExtra("fullName", name);
            editIntent.putExtra("dob", dob);
            editIntent.putExtra("gender", gender);
            editIntent.putExtra("nationality", nationality);
            editIntent.putExtra("passportNumber", passport);
            editIntent.putExtra("phone", phone);
            editIntent.putExtra("email", email);
            editIntent.putExtra("emergencyName", emergencyName);
            editIntent.putExtra("emergencyPhone", emergencyPhone);
            editIntent.putExtra("photoUri", photoUri);
            editIntent.putExtra("uniqueId", finalUniqueId); // ✅ keep the same ID
            startActivity(editIntent);
            finish();
        });

        // Save Button → Go to Next Screen
        btnSave.setOnClickListener(v -> {
            Intent saveIntent = new Intent(previewActivity.this, exportpdfActivity.class);
            saveIntent.putExtra("uniqueId", finalUniqueId);
            startActivity(saveIntent);
            finish();
        });
    }

    private void generateQrCode(String data) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            int size = 300;
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? android.graphics.Color.BLACK : android.graphics.Color.WHITE);
                }
            }
            ivQrCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}
