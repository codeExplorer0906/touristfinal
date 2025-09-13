package com.example.touristfinal;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;

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

// 🔹 Firebase imports
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

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

    // 🔹 Firebase
    private FirebaseFirestore db;
    private StorageReference storageRef;

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

        // 🔹 Firebase init
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Submit Button
        btnSubmit.setOnClickListener(v -> saveTouristData());
        // handle incoming edit intent (pre-fill)
        Intent in = getIntent();
        if (in != null && in.hasExtra("fullName")) {
            etFullName.setText(in.getStringExtra("fullName"));
            etDOB.setText(in.getStringExtra("dob"));
            etPassport.setText(in.getStringExtra("passportNumber"));
            etPhone.setText(in.getStringExtra("phone"));
            etEmail.setText(in.getStringExtra("email"));
            etEmergencyName.setText(in.getStringExtra("emergencyName"));
            etEmergencyPhone.setText(in.getStringExtra("emergencyPhone"));
            etIssueDate.setText(in.getStringExtra("issueDate"));
            etExpiryDate.setText(in.getStringExtra("expiryDate"));

            String g = in.getStringExtra("gender");
            if (g != null) {
                if (g.equalsIgnoreCase("Male")) findViewById(R.id.rbMale).setSelected(true);
                if (g.equalsIgnoreCase("Female")) findViewById(R.id.rbFemale).setSelected(true);
                if (g.equalsIgnoreCase("Other")) findViewById(R.id.rbOther).setSelected(true);
                // better: use actual ids:
                if (g.equalsIgnoreCase("Male")) ((RadioButton)findViewById(R.id.rbMale)).setChecked(true);
                if (g.equalsIgnoreCase("Female")) ((RadioButton)findViewById(R.id.rbFemale)).setChecked(true);
                if (g.equalsIgnoreCase("Other")) ((RadioButton)findViewById(R.id.rbOther)).setChecked(true);
            }

            // set spinner selection by value
            // set spinner selection by value
            String nat = in.getStringExtra("nationality");
            if (nat != null && spNationality.getAdapter() != null) {
                ArrayAdapter spinnerAdapter = (ArrayAdapter) spNationality.getAdapter();
                int pos = spinnerAdapter.getPosition(nat);
                if (pos >= 0) spNationality.setSelection(pos);
            }


            String pStr = in.getStringExtra("photoUri");
            if (pStr != null && !pStr.isEmpty()) {
                try {
                    photoUri = Uri.parse(pStr);
                    imagePreview.setImageURI(photoUri);
                } catch (Exception e) { /* ignore */ }
            }
        }

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

    // 🔹 Updated saveTouristData
    // replace the entire saveTouristData() in your FormActivity with this:
    private void saveTouristData() {
        // 1. Read & validate fields
        String fullName = etFullName.getText().toString().trim();
        String dob = etDOB.getText().toString().trim();
        int checkedId = rgGender.getCheckedRadioButtonId();
        String gender;
        if (checkedId != -1) {
            RadioButton rb = findViewById(checkedId);
            if (rb != null) gender = rb.getText().toString().trim();
            else {
                gender = "";
            }
        } else {
            gender = "";
        }
        String nationality = spNationality.getSelectedItem() != null ? spNationality.getSelectedItem().toString() : "";
        String passport = etPassport.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String emergencyName = etEmergencyName.getText().toString().trim();
        String emergencyPhone = etEmergencyPhone.getText().toString().trim();
        String issueDate = etIssueDate.getText().toString().trim();
        String expiryDate = etExpiryDate.getText().toString().trim();

        // basic validation (adjust as needed)
        if (fullName.isEmpty()) {
            etFullName.setError("Required");
            etFullName.requestFocus();
            return;
        }
        if (passport.isEmpty()) {
            etPassport.setError("Required");
            etPassport.requestFocus();
            return;
        }

        // disable submit to avoid double clicks
        btnSubmit.setEnabled(false);
        Toast savingToast = Toast.makeText(this, "Saving..., please wait", Toast.LENGTH_SHORT);
        savingToast.show();

        // 2. Convert photoUri -> Base64 (if present). Compress and check size.
        String photoBase64 = "";
        if (photoUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(photoUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                // resize if too large (simple scale-down)
                int maxDim = 1024;
                if (bitmap.getWidth() > maxDim || bitmap.getHeight() > maxDim) {
                    float scale = Math.min((float)maxDim/bitmap.getWidth(), (float)maxDim/bitmap.getHeight());
                    bitmap = Bitmap.createScaledBitmap(bitmap,
                            Math.round(bitmap.getWidth() * scale),
                            Math.round(bitmap.getHeight() * scale),
                            true);
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos); // 60% quality
                byte[] imageBytes = baos.toByteArray();

                // check approximate size — Firestore document limit ~1 MiB => avoid big images
                if (imageBytes.length > 800_000) { // ~800 KB
                    // warn user and stop saving
                    Toast.makeText(this, "Selected photo is too large. Choose a smaller photo.", Toast.LENGTH_LONG).show();
                    btnSubmit.setEnabled(true);
                    return;
                }

                photoBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error processing photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                btnSubmit.setEnabled(true);
                return;
            }
        }

        // 3. Prepare data map (safe and explicit)
        Map<String, Object> data = new HashMap<>();
        data.put("fullName", fullName);
        data.put("dob", dob);
        data.put("gender", gender);
        data.put("nationality", nationality);
        data.put("passportNumber", passport);
        data.put("phone", phone);
        data.put("email", email);
        data.put("emergencyName", emergencyName);
        data.put("emergencyPhone", emergencyPhone);
        data.put("photoBase64", photoBase64); // store as photoBase64
        data.put("issueDate", issueDate);
        data.put("expiryDate", expiryDate);

        // optionally add a timestamp
        data.put("createdAt", com.google.firebase.Timestamp.now());

        // optional integrity hash (keeps your earlier idea)
        String canonical = fullName + "|" + passport + "|" + dob + "|" + nationality;
        data.put("dataHash", sha256(canonical));

        // 4. Save to Firestore
        String finalPhotoBase6 = photoBase64;
        db.collection("tourists")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    // success -> open Preview activity and pass saved document id + values
                    String docId = documentReference.getId();
                    Toast.makeText(FormActivity.this, "Saved (ID: " + docId + ")", Toast.LENGTH_SHORT).show();

                    // Build intent to PreviewActivity (replace PreviewActivity.class with your preview Activity class)
                    Intent preview = new Intent(FormActivity.this, previewActivity.class);

                    // pass the docId so preview can fetch exact saved doc if needed
                    preview.putExtra("docId", docId);

                    // pass the displayed fields (so preview doesn't need to re-query)
                    preview.putExtra("fullName", fullName);
                    preview.putExtra("dob", dob);
                    preview.putExtra("gender", gender);
                    preview.putExtra("nationality", nationality);
                    preview.putExtra("passportNumber", passport);
                    preview.putExtra("phone", phone);
                    preview.putExtra("email", email);
                    preview.putExtra("emergencyName", emergencyName);
                    preview.putExtra("emergencyPhone", emergencyPhone);
                    preview.putExtra("issueDate", issueDate);
                    preview.putExtra("expiryDate", expiryDate);
                    preview.putExtra("photoBase64", finalPhotoBase6);

                    // start preview
                    startActivity(preview);

                    btnSubmit.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(FormActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSubmit.setEnabled(true);
                });
    }



    private void uploadImageAndSaveTourist(Uri imageUri, Tourist tourist) {
        String fileName = "tourist_photos/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference photoRef = storageRef.child(fileName);

        photoRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            tourist.setPhotoUri(downloadUrl);
                            saveTouristToFirestore(tourist, downloadUrl);
                        })
                )
                .addOnFailureListener(e -> {
                    Toast.makeText(FormActivity.this, "Photo upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    saveTouristToFirestore(tourist, "");
                });
    }

    private void saveTouristToFirestore(Tourist tourist, String photoUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("fullName", tourist.getFullName());
        data.put("dob", tourist.getDob());
        data.put("gender", tourist.getGender());
        data.put("nationality", tourist.getNationality());
        data.put("passportNumber", tourist.getPassportNumber());
        data.put("phone", tourist.getPhone());
        data.put("email", tourist.getEmail());
        data.put("emergencyName", tourist.getEmergencyName());
        data.put("emergencyPhone", tourist.getEmergencyPhone());
        data.put("photoUrl", photoUrl);
        data.put("issueDate", tourist.getIssueDate());
        data.put("expiryDate", tourist.getExpiryDate());

        // hash for authenticity
        String canonical = tourist.getFullName() + "|" + tourist.getPassportNumber() + "|" + tourist.getDob();
        data.put("dataHash", sha256(canonical));

        db.collection("tourists")
                .add(data)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(FormActivity.this, "Tourist saved successfully!", Toast.LENGTH_LONG).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(FormActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (imageUri != null) {
                    imagePreview.setImageURI(imageUri);
                    Toast.makeText(this, "Photo captured", Toast.LENGTH_SHORT).show();
                    photoUri = imageUri;
                }
            } else if (requestCode == REQUEST_GALLERY_PICK) {
                if (data != null && data.getData() != null) {
                    imageUri = data.getData();
                    imagePreview.setImageURI(imageUri);
                    Toast.makeText(this, "Photo selected", Toast.LENGTH_SHORT).show();
                    photoUri = imageUri;
                }
            }
        }
    }

    public static class Tourist {
        private String fullName;
        private String dob;
        private String gender;
        private String nationality;
        private String passportNumber;
        private String phone;
        private String email;
        private String emergencyName;
        private String emergencyPhone;
        private String photoUri;
        private String issueDate;
        private String expiryDate;

        // No-arg constructor for Firebase
        public Tourist() {}

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

        // Getters & setters
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getDob() { return dob; }
        public void setDob(String dob) { this.dob = dob; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public String getNationality() { return nationality; }
        public void setNationality(String nationality) { this.nationality = nationality; }
        public String getPassportNumber() { return passportNumber; }
        public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getEmergencyName() { return emergencyName; }
        public void setEmergencyName(String emergencyName) { this.emergencyName = emergencyName; }
        public String getEmergencyPhone() { return emergencyPhone; }
        public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }
        public String getPhotoUri() { return photoUri; }
        public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }
        public String getIssueDate() { return issueDate; }
        public void setIssueDate(String issueDate) { this.issueDate = issueDate; }
        public String getExpiryDate() { return expiryDate; }
        public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    }
}
