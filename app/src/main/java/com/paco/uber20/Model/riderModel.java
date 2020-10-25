package com.paco.uber20.Model;

public class riderModel {
    private String Name, Phone, Email,Password,Gender,Avatar;

    public riderModel() {
    }

    public riderModel(String name, String phone,
                      String email, String password,
                      String gender, String avatar) {
        Name = name;
        Phone = phone;
        Email = email;
        Password = password;
        Gender = gender;
        Avatar = avatar;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getPhone() {
        return Phone;
    }

    public void setPhone(String phone) {
        Phone = phone;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }

    public String getGender() {
        return Gender;
    }

    public void setGender(String gender) {
        Gender = gender;
    }

    public String getAvatar() {
        return Avatar;
    }

    public void setAvatar(String avatar) {
        Avatar = avatar;
    }
}
