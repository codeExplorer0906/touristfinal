package com.example.touristfinal;

class Tourist {
    public String fullName, dob, gender, nationality, passportNumber,
            phone, email, emergencyName, emergencyPhone,
            photoUrl, issueDate, expiryDate, touristId;

    // No-arg constructor required by Firebase
    public Tourist() {}

    public Tourist(String fullName, String dob, String gender, String nationality,
                   String passportNumber, String phone, String email,
                   String emergencyName, String emergencyPhone,
                   String photoUrl, String issueDate, String expiryDate, String touristId) {
        this.fullName = fullName;
        this.dob = dob;
        this.gender = gender;
        this.nationality = nationality;
        this.passportNumber = passportNumber;
        this.phone = phone;
        this.email = email;
        this.emergencyName = emergencyName;
        this.emergencyPhone = emergencyPhone;
        this.photoUrl = photoUrl;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.touristId = touristId;
    }
}
