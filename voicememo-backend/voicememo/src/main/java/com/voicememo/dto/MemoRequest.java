package com.voicememo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemoRequest {

    // Optional custom title — if empty we auto-generate "Memo - [date]"
    private String title;

    // Duration in seconds sent from the frontend/mobile app
    private Integer durationSeconds;
}