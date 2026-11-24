package com.library.model;

import java.sql.Date;

public class Member {
    private int memberId;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private Date membershipDate;
    private String status; // ACTIVE, SUSPENDED, EXPIRED

    public Member() {
    }

    public Member(String fullName, String email, String phone, String address) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.status = "ACTIVE";
    }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Date getMembershipDate() { return membershipDate; }
    public void setMembershipDate(Date membershipDate) { this.membershipDate = membershipDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return fullName + " (" + email + ") - " + status;
    }
}
