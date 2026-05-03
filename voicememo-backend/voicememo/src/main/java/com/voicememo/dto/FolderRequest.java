package com.voicememo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FolderRequest {

    @NotBlank(message = "Folder name is required")
    private String name;

    private String description;
}