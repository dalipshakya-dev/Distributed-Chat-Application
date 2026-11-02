package com.project.Chat_Application.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PagedResponseDto<T> {
    private List<T> content;
    private int pageNumber;
    private int totalPages;
    private long totalElements;
    private boolean last;
}