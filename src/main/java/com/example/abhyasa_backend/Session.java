package com.example.abhyasa_backend;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to the original booking
    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "parent_user_id", nullable = false)
    private User parentUser;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;

    // Session sequence: 1, 2, 3...
    @Column(nullable = false)
    private int sessionNumber = 1;

    @Column(nullable = false)
    private LocalDate sessionDate;

    @Column(nullable = false)
    private LocalTime sessionStartTime;

    @Column(nullable = false)
    private LocalTime sessionEndTime;

    // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    @Column(nullable = false)
    private String status = "SCHEDULED";

    // Doctor's clinical notes for this session
    @Column(columnDefinition = "TEXT")
    private String sessionNotes;

    // Medication prescribed until next session
    @Column(columnDefinition = "TEXT")
    private String medication;

    // Exercises/activities for home
    @Column(columnDefinition = "TEXT")
    private String homework;

    // NEEDS_FOLLOWUP, RECOVERED, REFERRED
    private String outcome;

    // If referred to another service after general screening
    @ManyToOne
    @JoinColumn(name = "referred_to_service_id")
    private Service referredToService;

    // Self-referencing: links to the follow-up session
    @OneToOne
    @JoinColumn(name = "next_session_id")
    private Session nextSession;

    // Follow-up appointment details (stored for easy display)
    private LocalDate followupDate;
    private LocalTime followupTime;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Default constructor
    public Session() {
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public User getParentUser() {
        return parentUser;
    }

    public void setParentUser(User parentUser) {
        this.parentUser = parentUser;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public int getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(int sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public LocalTime getSessionStartTime() {
        return sessionStartTime;
    }

    public void setSessionStartTime(LocalTime sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
    }

    public LocalTime getSessionEndTime() {
        return sessionEndTime;
    }

    public void setSessionEndTime(LocalTime sessionEndTime) {
        this.sessionEndTime = sessionEndTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSessionNotes() {
        return sessionNotes;
    }

    public void setSessionNotes(String sessionNotes) {
        this.sessionNotes = sessionNotes;
    }

    public String getMedication() {
        return medication;
    }

    public void setMedication(String medication) {
        this.medication = medication;
    }

    public String getHomework() {
        return homework;
    }

    public void setHomework(String homework) {
        this.homework = homework;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Service getReferredToService() {
        return referredToService;
    }

    public void setReferredToService(Service referredToService) {
        this.referredToService = referredToService;
    }

    public Session getNextSession() {
        return nextSession;
    }

    public void setNextSession(Session nextSession) {
        this.nextSession = nextSession;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getFollowupDate() {
        return followupDate;
    }

    public void setFollowupDate(LocalDate followupDate) {
        this.followupDate = followupDate;
    }

    public LocalTime getFollowupTime() {
        return followupTime;
    }

    public void setFollowupTime(LocalTime followupTime) {
        this.followupTime = followupTime;
    }
}
