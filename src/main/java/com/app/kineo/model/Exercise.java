package com.app.kineo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exercises")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "category")
    private String category; // e.g., STRENGTH, CARDIO, FLEXIBILITY

    @Column(name = "muscle_group")
    private String muscleGroup; // e.g., CHEST, LEGS, BACK

    private String equipment; // e.g., DUMBBELL, BARBELL, MACHINE

    @Column(name = "image_url")
    private String imageUrl; // URL to an image (S3, Cloudinary, or public link)

    @Column(name = "video_url")
    private String videoUrl; // URL to a video (YouTube, Vimeo, or storage link)
}
