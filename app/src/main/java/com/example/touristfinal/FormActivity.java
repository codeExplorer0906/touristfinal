package com.example.touristfinal;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.touristfinal.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class FormActivity extends AppCompatActivity {

    EditText etFullName, etDOB, etPassport, etPhone, etEmail,
            etEmergencyName, etEmergencyPhone, etIssueDate, etExpiryDate;
    RadioGroup rgGender;
    Spinner spNationality;
    Button btnSubmit;

    Uri photoUri = null;
    Calendar calendar = Calendar.getInstance();

    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int REQUEST_GALLERY_PICK = 1002;
    private static final int RC_CAMERA_PERMISSION = 2001;
    private static final int RC_STORAGE_PERMISSION = 2002;

    private ImageView imagePreview;
    private Button btnCamera, btnGallery;
    private Uri imageUri; // holds camera/gallery image Uri


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        // Link UI with Java
        etFullName = findViewById(R.id.etFullName);
        etDOB = findViewById(R.id.etDOB);
        etPassport = findViewById(R.id.etPassport);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etEmergencyName = findViewById(R.id.etEmergencyName);
        etEmergencyPhone = findViewById(R.id.etEmergencyPhone);
        etIssueDate = findViewById(R.id.etIssueDate);
        etExpiryDate = findViewById(R.id.etExpiryDate);
        rgGender = findViewById(R.id.rgGender);
        spNationality = findViewById(R.id.spNationality);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Fill Nationality Spinner with countries
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.countries_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNationality.setAdapter(adapter);

        // Date of Birth Picker
        etDOB.setOnClickListener(v -> showDatePicker(etDOB));

        // Issue Date = today
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime());
        etIssueDate.setText(today);

        // Expiry Date Picker
        etExpiryDate.setOnClickListener(v -> showDatePicker(etExpiryDate));

        // Upload Photo section
        imagePreview = findViewById(R.id.imagePreview);
        btnCamera = findViewById(R.id.btnCamera);
        btnGallery = findViewById(R.id.btnGallery);

        // disable camera button on devices with no camera
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            btnCamera.setEnabled(false);
        }

        // camera click
        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        RC_CAMERA_PERMISSION);
            } else {
                openCamera();
            }
        });

        // gallery click
        btnGallery.setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                // need READ_EXTERNAL_STORAGE permission
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            RC_STORAGE_PERMISSION);
                } else {
                    openGallery();
                }
            } else {
                // Android 13+ doesn’t need storage permission
                openGallery();
            }
        });

        // Submit Button
        btnSubmit.setOnClickListener(v -> saveTouristData());
    }

    private void openCamera() {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "TouristPhoto_" + System.currentTimeMillis());
            values.put(MediaStore.Images.Media.DESCRIPTION, "Photo from Tourist Guide app");
            // insert and get uri where camera app should save the image
            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
        } catch (Exception e) {
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");
        startActivityForResult(pickIntent, REQUEST_GALLERY_PICK);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RC_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission required to take photo", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == RC_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Storage permission required to choose photo", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void showDatePicker(EditText editText) {
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    String date = day + "/" + (month + 1) + "/" + year;
                    editText.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void saveTouristData() {
        String fullName = etFullName.getText().toString();
        String dob = etDOB.getText().toString();
        String gender = ((RadioButton)findViewById(rgGender.getCheckedRadioButtonId())).getText().toString();
        String nationality = spNationality.getSelectedItem().toString();
        String passport = etPassport.getText().toString();
        String phone = etPhone.getText().toString();
        String email = etEmail.getText().toString();
        String emergencyName = etEmergencyName.getText().toString();
        String emergencyPhone = etEmergencyPhone.getText().toString();
        String issueDate = etIssueDate.getText().toString();
        String expiryDate = etExpiryDate.getText().toString();

        Tourist tourist = new Tourist(fullName, dob, gender, nationality,
                passport, phone, email,
                emergencyName, emergencyPhone,
                (photoUri != null ? photoUri.toString() : ""),
                issueDate, expiryDate);

        Toast.makeText(this, "Tourist Registered: " + tourist.getFullName(), Toast.LENGTH_LONG).show();

        // TODO: Pass this tourist object to next screen (QR Code screen)
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (imageUri != null) {
                    imagePreview.setImageURI(imageUri);
                    Toast.makeText(this, "Photo captured", Toast.LENGTH_SHORT).show();
                    photoUri = imageUri; // keep reference for saving tourist data
                }
            } else if (requestCode == REQUEST_GALLERY_PICK) {
                if (data != null && data.getData() != null) {
                    imageUri = data.getData();
                    imagePreview.setImageURI(imageUri);
                    Toast.makeText(this, "Photo selected", Toast.LENGTH_SHORT).show();
                    photoUri = imageUri; // keep reference
                }
            }
        }
    }

    public static class Tourist {
        // Step 1: Personal Info
        private String fullName;
        private String dob;
        private String gender;
        private String nationality;

        // Step 2: Identity Info
        private String passportNumber;
        private String phone;
        private String email;

        // Step 3: Emergency Info
        private String emergencyName;
        private String emergencyPhone;

        // Step 4: Other Details
        private String photoUri;   // store path/URI of uploaded photo
        private String issueDate;
        private String expiryDate;

        // Constructor
        public Tourist(String fullName, String dob, String gender, String nationality,
                       String passportNumber, String phone, String email,
                       String emergencyName, String emergencyPhone,
                       String photoUri, String issueDate, String expiryDate) {
            this.fullName = fullName;
            this.dob = dob;
            this.gender = gender;
            this.nationality = nationality;
            this.passportNumber = passportNumber;
            this.phone = phone;
            this.email = email;
            this.emergencyName = emergencyName;
            this.emergencyPhone = emergencyPhone;
            this.photoUri = photoUri;
            this.issueDate = issueDate;
            this.expiryDate = expiryDate;
        }

        // Getters (useful later)
        public String getFullName() { return fullName; }
        public String getDob() { return dob; }
        public String getGender() { return gender; }
        public String getNationality() { return nationality; }
        public String getPassportNumber() { return passportNumber; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
        public String getEmergencyName() { return emergencyName; }
        public String getEmergencyPhone() { return emergencyPhone; }
        public String getPhotoUri() { return photoUri; }
        public String getIssueDate() { return issueDate; }
        public String getExpiryDate() { return expiryDate; }
    }
}
