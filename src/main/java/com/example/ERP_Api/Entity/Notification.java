package com.example.ERP_Api.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** One message for the admins, for example "a customer placed an order". */
@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** What happened, e.g. ORDER_PLACED. Lets the screen pick an icon later. */
    private String type;

    private String title;

    @Column(length = 500)
    private String message;

    /** The invoice the notification is about, so the screen can link to it. */
    private Long invoiceId;

    private Long amountCents;

    /** Shared by all admins: once one admin reads it, it counts as read for everyone. */
    private boolean seen = false;

    private Instant createdAt = Instant.now();
}

